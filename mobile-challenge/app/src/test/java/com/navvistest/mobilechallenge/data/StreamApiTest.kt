package com.navvistest.mobilechallenge.data

import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Checks that transport problems come back as typed errors instead of exceptions.
 * An interceptor stands in place of the server, so no real network is used.
 */
class StreamApiTest {

    private fun apiAnswering(interceptor: Interceptor) = StreamApi(
        client = OkHttpClient.Builder().addInterceptor(interceptor).build(),
        baseUrl = "https://navvis.com/".toHttpUrl(),
    )

    @Test
    fun `server error becomes an Http error`() = runTest {
        val api = apiAnswering { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(503)
                .message("Service Unavailable")
                .body("".toResponseBody())
                .build()
        }

        assertEquals(ApiResult.Failure(ApiError.Http(503)), api.fetchNumbers())
    }

    @Test
    fun `timeout becomes a Timeout error`() = runTest {
        val api = apiAnswering { throw SocketTimeoutException("timeout") }

        assertEquals(ApiResult.Failure(ApiError.Timeout), api.fetchInputs())
    }

    @Test
    fun `connection failure becomes a Network error`() = runTest {
        val api = apiAnswering { throw IOException("Connection reset") }

        assertEquals(ApiResult.Failure(ApiError.Network("Connection reset")), api.fetchNumbers())
    }
}
