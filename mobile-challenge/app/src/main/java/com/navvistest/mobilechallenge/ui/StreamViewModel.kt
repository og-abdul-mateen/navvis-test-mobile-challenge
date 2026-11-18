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
import com.navvistest.mobilechallenge.domain.NumberParser
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.RowResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Collects both streams and turns them into [UiState].
 *
 * Both collectors run on the main thread (the default for [viewModelScope]), so the stores
 * below are only touched from one thread and need no locking.
 */
class StreamViewModel(private val repository: StreamRepository) : ViewModel() {

    /** Valid rows by stream index. */
    private val rows = HashMap<Int, NumberRow>()

    /** Inputs by stream index. Paired with rows. */
    private val inputs = HashMap<Int, RawValue>()

    private var skippedCount = 0
    private var numbersStatus: StreamStatus = StreamStatus.Fetching
    private var inputsStatus: StreamStatus = StreamStatus.Fetching

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
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
                inputs[event.startIndex + offset] = raw
            }
            is StreamEvent.Finished -> inputsStatus = StreamStatus.Finished
            is StreamEvent.Failed -> inputsStatus = StreamStatus.Failed(event.error)
        }
        publish()
    }

    // Rebuilds the whole state on the main thread after every event.
    private fun publish() {
        _uiState.value = buildUiState(rows.values, skippedCount, numbersStatus, inputsStatus)
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { StreamViewModel(container.streamRepository) }
        }
    }
}

/** The domain layer works with plain numbers; anything that isn't an integer counts as missing. */
private fun RawValue.asLongOrNull(): Long? = (this as? RawValue.Integer)?.value
