package com.navvistest.mobilechallenge.data

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Makes one request to an endpoint and returns the decoded chunk.
 * It never throws for network or server problems; they come back as [ApiResult.Failure].
 */
class StreamApi(
    private val client: OkHttpClient,
    private val baseUrl: HttpUrl,
) {

    suspend fun fetchNumbers(): ApiResult<List<RawValue>> = fetch(path = "numbers.json", key = "numbers")

    suspend fun fetchInputs(): ApiResult<List<RawValue>> = fetch(path = "inputs.json", key = "computation_input")

    private suspend fun fetch(path: String, key: String): ApiResult<List<RawValue>> {
        val request = Request.Builder().url(baseUrl.resolve(path)!!).build()
        return try {
            val response = client.newCall(request).await()
            if (response.body == null) {
                ApiResult.Failure(ApiError.Http(response.code))
            } else {
                ChunkDecoder.decode(response.body, key)
            }
        } catch (e: InterruptedIOException) {
            // OkHttp reports both call and socket timeouts this way.
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        }
    }
}

private class HttpResponse(val code: Int, val body: String?)

/**
 * Runs the call on OkHttp's own thread pool and suspends until it finishes.
 * Cancelling the coroutine cancels the HTTP call.
 *
 * The body is read and the response is closed inside the callback, on OkHttp's thread.
 * Closing a response can write to the network (HTTP/2 sends a reset for unread data),
 * and Android doesn't allow that on the main thread, where callers may resume.
 */
private suspend fun Call.await(): HttpResponse = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            val result = try {
                response.use {
                    // Error bodies aren't needed, so they're never read.
                    HttpResponse(it.code, if (it.isSuccessful) it.body.string() else null)
                }
            } catch (e: IOException) {
                continuation.resumeWithException(e)
                return
            }
            continuation.resume(result)
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}
