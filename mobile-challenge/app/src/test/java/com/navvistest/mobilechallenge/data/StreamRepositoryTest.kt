package com.navvistest.mobilechallenge.data

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random

class StreamRepositoryTest {

    /** Answers each request with the next result in the list. */
    private class FakeEndpoint(vararg results: ApiResult<List<RawValue>>) {
        private val queue = ArrayDeque(results.toList())
        var calls = 0
            private set

        suspend fun fetch(): ApiResult<List<RawValue>> {
            calls++
            return queue.removeFirst()
        }
    }

    private fun chunk(vararg values: Long) = ApiResult.Success(values.map { RawValue.Integer(it) })
    private fun empty() = ApiResult.Success(emptyList<RawValue>())
    private fun failure(error: ApiError) = ApiResult.Failure(error)
    private fun integers(vararg values: Long) = values.map { RawValue.Integer(it) }

    private fun repositoryFor(numbers: FakeEndpoint) = StreamRepository(
        fetchNumbers = numbers::fetch,
        fetchInputs = { error("inputs not used in this test") },
        random = Random(0),
    )

    @Test
    fun `emits chunks with running indexes and stops on an empty array`() = runTest {
        val endpoint = FakeEndpoint(chunk(4, 150), chunk(12), empty())

        val events = repositoryFor(endpoint).numbers().toList()

        val expected = listOf(
            StreamEvent.Chunk(0, integers(4, 150)),
            StreamEvent.Chunk(2, integers(12)),
            StreamEvent.Finished(3),
        )
        assertEquals(expected, events)
        assertEquals(3, endpoint.calls)
    }

    @Test
    fun `retries a failed request and continues`() = runTest {
        val endpoint = FakeEndpoint(
            failure(ApiError.Http(503)),
            failure(ApiError.Timeout),
            chunk(7),
            empty(),
        )

        val events = repositoryFor(endpoint).numbers().toList()

        assertEquals(listOf(StreamEvent.Chunk(0, integers(7)), StreamEvent.Finished(1)), events)
        assertEquals(4, endpoint.calls)
    }

    @Test
    fun `gives up after the last attempt`() = runTest {
        val endpoint = FakeEndpoint(
            failure(ApiError.Http(503)),
            failure(ApiError.Http(503)),
            failure(ApiError.Http(503)),
        )

        val events = repositoryFor(endpoint).numbers().toList()

        assertEquals(listOf(StreamEvent.Failed(ApiError.Http(503))), events)
        assertEquals(3, endpoint.calls)
    }

    @Test
    fun `does not retry a 404`() = runTest {
        val endpoint = FakeEndpoint(failure(ApiError.Http(404)))

        val events = repositoryFor(endpoint).numbers().toList()

        assertEquals(listOf(StreamEvent.Failed(ApiError.Http(404))), events)
        assertEquals(1, endpoint.calls)
    }

    @Test
    fun `collecting again after a failure resumes from the next index`() = runTest {
        val endpoint = FakeEndpoint(
            chunk(4, 150),
            failure(ApiError.Timeout),
            failure(ApiError.Timeout),
            failure(ApiError.Timeout),
            chunk(12),
            empty(),
        )
        val repository = repositoryFor(endpoint)

        val firstRun = repository.numbers().toList()
        val secondRun = repository.numbers().toList()

        assertEquals(
            listOf(StreamEvent.Chunk(0, integers(4, 150)), StreamEvent.Failed(ApiError.Timeout)),
            firstRun,
        )
        assertEquals(listOf(StreamEvent.Chunk(2, integers(12)), StreamEvent.Finished(3)), secondRun)
    }
}
