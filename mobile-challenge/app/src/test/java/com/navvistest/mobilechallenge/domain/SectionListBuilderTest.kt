package com.navvistest.mobilechallenge.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SectionListBuilderTest {

    private fun row(
        index: Int,
        section: Int,
        item: Int,
        result: RowResult = RowResult.NotNeeded,
    ) = NumberRow(
        index = index,
        number = ParsedNumber(section, item, showsResult = result != RowResult.NotNeeded),
        result = result,
    )

    private fun ready(value: Int) = RowResult.Ready(ComputationResult.Success(value))

    /** Renders sections the way the challenge draws them, for easy comparison. */
    private fun List<Section>.render(): List<String> = flatMap { section ->
        listOf(section.title) + section.rows.map { row ->
            val result = row.result
            when (result) {
                is RowResult.Ready -> "${row.number.itemTitle} result=${result.result.formatted}"
                RowResult.Pending -> "${row.number.itemTitle} pending"
                is RowResult.Failed -> "${row.number.itemTitle} error=${result.reason}"
                RowResult.NotNeeded -> row.number.itemTitle
            }
        }
    }

    @Test
    fun `matches the example layout from the challenge`() {
        // Same rows as the challenge's example, in a scrambled arrival order.
        val rows = listOf(
            row(index = 0, section = 1, item = 1, result = ready(1)),
            row(index = 1, section = 0, item = 3, result = ready(12)),
            row(index = 2, section = 0, item = 4),
            row(index = 3, section = 0, item = 3, result = ready(3)),
            row(index = 4, section = 0, item = 3),
        )

        val expected = listOf(
            "SECTION 1",
            "Item 4",
            "Item 4 result=03",
            "Item 4 result=12",
            "Item 5",
            "SECTION 2",
            "Item 2 result=01",
        )
        assertEquals(expected, SectionListBuilder.build(rows).render())
    }

    @Test
    fun `orders sections by index and leaves out empty ones`() {
        val rows = listOf(
            row(index = 0, section = 3, item = 0),
            row(index = 1, section = 0, item = 0),
        )

        val sections = SectionListBuilder.build(rows)

        assertEquals(listOf("SECTION 1", "SECTION 4"), sections.map { it.title })
    }

    @Test
    fun `orders rows with the same item by result state`() {
        val rows = listOf(
            row(index = 0, section = 0, item = 2, result = RowResult.Failed(RowError.TimedOut)),
            row(index = 1, section = 0, item = 2, result = RowResult.Pending),
            row(index = 2, section = 0, item = 2, result = ready(40)),
            row(index = 3, section = 0, item = 2),
            row(index = 4, section = 0, item = 2, result = ready(7)),
        )

        val expected = listOf(
            "SECTION 1",
            "Item 3",
            "Item 3 result=07",
            "Item 3 result=40",
            "Item 3 pending",
            "Item 3 error=TimedOut",
        )
        assertEquals(expected, SectionListBuilder.build(rows).render())
    }

    @Test
    fun `keeps stream order for rows that are otherwise equal`() {
        val rows = listOf(
            row(index = 9, section = 0, item = 1, result = RowResult.Pending),
            row(index = 2, section = 0, item = 1, result = RowResult.Pending),
            row(index = 5, section = 0, item = 1, result = RowResult.Pending),
        )

        val sections = SectionListBuilder.build(rows)

        assertEquals(listOf(2, 5, 9), sections.single().rows.map { it.index })
    }
}
