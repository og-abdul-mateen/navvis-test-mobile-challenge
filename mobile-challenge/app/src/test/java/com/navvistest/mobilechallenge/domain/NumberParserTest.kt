package com.navvistest.mobilechallenge.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NumberParserTest {

    @Test
    fun `parses the example numbers from the challenge`() {
        val expected = mapOf(
            4L to ParsedNumber(sectionIndex = 0, itemValue = 1, showsResult = false),
            150L to ParsedNumber(sectionIndex = 2, itemValue = 5, showsResult = true),
            12L to ParsedNumber(sectionIndex = 0, itemValue = 3, showsResult = false),
            21L to ParsedNumber(sectionIndex = 1, itemValue = 5, showsResult = false),
            136L to ParsedNumber(sectionIndex = 0, itemValue = 2, showsResult = true),
            16L to ParsedNumber(sectionIndex = 0, itemValue = 4, showsResult = false),
        )

        expected.forEach { (value, number) ->
            assertEquals("value $value", number, NumberParser.parse(value))
        }
    }

    @Test
    fun `maps indexes to titles`() {
        val number = NumberParser.parse(150)!!

        assertEquals("SECTION 3", number.sectionTitle)
        assertEquals("Item 6", number.itemTitle)
    }

    @Test
    fun `parses the lowest and highest valid bytes`() {
        assertEquals(ParsedNumber(0, 0, false), NumberParser.parse(0))
        // 0b1_00101_11: every field at its highest valid value.
        assertEquals(ParsedNumber(3, 5, true), NumberParser.parse(151))
    }

    @Test
    fun `rejects missing and out of range values`() {
        assertNull(NumberParser.parse(null))
        assertNull(NumberParser.parse(-1))
        assertNull(NumberParser.parse(256))
    }

    @Test
    fun `rejects item values without a title`() {
        // Item value 6, the first one without a title.
        assertNull(NumberParser.parse(0b0_00110_00))
        // 255 is in range, but its item value is 31.
        assertNull(NumberParser.parse(255))
    }
}
