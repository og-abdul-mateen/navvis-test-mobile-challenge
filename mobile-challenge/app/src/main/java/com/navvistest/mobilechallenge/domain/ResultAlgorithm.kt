package com.navvistest.mobilechallenge.domain

import kotlin.random.Random

/**
 * The server-side computation that turns an input into a result.
 * It's blocking and can be slow, so callers must run it off the main thread.
 */
fun interface ResultAlgorithm {
    fun compute(input: Int): Int
}

/**
 * Simulates the real algorithm for computation of results. It returns the last two digits
 * of the input and imitates the cost the challenge describes: a few milliseconds of work
 * most of the time, with occasional slow outliers.
 *
 * With the defaults, about 1 in 100 inputs is an outlier, and some outliers take longer
 * than [ResultComputer]'s timeout, so timeouts show up in the app now and then.
 *
 * [random] is used from several threads at once, so it must be thread-safe.
 */
class SimulatedAlgorithm(
    private val random: Random = Random.Default,
    private val normalCostMillis: LongRange = 1L..5L,
    private val outlierCostMillis: LongRange = 50L..3_000L,
    private val outlierChance: Double = 0.01,
) : ResultAlgorithm {

    override fun compute(input: Int): Int {
        val isOutlier = random.nextDouble() < outlierChance
        val cost = (if (isOutlier) outlierCostMillis else normalCostMillis).random(random)
        // Sleeping simulates CPU work: it keeps the thread busy for the same time.
        if (cost > 0) Thread.sleep(cost)
        return input % 100
    }
}
