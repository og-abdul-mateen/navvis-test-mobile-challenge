package com.navvistest.mobilechallenge.domain

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

/**
 * These tests use runBlocking, not runTest. runTest's virtual clock would jump past the
 * timeout while the algorithm runs on a real background thread, so every call would time out.
 */
class ResultComputerTest {

    /** The simulated algorithm with no cost. */
    private val instantAlgorithm = SimulatedAlgorithm(Random(0), normalCostMillis = 0L..0L, outlierChance = 0.0)

    @Test
    fun `returns the last two digits padded to two characters`() = runBlocking {
        val computer = ResultComputer(instantAlgorithm)

        assertEquals("23", (computer.compute(4123) as ComputationResult.Success).formatted)
        assertEquals("01", (computer.compute(1) as ComputationResult.Success).formatted)
        assertEquals("00", (computer.compute(0) as ComputationResult.Success).formatted)
    }

    @Test
    fun `rejects inputs that are missing or out of range without running the algorithm`() = runBlocking {
        val calls = AtomicInteger()
        val computer = ResultComputer({ calls.incrementAndGet() })
        val invalid = ComputationResult.Failure(ComputationError.InvalidInput)

        assertEquals(invalid, computer.compute(null))
        assertEquals(invalid, computer.compute(-1))
        assertEquals(invalid, computer.compute(65_536))
        assertEquals(0, calls.get())
    }

    @Test
    fun `stops a slow computation at the timeout`() = runBlocking {
        // The test checks the thread is interrupted, not just abandoned.
        val computer = ResultComputer({ Thread.sleep(10_000); 0 }, timeout = 50.milliseconds)

        val result: ComputationResult
        val elapsed = measureTime { result = computer.compute(7) }

        assertEquals(ComputationResult.Failure(ComputationError.TimedOut), result)
        assertTrue("took $elapsed", elapsed < 2_000.milliseconds)
    }

    @Test
    fun `runs the algorithm once per input value`() = runBlocking {
        val calls = AtomicInteger()
        val computer = ResultComputer({ calls.incrementAndGet(); it % 100 })

        computer.compute(4123)
        computer.compute(4123)
        computer.compute(23)

        assertEquals(2, calls.get())
    }

    @Test
    fun `time spent waiting for a turn does not count towards the timeout`() = runBlocking {
        // One slot, 30 ms of work each, 100 ms timeout: the last of 10 waits about 270 ms in line.
        val computer = ResultComputer(
            algorithm = { Thread.sleep(30); it % 100 },
            timeout = 100.milliseconds,
            parallelism = 1,
        )

        val results = (1L..10L).map { input -> async { computer.compute(input) } }.awaitAll()

        assertTrue(results.all { it is ComputationResult.Success })
    }
}
