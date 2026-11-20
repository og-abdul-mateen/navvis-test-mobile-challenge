package com.navvistest.mobilechallenge.data

/**
 * One element of a JSON array from either endpoint, before any domain rules are applied.
 *
 * Range checks (0..255 for numbers, 0..65535 for inputs) are left to the domain layer,
 * because the two endpoints have different rules.
 */
sealed interface RawValue {
    /** A JSON number with no fraction that fits in a Long. */
    data class Integer(val value: Long) : RawValue

    /** JSON `null`: the server couldn't deliver this value. */
    data object Null : RawValue

    /** Anything else: a string, a decimal, a boolean, an object or a nested array. */
    data class Invalid(val raw: String) : RawValue
}
