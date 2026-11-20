package com.navvistest.mobilechallenge.ui

import com.navvistest.mobilechallenge.data.RawValue
import com.navvistest.mobilechallenge.domain.NumberParser
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.RowResult
import org.junit.Assert.assertEquals
import org.junit.Test

class RawValuesTest {

    private fun row(value: Long) = NumberRow(index = 0, number = NumberParser.parse(value)!!, result = RowResult.Pending)

    @Test
    fun `shows the number, its bits and the input`() {
        assertEquals("150 · 1 00101 10 · input 25", rawValuesText(row(150), RawValue.Integer(25)))
    }

    @Test
    fun `leaves out the input when the row needs no result`() {
        assertEquals("4 · 0 00001 00", rawValuesText(row(4), RawValue.Integer(10)))
    }

    @Test
    fun `says what is wrong with the input`() {
        assertEquals("136 · 1 00010 00 · input not received", rawValuesText(row(136), null))
        assertEquals("136 · 1 00010 00 · input null", rawValuesText(row(136), RawValue.Null))
        assertEquals("136 · 1 00010 00 · input \"n/a\"", rawValuesText(row(136), RawValue.Invalid("\"n/a\"")))
    }
}
