package com.navvistest.mobilechallenge.domain

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

sealed interface ComputationResult {
    data class Success(val value: Int) : ComputationResult {
        /** The result as shown in the list, for example "03". */
        val formatted: String get() = value.toString().padStart(2, '0')
    }

    data class Failure(val error: ComputationError) : ComputationResult
}

enum class ComputationError {
    /** The input was null, not an integer, or outside 0..65535. */
    InvalidInput,

    /** The algorithm took longer than the timeout. */
    TimedOut,
}

/**
 * Runs [ResultAlgorithm] for an input from the inputs stream.
 *
 * - Work runs on a limited slice of [Dispatchers.Default], so the main thread stays free and
 *   at least one core is left for building the list.
 * - A computation that runs past [timeout] is interrupted and reported as [ComputationError.TimedOut].
 * - Results are cached by input value, because the same input always gives the same result.
 *   Timeouts aren't cached, so a later request for the same input tries again.
 */
class ResultComputer(
    private val algorithm: ResultAlgorithm = SimulatedAlgorithm(),
    private val timeout: Duration = 2.seconds,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(PARALLELISM),
) {

    private val cache = ConcurrentHashMap<Int, Int>()

    suspend fun compute(input: Long?): ComputationResult {
        if (input == null || input !in INPUT_RANGE) {
            return ComputationResult.Failure(ComputationError.InvalidInput)
        }
        val value = input.toInt()
        cache[value]?.let { return ComputationResult.Success(it) }

        // runInterruptible interrupts the thread when the timeout cancels the coroutine,
        // so a slow computation stops instead of holding a thread until it finishes.
        val result = withTimeoutOrNull(timeout) {
            runInterruptible(dispatcher) { algorithm.compute(value) }
        } ?: return ComputationResult.Failure(ComputationError.TimedOut)

        cache[value] = result
        return ComputationResult.Success(result)
    }

    private companion object {
        val INPUT_RANGE = 0L..65_535L
        val PARALLELISM = (Runtime.getRuntime().availableProcessors() - 1).coerceAtLeast(1)
    }
}
