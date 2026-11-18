package com.navvistest.mobilechallenge.data.mock

import com.navvistest.mobilechallenge.data.RawValue
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

/**
 * Runs the real StreamApi and StreamRepository against the mock, the same way the app does.
 */
class MockInterceptorTest {

    @Test
    fun `streams every value in order and then finishes`() = runTest {
        val scenario = MockScenario.happyPath(Random(1)).copy(delayMillis = 0L..0L)
        val client = OkHttpClient.Builder()
            .addInterceptor(MockInterceptor(scenario, Random(2)))
            .build()
        val repository = StreamRepository(StreamApi(client, "https://navvis.com/".toHttpUrl()))

        val events = repository.numbers().toList()

        val received = events.filterIsInstance<StreamEvent.Chunk>().flatMap { it.values }
        val expected = scenario.numbers.map { RawValue.Integer((it as JsonPrimitive).int.toLong()) }
        assertEquals(expected, received)
        assertEquals(StreamEvent.Finished(scenario.numbers.size), events.last())
    }
}
