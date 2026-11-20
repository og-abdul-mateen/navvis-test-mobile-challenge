package com.navvistest.mobilechallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navvistest.mobilechallenge.R
import com.navvistest.mobilechallenge.data.mock.MockScenarioType
import com.navvistest.mobilechallenge.domain.ComputationResult
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.ParsedNumber
import com.navvistest.mobilechallenge.domain.RowError
import com.navvistest.mobilechallenge.domain.RowResult
import com.navvistest.mobilechallenge.domain.Section
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme
import com.navvistest.mobilechallenge.ui.theme.NavVisBlue

@Composable
fun StreamRoute(viewModel: StreamViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scenario by viewModel.scenario.collectAsStateWithLifecycle()
    StreamScreen(
        uiState = uiState,
        scenario = scenario,
        onRetry = viewModel::retry,
        onScenarioSelected = viewModel::selectScenario,
    )
}

/** Draws a [UiState]. Holds no logic, so it can be previewed with any state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(
    uiState: UiState,
    scenario: MockScenarioType?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    onScenarioSelected: (MockScenarioType) -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = if (scenario != null) {
                                stringResource(R.string.backend_mock, scenario.label)
                            } else {
                                stringResource(R.string.backend_real)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    if (scenario != null) ScenarioMenu(current = scenario, onSelected = onScenarioSelected)
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            UiState.Loading -> LoadingState(contentModifier)
            is UiState.Empty -> {
                // Nothing at all vs. only bad values: the second case tells the user data did arrive.
                val message = when (val skipped = uiState.skippedCount) {
                    0 -> stringResource(R.string.state_empty)
                    else -> pluralStringResource(R.plurals.state_empty_all_skipped, skipped, skipped)
                }
                CenteredMessage(message, contentModifier)
            }
            is UiState.Error -> ErrorState(uiState.error, onRetry, contentModifier)
            is UiState.Content -> Column(contentModifier) {
                uiState.failure?.let { StreamFailureBanner(it, onRetry) }
                Box(Modifier.weight(1f)) {
                    SectionList(
                        sections = uiState.sections,
                        footer = if (uiState.isComplete) {
                            { SummaryFooter(uiState) }
                        } else {
                            null
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                    // Drawn over the list instead of above it, so rows don't shift when it goes away.
                    if (uiState.isFetching) {
                        LinearProgressIndicator(
                            color = NavVisBlue,
                            trackColor = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun SectionList(
    sections: List<Section>,
    footer: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keys make the list hold on to the first visible item, so rows inserted above it would
    // push the top of the list out of view. While the user is still at the very top, it stays there.
    // The position is read without observation so scrolling doesn't recompose this function.
    val isAtTop = Snapshot.withoutReadObservation {
        listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
    }
    if (isAtTop) listState.requestScrollToItem(0)

    LazyColumn(state = listState, modifier = modifier) {
        sections.forEach { section(it) }
        if (footer != null) {
            item(key = "footer", contentType = "footer") { footer() }
        }
    }
}

/** Summary shown under the list once both streams are finished, for example "200 numbers shown, 3 skipped, 97 results". */
@Composable
private fun SummaryFooter(state: UiState.Content) {
    val parts = listOf(
        pluralStringResource(R.plurals.summary_numbers_shown, state.rowCount, state.rowCount),
        stringResource(R.string.summary_skipped, state.skippedCount),
        pluralStringResource(R.plurals.summary_results, state.resultCount, state.resultCount),
    )
    Text(
        text = parts.joinToString(separator = ", "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
    )
}

private fun LazyListScope.section(section: Section) {
    // Keys keep each item's identity when rows move as results arrive.
    // Headers and rows use different key types, so they can't clash.
    item(key = "section-${section.index}", contentType = "header") {
        SectionHeader(section.title)
    }
    // One items() call per section. Calling item() for every row would walk all of them on the
    // main thread each time the list updates; items() only looks at the rows on screen.
    items(section.rows, key = { it.index }, contentType = { "row" }) { row ->
        NumberRowItem(row)
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    )
    HorizontalDivider()
}

@Composable
private fun NumberRowItem(row: NumberRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.number.itemTitle, style = MaterialTheme.typography.bodyLarge)
        RowResultText(row.result)
    }
    HorizontalDivider()
}

@Composable
private fun RowResultText(result: RowResult) {
    when (result) {
        RowResult.NotNeeded -> Unit
        RowResult.Pending -> {
            val description = stringResource(R.string.row_pending)
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                modifier = Modifier
                    .size(16.dp)
                    .semantics { contentDescription = description },
            )
        }
        is RowResult.Ready -> Text(
            text = stringResource(R.string.row_result, result.result.formatted),
            style = MaterialTheme.typography.bodyMedium,
        )
        is RowResult.Failed -> Text(
            text = stringResource(errorText(result.reason)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

private fun errorText(reason: RowError): Int = when (reason) {
    RowError.MissingInput -> R.string.row_error_missing_input
    RowError.InvalidInput -> R.string.row_error_invalid_input
    RowError.TimedOut -> R.string.row_error_timed_out
}

@Composable
private fun CenteredMessage(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(16.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStatePreview() {
    MobileChallengeTheme {
        StreamScreen(uiState = UiState.Empty(skippedCount = 12), scenario = MockScenarioType.Normal, onRetry = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun StreamScreenPreview() {
    fun row(index: Int, item: Int, result: RowResult) =
        NumberRow(index, ParsedNumber(0, item, result != RowResult.NotNeeded), result)

    val sections = listOf(
        Section(
            index = 0,
            title = "SECTION 1",
            rows = listOf(
                row(0, 3, RowResult.NotNeeded),
                row(1, 3, RowResult.Ready(ComputationResult.Success(3))),
                row(2, 3, RowResult.Pending),
                row(3, 4, RowResult.Failed(RowError.TimedOut)),
            ),
        ),
    )
    MobileChallengeTheme {
        StreamScreen(
            uiState = UiState.Content(
                sections,
                rowCount = 4,
                skippedCount = 0,
                resultCount = 1,
                isFetching = true,
                failure = null,
            ),
            scenario = MockScenarioType.Normal,
            onRetry = {},
        )
    }
}
