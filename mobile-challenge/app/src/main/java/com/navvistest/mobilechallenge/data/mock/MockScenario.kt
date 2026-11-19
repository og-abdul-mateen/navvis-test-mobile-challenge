package com.navvistest.mobilechallenge.data.mock

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

/**
 * The data a mock server hands out, and how it hands it out.
 *
 * Values are [JsonElement]s so a scenario can include nulls, strings and decimals.
 */
data class MockScenario(
    val numbers: List<JsonElement>,
    val inputs: List<JsonElement>,
    /** Each response holds between 1 and this many values. */
    val maxChunkSize: Int = 25,
    /** Each response waits a random time in this range, in milliseconds. */
    val delayMillis: LongRange = 50L..300L,
    /** Chance that a request gets a 5xx, a 429 or a body that isn't JSON. */
    val serverErrorChance: Double = 0.0,
    /** Chance that a request times out or loses its connection. */
    val networkErrorChance: Double = 0.0,
) {
    companion object {

        /** 200 valid numbers and 200 valid inputs, served in small chunks with short delays. */
        fun happyPath(random: Random = Random.Default, count: Int = 200) = MockScenario(
            numbers = List(count) { validNumber(random) },
            inputs = List(count) { validInput(random) },
        )

        /**
         * Builds a byte that parses to a known section and item. A plain random 0..255 would
         * mostly be invalid, because only item values 0..5 out of 0..31 have a title.
         */
        fun validNumber(random: Random): JsonElement {
            val section = random.nextInt(0, 4)
            val item = random.nextInt(0, 6)
            val resultBit = random.nextInt(0, 2)
            return JsonPrimitive((resultBit shl 7) or (item shl 2) or section)
        }

        fun validInput(random: Random): JsonElement = JsonPrimitive(random.nextInt(0, 65_536))
    }
}
