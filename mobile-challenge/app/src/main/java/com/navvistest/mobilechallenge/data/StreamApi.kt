package com.navvistest.mobilechallenge.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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
            client.newCall(request).await().use { response ->
                if (!response.isSuccessful) {
                    ApiResult.Failure(ApiError.Http(response.code))
                } else {
                    val body = withContext(Dispatchers.IO) { response.body.string() }
                    ChunkDecoder.decode(body, key)
                }
            }
        } catch (e: InterruptedIOException) {
            // OkHttp reports both call and socket timeouts this way.
            ApiResult.Failure(ApiError.Timeout)
        } catch (e: IOException) {
            ApiResult.Failure(ApiError.Network(e.message))
        }
    }
}

/**
 * Runs the call on OkHttp's own thread pool and suspends until it finishes.
 * Cancelling the coroutine cancels the HTTP call.
 */
private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    enqueue(object : Callback {
        override fun onResponse(call: Call, response: Response) {
            continuation.resume(response) { _, value, _ -> value.close() }
        }

        override fun onFailure(call: Call, e: IOException) {
            continuation.resumeWithException(e)
        }
    })
    continuation.invokeOnCancellation { cancel() }
}
