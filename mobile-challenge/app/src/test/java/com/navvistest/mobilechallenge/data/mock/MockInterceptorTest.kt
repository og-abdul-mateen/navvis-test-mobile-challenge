package com.navvistest.mobilechallenge.data.mock

import com.navvistest.mobilechallenge.data.RawValue
import com.navvistest.mobilechallenge.data.RetryPolicy
import com.navvistest.mobilechallenge.data.StreamApi
import com.navvistest.mobilechallenge.data.StreamEvent
import com.navvistest.mobilechallenge.data.StreamRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds

/**
 * Runs the real StreamApi and StreamRepository against the mock, the same way the app does.
 */
class MockInterceptorTest {

    private fun repositoryFor(scenario: MockScenario, retryPolicy: RetryPolicy = RetryPolicy()): StreamRepository {
        val client = OkHttpClient.Builder()
            .addInterceptor(MockInterceptor(scenario, Random(2)))
            .build()
        val api = StreamApi(client, "https://navvis.com/".toHttpUrl())
        return StreamRepository(api::fetchNumbers, api::fetchInputs, retryPolicy, Random(3))
    }

    private fun MockScenario.expectedNumbers() = numbers.map { RawValue.Integer((it as JsonPrimitive).int.toLong()) }

    @Test
    fun `streams every value in order and then finishes`() = runTest {
        val scenario = MockScenario.happyPath(Random(1)).copy(delayMillis = 0L..0L)

        val events = repositoryFor(scenario).numbers().toList()

        val received = events.filterIsInstance<StreamEvent.Chunk>().flatMap { it.values }
        assertEquals(scenario.expectedNumbers(), received)
        assertEquals(StreamEvent.Finished(scenario.numbers.size), events.last())
    }

    @Test
    fun `failed requests never lose or repeat values`() = runTest {
        // Lots of failures, and enough attempts that the stream always gets through.
        val scenario = MockScenario.happyPath(Random(1)).copy(
            delayMillis = 0L..0L,
            serverErrorChance = 0.3,
            networkErrorChance = 0.2,
        )
        val patient = RetryPolicy(maxAttempts = 50, initialDelay = 1.milliseconds, maxDelay = 1.milliseconds)

        val events = repositoryFor(scenario, patient).numbers().toList()

        val received = events.filterIsInstance<StreamEvent.Chunk>().flatMap { it.values }
        assertEquals(scenario.expectedNumbers(), received)
    }
}
