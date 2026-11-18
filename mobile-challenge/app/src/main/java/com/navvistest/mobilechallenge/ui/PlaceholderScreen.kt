package com.navvistest.mobilechallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.navvistest.mobilechallenge.R
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

/**
 * Temporary screen until the stream list is built.
 * [summary] is a one-line description of the current state, to check the ViewModel works.
 */
@Composable
fun PlaceholderScreen(useMock: Boolean, summary: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = stringResource(if (useMock) R.string.backend_mock else R.string.backend_real),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = summary,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

/** Not translated on purpose: this text goes away with the placeholder. */
fun UiState.summary(): String = when (this) {
    UiState.Loading -> "Loading"
    is UiState.Empty -> "Empty, $skippedCount skipped"
    is UiState.Error -> "Error: $error"
    is UiState.Content -> buildString {
        append("$rowCount rows in ${sections.size} sections, $skippedCount skipped")
        append(if (isFetching) ", fetching" else ", done")
        failure?.let { append(", ${it.stream} failed: ${it.error}") }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderScreenPreview() {
    MobileChallengeTheme {
        PlaceholderScreen(useMock = true, summary = UiState.Loading.summary())
    }
}
