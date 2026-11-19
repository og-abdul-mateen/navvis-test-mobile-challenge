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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.navvistest.mobilechallenge.R
import com.navvistest.mobilechallenge.domain.ComputationResult
import com.navvistest.mobilechallenge.domain.NumberRow
import com.navvistest.mobilechallenge.domain.ParsedNumber
import com.navvistest.mobilechallenge.domain.RowError
import com.navvistest.mobilechallenge.domain.RowResult
import com.navvistest.mobilechallenge.domain.Section
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

@Composable
fun StreamRoute(viewModel: StreamViewModel, useMock: Boolean) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StreamScreen(uiState = uiState, useMock = useMock)
}

/** Draws a [UiState]. Holds no logic, so it can be previewed with any state. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamScreen(uiState: UiState, useMock: Boolean, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.app_name))
                        Text(
                            text = stringResource(if (useMock) R.string.backend_mock else R.string.backend_real),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        val contentModifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (uiState) {
            UiState.Loading -> CenteredMessage(stringResource(R.string.state_loading), contentModifier)
            is UiState.Empty -> CenteredMessage(stringResource(R.string.state_empty), contentModifier)
            is UiState.Error -> CenteredMessage(stringResource(R.string.state_error), contentModifier)
            is UiState.Content -> SectionList(uiState.sections, contentModifier)
        }
    }
}

@Composable
private fun SectionList(sections: List<Section>, modifier: Modifier = Modifier) {
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
    }
}

private fun LazyListScope.section(section: Section) {
    // Keys keep each item's identity when rows move as results arrive.
    // Headers and rows use different key types, so they can't clash.
    item(key = "section-${section.index}", contentType = "header") {
        SectionHeader(section.title)
    }
    section.rows.forEach { row ->
        item(key = row.index, contentType = "row") {
            NumberRowItem(row)
        }
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
        Text(text, style = MaterialTheme.typography.bodyLarge)
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
            uiState = UiState.Content(sections, rowCount = 4, skippedCount = 0, isFetching = true, failure = null),
            useMock = true,
        )
    }
}
