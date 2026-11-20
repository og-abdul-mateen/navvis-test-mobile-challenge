package com.navvistest.mobilechallenge.data.mock

import com.navvistest.mobilechallenge.data.mock.MockScenario.Companion.validInput
import com.navvistest.mobilechallenge.data.mock.MockScenario.Companion.validNumber
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

/**
 * The scenarios a tester can pick in the mock build. Each one builds fresh data, so picking
 * the same scenario twice gives a new run.
 *
 * Labels are plain strings, not resources: they only appear in the debug menu of the mock build.
 */
enum class MockScenarioType(val label: String) {
    Normal("Normal data") {
        override fun build(random: Random) = MockScenario.happyPath(random)
    },

    /** About a quarter of the values in each stream can't be used, mixed in with good ones. */
    MessyValues("Messy values") {
        override fun build(random: Random) = MockScenario(
            numbers = List(COUNT) { if (random.nextDouble() < 0.25) badNumber(random) else validNumber(random) },
            inputs = List(COUNT) { if (random.nextDouble() < 0.25) badInput(random) else validInput(random) },
        )
    },

    /** Some requests fail with 5xx, 429 or a broken body. Retries cover most of them. */
    ServerErrors("Server errors") {
        override fun build(random: Random) = MockScenario.happyPath(random).copy(serverErrorChance = 0.25)
    },

    /** Some requests time out or lose their connection. */
    FlakyNetwork("Flaky network") {
        override fun build(random: Random) = MockScenario.happyPath(random).copy(networkErrorChance = 0.25)
    },

    /** The inputs stream ends early, so the last rows get "missing input". */
    FewerInputs("Fewer inputs than numbers") {
        override fun build(random: Random) = MockScenario.happyPath(random).let {
            it.copy(inputs = it.inputs.take(COUNT * 3 / 5))
        }
    },

    MoreInputs("More inputs than numbers") {
        override fun build(random: Random) = MockScenario.happyPath(random).let {
            it.copy(inputs = it.inputs + List(COUNT / 2) { validInput(random) })
        }
    },

    NoNumbers("No numbers") {
        override fun build(random: Random) = MockScenario(numbers = emptyList(), inputs = emptyList())
    },

    AllInvalid("Only invalid numbers") {
        override fun build(random: Random) = MockScenario(
            numbers = List(COUNT) { badNumber(random) },
            inputs = List(COUNT) { validInput(random) },
        )
    },

    /** Long delays, to see the loading states. */
    SlowServer("Slow server") {
        override fun build(random: Random) = MockScenario.happyPath(random).copy(delayMillis = 1_500L..2_500L)
    },

    /** 100,000 numbers in large chunks, to check the app stays smooth. */
    LargeDataset("100,000 numbers") {
        override fun build(random: Random) = MockScenario(
            numbers = List(100_000) { validNumber(random) },
            inputs = List(100_000) { validInput(random) },
            maxChunkSize = 500,
            delayMillis = 10L..50L,
        )
    };

    abstract fun build(random: Random = Random.Default): MockScenario

    private companion object {
        const val COUNT = 200

        /** Every way a number can be unusable: null, out of range, untitled item, string, decimal. */
        fun badNumber(random: Random): JsonElement = when (random.nextInt(5)) {
            0 -> JsonNull
            1 -> JsonPrimitive(if (random.nextBoolean()) -random.nextInt(1, 100) else random.nextInt(256, 1_000))
            // Item value 6..31 has no title.
            2 -> JsonPrimitive((random.nextInt(6, 32) shl 2) or random.nextInt(0, 4))
            3 -> JsonPrimitive(random.nextInt(0, 256).toString())
            else -> JsonPrimitive(random.nextInt(0, 256) + 0.5)
        }

        fun badInput(random: Random): JsonElement = when (random.nextInt(3)) {
            0 -> JsonNull
            1 -> JsonPrimitive(random.nextInt(65_536, 200_000))
            else -> JsonPrimitive("n/a")
        }
    }
}
