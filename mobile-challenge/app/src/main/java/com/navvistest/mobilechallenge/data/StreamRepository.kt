package com.navvistest.mobilechallenge.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

private typealias FetchChunk = suspend () -> ApiResult<List<RawValue>>

/**
 * Streams both endpoints chunk by chunk until the server returns an empty array.
 *
 * Each endpoint keeps its own position. If a stream fails, collecting it again continues
 * from the next index instead of starting over. Each flow should have one collector at a time.
 */
class StreamRepository(
    fetchNumbers: FetchChunk,
    fetchInputs: FetchChunk,
    private val retryPolicy: RetryPolicy = RetryPolicy(),
    private val random: Random = Random.Default,
) {

    constructor(api: StreamApi) : this(api::fetchNumbers, api::fetchInputs)

    private val numbers = ChunkStream(fetchNumbers)
    private val inputs = ChunkStream(fetchInputs)

    fun numbers(): Flow<StreamEvent> = numbers.events()

    fun inputs(): Flow<StreamEvent> = inputs.events()

    private inner class ChunkStream(private val fetch: FetchChunk) {
        private var nextIndex = 0
        private var finished = false

        fun events(): Flow<StreamEvent> = flow {
            while (!finished) {
                when (val result = fetchWithRetry()) {
                    is ApiResult.Success -> {
                        val values = result.data
                        if (values.isEmpty()) {
                            finished = true
                        } else {
                            // The server has moved past these values, so advance before emitting.
                            val startIndex = nextIndex
                            nextIndex += values.size
                            emit(StreamEvent.Chunk(startIndex, values))
                        }
                    }
                    is ApiResult.Failure -> {
                        emit(StreamEvent.Failed(result.error))
                        return@flow
                    }
                }
            }
            emit(StreamEvent.Finished(nextIndex))
        }

        private suspend fun fetchWithRetry(): ApiResult<List<RawValue>> {
            var attempt = 1
            while (true) {
                val result = fetch()
                val shouldRetry = result is ApiResult.Failure &&
                    result.error.isRetryable() &&
                    attempt < retryPolicy.maxAttempts
                if (!shouldRetry) return result
                attempt++
                delay(retryPolicy.delayBefore(attempt, random))
            }
        }
    }
}
