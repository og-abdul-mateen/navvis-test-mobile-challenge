package com.navvistest.mobilechallenge.domain

/**
 * A number from the numbers stream after its bits have been decoded.
 *
 * @property sectionIndex 0..3, from the two least significant bits.
 * @property itemValue 0..5, from bits 2..6.
 * @property showsResult true when the most significant bit is set.
 */
data class ParsedNumber(
    val sectionIndex: Int,
    val itemValue: Int,
    val showsResult: Boolean,
) {
    val sectionTitle: String get() = NumberParser.SECTION_TITLES[sectionIndex]
    val itemTitle: String get() = NumberParser.ITEM_TITLES[itemValue]
}
