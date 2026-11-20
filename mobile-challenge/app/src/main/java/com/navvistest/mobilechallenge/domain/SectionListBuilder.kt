package com.navvistest.mobilechallenge.domain

/**
 * Groups rows into sections and sorts them for display.
 *
 * - Sections are ordered by index. Sections without rows are left out.
 * - Rows in a section are ordered by item value, then by result:
 *   no result, results from low to high, pending, errors.
 * - Rows that are otherwise equal keep their order in the stream.
 */
object SectionListBuilder {

    private val rowOrder: Comparator<NumberRow> = compareBy<NumberRow>(
        { it.number.itemValue },
        { resultRank(it.result) },
        { (it.result as? RowResult.Ready)?.result?.value },
        { it.index },
    )

    fun build(rows: Collection<NumberRow>): List<Section> =
        rows.groupBy { it.number.sectionIndex }
            .toSortedMap()
            .map { (sectionIndex, sectionRows) ->
                Section(
                    index = sectionIndex,
                    title = NumberParser.SECTION_TITLES[sectionIndex],
                    rows = sectionRows.sortedWith(rowOrder),
                )
            }

    private fun resultRank(result: RowResult): Int = when (result) {
        RowResult.NotNeeded -> 0
        is RowResult.Ready -> 1
        RowResult.Pending -> 2
        is RowResult.Failed -> 3
    }
}
