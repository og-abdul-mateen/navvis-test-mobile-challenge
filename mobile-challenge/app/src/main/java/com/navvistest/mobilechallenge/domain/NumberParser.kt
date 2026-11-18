package com.navvistest.mobilechallenge.domain

/**
 * Decodes a byte from the numbers stream. Bit layout, most significant first:
 *
 * ```
 *  bit:   7   6 5 4 3 2   1 0
 *         R   I I I I I   S S
 * ```
 *
 * S = section index, I = item value, R = result flag.
 */
object NumberParser {

    val SECTION_TITLES = listOf("SECTION 1", "SECTION 2", "SECTION 3", "SECTION 4")
    val ITEM_TITLES = listOf("Item 1", "Item 2", "Item 3", "Item 4", "Item 5", "Item 6")

    private const val SECTION_MASK = 0b11
    private const val ITEM_SHIFT = 2
    private const val ITEM_MASK = 0b1_1111
    private const val RESULT_SHIFT = 7

    /**
     * Returns null when the value can't be shown: it's missing (the server sent null or a
     * non-integer), it's outside 0..255, or its item value has no title (6..31).
     */
    fun parse(value: Long?): ParsedNumber? {
        if (value == null || value !in 0L..255L) return null
        val byte = value.toInt()

        val sectionIndex = byte and SECTION_MASK
        val itemValue = (byte shr ITEM_SHIFT) and ITEM_MASK
        val showsResult = (byte shr RESULT_SHIFT) == 1

        if (itemValue !in ITEM_TITLES.indices) return null
        return ParsedNumber(sectionIndex, itemValue, showsResult)
    }
}
