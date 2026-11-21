package com.navvistest.mobilechallenge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.navvistest.mobilechallenge.data.RawValue
import com.navvistest.mobilechallenge.data.StreamEvent
import com.navvistest.mobilechallenge.data.StreamRepository
import com.navvistest.mobilechallenge.data.mock.MockScenarioType
import com.navvistest.mobilechallenge.di.AppContainer
import com.navvistest.mobilechallenge.domain.ComputationError
import com.navvistest.mobilechallenge.domain.ComputationResult
import com.navvistest.mobilechallenge.domain.NumberParser
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.ResultComputer
import com.navvistest.mobilechallenge.domain.RowError
import com.navvistest.mobilechallenge.domain.RowResult
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Collects both streams, pairs each number with the input at the same index, and turns
 * everything into [UiState].
 *
 * Everything here runs on the main thread (the default for [viewModelScope]): the stream
 * collectors and the code that stores finished results. So the stores below are only touched
 * from one thread and need no locking. Only the computations themselves run elsewhere.
 */
class StreamViewModel(
    private val openRepository: (MockScenarioType?) -> StreamRepository,
    private val resultComputer: ResultComputer,
    initialScenario: MockScenarioType?,
) : ViewModel() {

    private val _scenario = MutableStateFlow(initialScenario)

    /** The mock scenario in use. It is always null in the real build. */
    val scenario: StateFlow<MockScenarioType?> = _scenario.asStateFlow()

    private lateinit var repository: StreamRepository

    /**
     * Everything that belongs to one run of the streams: both collectors and every
     * computation. Cancelling it stops the run, so that the new scenario can
     * start from a clean slate.
     */
    private lateinit var session: CoroutineScope

    private lateinit var timer: SessionTimer

    /** Valid rows by stream index. */
    private val rows = HashMap<Int, NumberRow>()

    /** Inputs by stream index. */
    private val inputs = HashMap<Int, RawValue>()

    /** How many inputs exist in total, once the inputs stream has finished. */
    private var inputsTotal: Int? = null

    private var skippedCount = 0
    private var numbersStatus: StreamStatus = StreamStatus.Fetching
    private var inputsStatus: StreamStatus = StreamStatus.Fetching

    private val _uiState = MutableStateFlow<UiState>(UiState.Ready)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Goes up by one on every change. The number itself means nothing; a new value tells
     * the rebuild loop that something changed. A StateFlow ignores a value equal to the
     * current one, which is why this is a counter and not Unit.
     */
    private val changeCount = MutableStateFlow(0)

    private val _showRawValues = MutableStateFlow(false)

    /** Mock build only: show each row's number, bits and input under it. Kept across scenarios. */
    val showRawValues: StateFlow<Boolean> = _showRawValues.asStateFlow()

    /** False until START is pressed, and again after picking a new scenario. */
    private var isRunning = false

    init {
        viewModelScope.launch { rebuildLoop() }
    }

    /** Starts fetching. Nothing is requested before this, since every request uses up server data. */
    fun start() {
        if (isRunning) return
        isRunning = true
        startSession()
    }

    /**
     * Throws away the current run and goes back to the START button, with [scenario] picked.
     * It's only used in the mock build.
     */
    fun selectScenario(scenario: MockScenarioType) {
        if (_scenario.value == null) return
        _scenario.value = scenario
        stopSession()
        _uiState.value = UiState.Ready
    }

    fun toggleRawValues() {
        _showRawValues.value = !_showRawValues.value
        publish()
    }

    private fun stopSession() {
        isRunning = false
        if (::session.isInitialized) session.cancel()
        rows.clear()
        inputs.clear()
    }

    private fun startSession() {
        if (::session.isInitialized) session.cancel()
        session = CoroutineScope(viewModelScope.coroutineContext + SupervisorJob(viewModelScope.coroutineContext.job))
        timer = SessionTimer(_scenario.value?.label ?: "navvis.com")

        rows.clear()
        inputs.clear()
        inputsTotal = null
        skippedCount = 0
        numbersStatus = StreamStatus.Fetching
        inputsStatus = StreamStatus.Fetching

        repository = openRepository(_scenario.value)
        collectNumbers()
        collectInputs()
        publish()
    }

    /**
     * Fetches any failed stream again. The repository remembers where each stream stopped,
     * so this carries on from the next index and no row is fetched twice.
     * A stream that is still fetching or already finished is left alone, so tapping
     * Retry twice doesn't start a second collector.
     */
    fun retry() {
        if (numbersStatus is StreamStatus.Failed) {
            numbersStatus = StreamStatus.Fetching
            collectNumbers()
        }
        if (inputsStatus is StreamStatus.Failed) {
            inputsStatus = StreamStatus.Fetching
            collectInputs()
        }
        publish()
    }

    private fun collectNumbers() {
        session.launch { repository.numbers().collect { onNumbersEvent(it) } }
    }

    private fun collectInputs() {
        session.launch { repository.inputs().collect { onInputsEvent(it) } }
    }

    private fun onNumbersEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.Chunk -> event.values.forEachIndexed { offset, raw ->
                val index = event.startIndex + offset
                val number = NumberParser.parse(raw.asLongOrNull())
                if (number == null) {
                    skippedCount++
                } else {
                    val result = if (number.showsResult) RowResult.Pending else RowResult.NotNeeded
                    rows[index] = NumberRow(index, number, result)
                    if (number.showsResult) pairWithInput(index)
                }
            }
            is StreamEvent.Finished -> numbersStatus = StreamStatus.Finished
            is StreamEvent.Failed -> numbersStatus = StreamStatus.Failed(event.error)
        }
        publish()
    }

    private fun onInputsEvent(event: StreamEvent) {
        when (event) {
            is StreamEvent.Chunk -> event.values.forEachIndexed { offset, raw ->
                val index = event.startIndex + offset
                inputs[index] = raw
                if (rows[index]?.number?.showsResult == true) pairWithInput(index)
            }
            is StreamEvent.Finished -> {
                inputsStatus = StreamStatus.Finished
                inputsTotal = event.totalCount
                // Rows past the last input will never get one.
                rows.values
                    .filter { it.index >= event.totalCount && it.result == RowResult.Pending }
                    .forEach { rows[it.index] = it.copy(result = RowResult.Failed(RowError.MissingInput)) }
            }
            is StreamEvent.Failed -> inputsStatus = StreamStatus.Failed(event.error)
        }
        publish()
    }

    /**
     * Starts the computation for a row that needs a result, if its input is here.
     *
     * Called from both streams: whichever of the number and the input arrives second
     * starts the work, so it runs exactly once per row.
     */
    private fun pairWithInput(index: Int) {
        val input = inputs[index]
        if (input == null) {
            val total = inputsTotal
            if (total != null && index >= total) {
                rows[index] = rows.getValue(index).copy(result = RowResult.Failed(RowError.MissingInput))
            }
            return
        }
        session.launch {
            val result = resultComputer.compute(input.asLongOrNull())
            rows[index] = rows.getValue(index).copy(result = result.toRowResult())
            publish()
        }
    }

    /** Asks for the screen state to be rebuilt. Only called on the main thread. */
    private fun publish() {
        changeCount.value++
    }

    /**
     * Rebuilds the state at most once per [REBUILD_INTERVAL]. The first change is shown at once.
     * A StateFlow collector only sees the latest value, so all changes during the build or the
     * wait that follows are combined into the next rebuild, and the last change is never lost.
     *
     * The snapshot is taken on the main thread, where the stores are written. Grouping and
     * sorting then run on [Dispatchers.Default], so a large list never blocks the UI.
     *
     * This uses collect, not collectLatest: collectLatest would cancel a rebuild every time
     * a new change arrived, so during a busy stream the screen would hardly ever update.
     */
    private suspend fun rebuildLoop() {
        // drop(1) skips the starting value, which would only rebuild the initial Loading state.
        changeCount.drop(1).collect {
            if (!isRunning) return@collect
            val snapshot = rows.values.toList()
            val skipped = skippedCount
            val numbers = numbersStatus
            val inputsState = inputsStatus
            val sessionTimer = timer
            // Copied only while the toggle is on, so the normal screen pays nothing for it.
            val rawInputs = if (_showRawValues.value) HashMap(inputs) else null
            val (state, anyPending) = withContext(Dispatchers.Default) {
                buildUiState(snapshot, skipped, numbers, inputsState, rawInputs) to
                    snapshot.any { it.result == RowResult.Pending }
            }
            // A scenario may have been picked while this was building. Its result belongs to a
            // session that no longer exists, so it must not replace the START screen.
            if (!isRunning || sessionTimer !== timer) return@collect
            _uiState.value = state

            if (state is UiState.Content) {
                sessionTimer.mark("first rows on screen")
                if (state.isComplete) sessionTimer.mark("both streams finished")
                if (state.isComplete && !anyPending) sessionTimer.mark("all results computed")
            }
            delay(REBUILD_INTERVAL)
        }
    }

    companion object {
        private val REBUILD_INTERVAL = 100.milliseconds

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                StreamViewModel(container::streamRepository, container.resultComputer, container.defaultScenario)
            }
        }
    }
}

/** The domain layer works with plain numbers; anything that isn't an integer counts as missing. */
private fun RawValue.asLongOrNull(): Long? = (this as? RawValue.Integer)?.value

private fun ComputationResult.toRowResult(): RowResult = when (this) {
    is ComputationResult.Success -> RowResult.Ready(this)
    is ComputationResult.Failure -> RowResult.Failed(
        when (error) {
            ComputationError.InvalidInput -> RowError.InvalidInput
            ComputationError.TimedOut -> RowError.TimedOut
        },
    )
}
