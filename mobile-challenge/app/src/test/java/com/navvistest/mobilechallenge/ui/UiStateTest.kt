package com.navvistest.mobilechallenge.ui

import com.navvistest.mobilechallenge.data.ApiError
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.ParsedNumber
import com.navvistest.mobilechallenge.domain.RowResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Checks which screen state is picked for each mix of rows and stream status. */
class UiStateTest {

    private val oneRow = listOf(NumberRow(0, ParsedNumber(0, 0, false), RowResult.NotNeeded))
    private val fetching = StreamStatus.Fetching
    private val finished = StreamStatus.Finished
    private val failed = StreamStatus.Failed(ApiError.Http(503))

    @Test
    fun `no rows while numbers are fetching is Loading`() {
        assertEquals(UiState.Loading, buildUiState(emptyList(), 0, fetching, fetching))
    }

    @Test
    fun `no rows after numbers finished is Empty with the skipped count`() {
        assertEquals(UiState.Empty(skippedCount = 4), buildUiState(emptyList(), 4, finished, fetching))
    }

    @Test
    fun `numbers failing before any row is Error`() {
        assertEquals(UiState.Error(ApiError.Http(503)), buildUiState(emptyList(), 0, failed, fetching))
    }

    @Test
    fun `rows stay on screen when a stream fails later`() {
        val state = buildUiState(oneRow, 0, finished, failed)

        assertTrue(state is UiState.Content)
        state as UiState.Content
        assertEquals(StreamFailure(StreamName.Inputs, ApiError.Http(503)), state.failure)
        assertEquals(false, state.isFetching)
    }
}
