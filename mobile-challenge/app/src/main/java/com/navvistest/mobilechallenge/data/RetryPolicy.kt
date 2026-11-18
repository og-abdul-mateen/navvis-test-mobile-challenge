package com.navvistest.mobilechallenge.data

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Policy for deciding retries for failed requests. We do exponential backoff with jitter.
 *
 * By default, the waits are roughly 250..500 ms before the 2nd attempt
 * and 500..1000 ms before the 3rd.
 */
data class RetryPolicy(
    val maxAttempts: Int = 3,
    val initialDelay: Duration = 500.milliseconds,
    val maxDelay: Duration = 5.seconds,
) {

    /**
     * Wait before attempt number [attempt] (2 for the first retry).
     * The delay doubles each time, capped at [maxDelay]. A random value between half and
     * the full delay is used, so many clients failing together don't all retry at the same moment.
     */
    fun delayBefore(attempt: Int, random: Random): Duration {
        val exponential = initialDelay * (1 shl (attempt - 2))
        val capped = minOf(exponential, maxDelay)
        return capped * (0.5 + random.nextDouble() * 0.5)
    }
}

/**
 * Whether the error is worth a retry because most 4xx codes (e.g. 404) won't work even after a
 * retry, so they fail straight away.
 * Timeouts, network problems, bad bodies, 5xx and 429 may succeed on a second try.
 */
fun ApiError.isRetryable(): Boolean = when (this) {
    is ApiError.Http -> code == 429 || code in 500..599
    ApiError.Timeout, is ApiError.Network, is ApiError.MalformedBody -> true
}
