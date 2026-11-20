package com.navvistest.mobilechallenge.domain

/**
 * One entry in the list: a valid number and, if its result bit is set, the state of its result.
 *
 * @property index Position in the numbers stream. Also used as a stable key and to keep
 * arrival order when two rows would otherwise sort the same.
 */
data class NumberRow(
    val index: Int,
    val number: ParsedNumber,
    val result: RowResult,
)

sealed interface RowResult {
    /** The result bit isn't set, so no result is shown. */
    data object NotNeeded : RowResult

    /** The input hasn't arrived yet, or the computation is still running. */
    data object Pending : RowResult

    data class Ready(val result: ComputationResult.Success) : RowResult

    data class Failed(val reason: RowError) : RowResult
}

enum class RowError {
    /** The inputs stream finished before reaching this row's index. */
    MissingInput,

    /** The input was null, not an integer, or outside 0..65535. */
    InvalidInput,

    /** The computation took longer than the timeout. */
    TimedOut,
}

data class Section(
    val index: Int,
    val title: String,
    val rows: List<NumberRow>,
)
