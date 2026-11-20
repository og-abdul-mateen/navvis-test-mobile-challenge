package com.navvistest.mobilechallenge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import com.navvistest.mobilechallenge.R
import com.navvistest.mobilechallenge.data.mock.MockScenarioType

/**
 * Top bar menu of the mock build. Picking a scenario, even the current one, will start a fresh run.
 * The last item turns the raw values under each row on and off.
 */
@Composable
fun ScenarioMenu(
    current: MockScenarioType,
    onSelected: (MockScenarioType) -> Unit,
    showRawValues: Boolean,
    onToggleRawValues: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }, shape = RectangleShape) {
            Text(stringResource(R.string.scenario_menu))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MockScenarioType.entries.forEach { scenario ->
                DropdownMenuItem(
                    text = { Text(scenario.label) },
                    leadingIcon = { RadioButton(selected = scenario == current, onClick = null) },
                    onClick = {
                        expanded = false
                        onSelected(scenario)
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Show raw values") },
                leadingIcon = { Checkbox(checked = showRawValues, onCheckedChange = null) },
                onClick = {
                    expanded = false
                    onToggleRawValues()
                },
            )
        }
    }
}
