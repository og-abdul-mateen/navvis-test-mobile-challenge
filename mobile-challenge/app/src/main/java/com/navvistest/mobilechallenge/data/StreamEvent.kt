package com.navvistest.mobilechallenge.data

sealed interface StreamEvent {
    /**
     * A chunk of values from the server. [startIndex] is the stream index of the first value,
     * so value `i` in [values] has stream index `startIndex + i`.
     */
    data class Chunk(val startIndex: Int, val values: List<RawValue>) : StreamEvent

    /** The server returned an empty array. [totalCount] values were received in total. */
    data class Finished(val totalCount: Int) : StreamEvent

    /** Fetching stopped after retries ran out, or on an error that isn't worth retrying. */
    data class Failed(val error: ApiError) : StreamEvent
}
