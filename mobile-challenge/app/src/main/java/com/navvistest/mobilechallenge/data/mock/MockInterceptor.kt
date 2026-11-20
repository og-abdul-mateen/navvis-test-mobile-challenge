package com.navvistest.mobilechallenge.data.mock

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import java.net.SocketTimeoutException
import kotlin.random.Random

/**
 * Stands in for the server in the mock flavor. It answers requests for the two endpoints
 * without calling [Interceptor.Chain.proceed], so nothing reaches the network.
 *
 * Like the real service, each request returns the next chunk, then empty arrays once
 * everything has been sent.
 *
 * [random] is used from several OkHttp threads, so it must be thread-safe. [Random.Default] is.
 */
class MockInterceptor(
    private val scenario: MockScenario,
    private val random: Random = Random.Default,
) : Interceptor {

    private val numbers = MockEndpoint(key = "numbers", values = scenario.numbers)
    private val inputs = MockEndpoint(key = "computation_input", values = scenario.inputs)

    override fun intercept(chain: Interceptor.Chain): Response {
        val endpoint = when (chain.request().url.encodedPath) {
            "/numbers.json" -> numbers
            "/inputs.json" -> inputs
            else -> return chain.respond(code = 404, body = "")
        }

        // This runs on an OkHttp worker thread, so sleeping doesn't block the app.
        val delay = scenario.delayMillis.random(random)
        if (delay > 0) Thread.sleep(delay)
        if (chain.call().isCanceled()) throw IOException("Canceled")

        // Failures happen before a chunk is taken, so a failed request never loses data.
        if (random.nextDouble() < scenario.networkErrorChance) {
            throw if (random.nextBoolean()) SocketTimeoutException("timeout") else IOException("Connection reset")
        }
        if (random.nextDouble() < scenario.serverErrorChance) {
            return when (random.nextInt(3)) {
                0 -> chain.respond(code = 503, body = "")
                1 -> chain.respond(code = 429, body = "")
                else -> chain.respond(code = 200, body = "<html>Service temporarily unavailable</html>")
            }
        }

        val chunk = endpoint.nextChunk(maxSize = scenario.maxChunkSize)
        val body = JsonObject(mapOf(endpoint.key to JsonArray(chunk)))
        return chain.respond(code = 200, body = body.toString())
    }

    private fun Interceptor.Chain.respond(code: Int, body: String): Response = Response.Builder()
        .request(request())
        .protocol(Protocol.HTTP_1_1)
        .code(code)
        .message(if (code == 200) "OK" else "Error")
        .body(body.toResponseBody(JSON))
        .build()

    /**
     * The server-side position for one endpoint. Both endpoints are requested at the same time
     * on different threads, so reading and moving the position is synchronized.
     */
    private inner class MockEndpoint(val key: String, private val values: List<JsonElement>) {
        private var position = 0

        @Synchronized
        fun nextChunk(maxSize: Int): List<JsonElement> {
            val size = random.nextInt(1, maxSize + 1)
            val end = minOf(position + size, values.size)
            val chunk = values.subList(position, end).toList()
            position = end
            return chunk
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
