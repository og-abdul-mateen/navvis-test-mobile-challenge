package com.navvistest.mobilechallenge.data

sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

/**
 * To represent errors from the API. The repository uses this to decide whether to retry,
 * and the UI turns it into a message.
 */
sealed interface ApiError {
    /** The server answered with a non-2xx status code. */
    data class Http(val code: Int) : ApiError

    /** The request took longer than the client's call timeout. */
    data object Timeout : ApiError

    /** No connection, DNS failure, connection reset and similar I/O problems. */
    data class Network(val message: String?) : ApiError

    /** The body wasn't JSON, or didn't have the expected key holding an array. */
    data class MalformedBody(val reason: String) : ApiError
}
