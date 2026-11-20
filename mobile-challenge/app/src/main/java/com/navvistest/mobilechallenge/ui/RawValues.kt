package com.navvistest.mobilechallenge.ui

import com.navvistest.mobilechallenge.data.RawValue
import com.navvistest.mobilechallenge.domain.NumberRow

/**
 * The line shown under a row when "Show raw values" is on, so anyone can check a row by hand:
 * the number, its bits split as result | item | section, and the input it was paired with.
 *
 * For example `150 · 1 00101 10 · input 25`.
 *
 * The number is rebuilt from the parsed fields. That gives back the exact original, because
 * a valid number is fully described by its three fields.
 *
 * Debug text for the mock build, so it isn't in strings.xml.
 */
fun rawValuesText(row: NumberRow, input: RawValue?): String {
    val number = row.number
    val resultBit = if (number.showsResult) 1 else 0
    val value = (resultBit shl 7) or (number.itemValue shl 2) or number.sectionIndex
    val bits = value.toString(2).padStart(8, '0')

    val parts = mutableListOf("$value", "${bits[0]} ${bits.substring(1, 6)} ${bits.substring(6)}")
    // Rows without the result bit never use their input, so it isn't shown.
    if (number.showsResult) {
        parts += "input " + when (input) {
            null -> "not received"
            RawValue.Null -> "null"
            is RawValue.Integer -> input.value.toString()
            is RawValue.Invalid -> input.raw
        }
    }
    return parts.joinToString(" · ")
}
