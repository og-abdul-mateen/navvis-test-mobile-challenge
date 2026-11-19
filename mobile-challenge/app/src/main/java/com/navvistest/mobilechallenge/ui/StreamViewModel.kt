package com.navvistest.mobilechallenge.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.navvistest.mobilechallenge.data.RawValue
import com.navvistest.mobilechallenge.data.StreamEvent
import com.navvistest.mobilechallenge.data.StreamRepository
import com.navvistest.mobilechallenge.di.AppContainer
import com.navvistest.mobilechallenge.domain.ComputationError
import com.navvistest.mobilechallenge.domain.ComputationResult
import com.navvistest.mobilechallenge.domain.NumberParser
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.ResultComputer
import com.navvistest.mobilechallenge.domain.RowError
import com.navvistest.mobilechallenge.domain.RowResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Collects both streams, pairs each number with the input at the same index, and turns
 * everything into [UiState].
 *
 * Everything here runs on the main thread (the default for [viewModelScope]): the stream
 * collectors and the code that stores finished results. So the stores below are only touched
 * from one thread and need no locking. Only the computations themselves run elsewhere.
 */
class StreamViewModel(
    private val repository: StreamRepository,
    private val resultComputer: ResultComputer,
) : ViewModel() {

    /** Valid rows by stream index. */
    private val rows = HashMap<Int, NumberRow>()

    /** Inputs by stream index. */
    private val inputs = HashMap<Int, RawValue>()

    /** How many inputs exist in total, once the inputs stream has finished. */
    private var inputsTotal: Int? = null

    private var skippedCount = 0
    private var numbersStatus: StreamStatus = StreamStatus.Fetching
    private var inputsStatus: StreamStatus = StreamStatus.Fetching

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    /**
     * Goes up by one on every change. The number itself means nothing; a new value tells
     * the rebuild loop that something changed. A StateFlow ignores a value equal to the
     * current one, which is why this is a counter and not Unit.
     */
    private val changeCount = MutableStateFlow(0)

    init {
        viewModelScope.launch { rebuildLoop() }
        viewModelScope.launch { repository.numbers().collect { onNumbersEvent(it) } }
        viewModelScope.launch { repository.inputs().collect { onInputsEvent(it) } }
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
        viewModelScope.launch {
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
            val snapshot = rows.values.toList()
            val skipped = skippedCount
            val numbers = numbersStatus
            val inputsState = inputsStatus
            _uiState.value = withContext(Dispatchers.Default) {
                buildUiState(snapshot, skipped, numbers, inputsState)
            }
            delay(REBUILD_INTERVAL)
        }
    }

    companion object {
        private val REBUILD_INTERVAL = 100.milliseconds

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StreamViewModel(container.streamRepository, container.resultComputer) }
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
