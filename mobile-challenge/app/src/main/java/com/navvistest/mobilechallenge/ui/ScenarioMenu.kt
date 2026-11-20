package com.navvistest.mobilechallenge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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

/** Top bar menu of the mock build. Picking a scenario, even the current one, will start a fresh run. */
@Composable
fun ScenarioMenu(current: MockScenarioType, onSelected: (MockScenarioType) -> Unit) {
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
        }
    }
}
