package com.navvistest.mobilechallenge.ui

import com.navvistest.mobilechallenge.data.ApiError
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.RowResult
import com.navvistest.mobilechallenge.domain.Section
import com.navvistest.mobilechallenge.domain.SectionListBuilder

/**
 * Everything the stream screen shows. One value describes the whole screen.
 */
sealed interface UiState {

    /** No rows yet and the numbers stream is still fetching. */
    data object Loading : UiState

    /** The numbers stream finished without any valid rows. */
    data class Empty(val skippedCount: Int) : UiState

    /**
     * At least one row to show.
     *
     * @property resultCount rows whose result has been computed.
     * @property isFetching true while either stream is still fetching.
     * @property failure set when a stream failed after rows were already shown.
     */
    data class Content(
        val sections: List<Section>,
        val rowCount: Int,
        val skippedCount: Int,
        val resultCount: Int,
        val isFetching: Boolean,
        val failure: StreamFailure?,
    ) : UiState {
        /** Both streams finished without an error. Results may still be computing. */
        val isComplete: Boolean get() = !isFetching && failure == null
    }

    /** The numbers stream failed before any row arrived. */
    data class Error(val error: ApiError) : UiState
}

enum class StreamName { Numbers, Inputs }

data class StreamFailure(val stream: StreamName, val error: ApiError)

sealed interface StreamStatus {
    data object Fetching : StreamStatus
    data object Finished : StreamStatus
    data class Failed(val error: ApiError) : StreamStatus
}

/**
 * Picks the screen state from the current rows and the status of both streams.
 * Kept as a plain function so the rules can be tested without a ViewModel.
 */
fun buildUiState(
    rows: Collection<NumberRow>,
    skippedCount: Int,
    numbersStatus: StreamStatus,
    inputsStatus: StreamStatus,
): UiState {
    if (rows.isEmpty()) {
        return when (numbersStatus) {
            StreamStatus.Fetching -> UiState.Loading
            StreamStatus.Finished -> UiState.Empty(skippedCount)
            is StreamStatus.Failed -> UiState.Error(numbersStatus.error)
        }
    }
    val failure = when {
        numbersStatus is StreamStatus.Failed -> StreamFailure(StreamName.Numbers, numbersStatus.error)
        inputsStatus is StreamStatus.Failed -> StreamFailure(StreamName.Inputs, inputsStatus.error)
        else -> null
    }
    return UiState.Content(
        sections = SectionListBuilder.build(rows),
        rowCount = rows.size,
        skippedCount = skippedCount,
        resultCount = rows.count { it.result is RowResult.Ready },
        isFetching = numbersStatus == StreamStatus.Fetching || inputsStatus == StreamStatus.Fetching,
        failure = failure,
    )
}
