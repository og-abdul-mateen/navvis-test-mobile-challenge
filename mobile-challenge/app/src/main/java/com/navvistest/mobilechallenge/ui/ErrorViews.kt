package com.navvistest.mobilechallenge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.navvistest.mobilechallenge.R
import com.navvistest.mobilechallenge.data.ApiError
import com.navvistest.mobilechallenge.ui.theme.MobileChallengeTheme

/** Full screen error states for when the numbers stream fails before anything could be shown. */
@Composable
fun ErrorState(error: ApiError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.state_error), style = MaterialTheme.typography.titleMedium)
        Text(
            text = error.describe(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
        )
        Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

/**
 * Banner which can be added above the list when a stream fails after rows are already on screen.
 * The rows stay where they are and only the failed stream is fetched again on retry.
 */
@Composable
fun StreamFailureBanner(failure: StreamFailure, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val title = when (failure.stream) {
        StreamName.Numbers -> stringResource(R.string.banner_numbers_failed)
        StreamName.Inputs -> stringResource(R.string.banner_inputs_failed)
    }
    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium)
                Text(failure.error.describe(), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** Plain textual description for the users. */
@Composable
private fun ApiError.describe(): String = when (this) {
    is ApiError.Http -> if (code in 500..599) {
        stringResource(R.string.error_server, code)
    } else {
        stringResource(R.string.error_request_failed, code)
    }
    ApiError.Timeout -> stringResource(R.string.error_timeout)
    is ApiError.Network -> stringResource(R.string.error_no_connection)
    is ApiError.MalformedBody -> stringResource(R.string.error_unreadable)
}

@Preview(showBackground = true)
@Composable
private fun StreamFailureBannerPreview() {
    MobileChallengeTheme {
        StreamFailureBanner(StreamFailure(StreamName.Inputs, ApiError.Http(503)), onRetry = {})
    }
}
