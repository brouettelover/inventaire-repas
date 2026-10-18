package fr.inventairerepas.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.ServingsStepper
import fr.inventairerepas.ui.common.formatMinutes
import fr.inventairerepas.ui.common.label
import java.time.DayOfWeek

private val DURATION_OPTIONS: List<Int?> = listOf(null, 15, 20, 30, 45, 60, 90, 120)

private fun durationLabel(minutes: Int?) = minutes?.let { "≤ ${formatMinutes(it)}" } ?: "Illimitée"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    googleAccountSection: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings = state.settings

    AppScaffold(title = "Réglages", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Courses", style = MaterialTheme.typography.titleMedium)
            SettingRow("Premier jour de courses") {
                Picker(
                    current = settings.firstShoppingDay,
                    options = DayOfWeek.entries,
                    label = { it.label() },
                    onSelect = viewModel::setFirstShoppingDay,
                )
            }
            Text(
                "Second jour de courses : ${settings.firstShoppingDay.plus(3).label()}. " +
                    "Le 7e jour (${settings.firstShoppingDay.plus(6).label()}) est rattaché au second bloc.",
                style = MaterialTheme.typography.bodySmall,
            )
            SettingRow("Personnes par défaut") {
                ServingsStepper(settings.defaultServings, viewModel::setDefaultServings)
            }

            HorizontalDivider()
            Text("Durée maximale par jour (tirage aléatoire)", style = MaterialTheme.typography.titleMedium)
            DayOfWeek.entries.forEach { day ->
                SettingRow(day.label()) {
                    Picker(
                        current = settings.maxMinutesFor(day),
                        options = (DURATION_OPTIONS + settings.maxMinutesFor(day)).distinct()
                            .sortedBy { it ?: Int.MAX_VALUE },
                        label = ::durationLabel,
                        onSelect = { viewModel.setMaxMinutes(day, it) },
                    )
                }
            }

            HorizontalDivider()
            Text("Compte Google", style = MaterialTheme.typography.titleMedium)
            googleAccountSection()
        }
    }
}

@Composable
private fun SettingRow(title: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun <T> Picker(current: T, options: List<T>, label: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }) { Text(label(current)) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(label(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}
