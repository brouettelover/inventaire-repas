package fr.inventairerepas.ui.settings

import android.util.Patterns
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import fr.inventairerepas.sync.DriveSyncState
import fr.inventairerepas.ui.common.AppScaffold
import fr.inventairerepas.ui.common.ServingsStepper
import fr.inventairerepas.ui.common.formatMinutes
import fr.inventairerepas.ui.common.label
import fr.inventairerepas.ui.export.GoogleAccountSection
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DURATION_OPTIONS: List<Int?> = listOf(null, 15, 20, 30, 45, 60, 90, 120)

private fun durationLabel(minutes: Int?) = minutes?.let { "≤ ${formatMinutes(it)}" } ?: "Illimitée"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
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
            GoogleAccountSection(email = settings.googleAccountEmail, onAccountChange = viewModel::setGoogleAccount)
            if (settings.googleAccountEmail != null) {
                DriveRecipesSection(
                    drive = state.drive,
                    onSyncNow = viewModel::syncRecipesNow,
                    onFindFolderAgain = viewModel::findDriveFolderAgain,
                )
            }

            HorizontalDivider()
            Text("Partage", style = MaterialTheme.typography.titleMedium)
            if (!state.loading) {
                ShareEmailField(current = settings.shareWithEmail, onSave = viewModel::setShareWithEmail)
            }
        }
    }
}

/** État de la synchronisation des recettes avec le dossier Drive « recettes ». */
@Composable
private fun DriveRecipesSection(drive: DriveSyncState, onSyncNow: () -> Unit, onFindFolderAgain: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Recettes sur Google Drive", style = MaterialTheme.typography.titleSmall)
        Text(
            "Les recettes sont synchronisées avec le dossier « recettes » de ton Drive : un Google Doc par recette.",
            style = MaterialTheme.typography.bodySmall,
        )
        val last = drive.lastSyncAt?.let {
            DateTimeFormatter.ofPattern("HH:mm").format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
        }
        Text(
            when {
                drive.running -> "Synchronisation en cours…"
                last != null -> "Dernière synchronisation à $last."
                else -> "Pas encore synchronisé depuis l'ouverture de l'app."
            },
        )
        drive.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(enabled = !drive.running, onClick = onSyncNow) { Text("Synchroniser") }
            OutlinedButton(enabled = !drive.running, onClick = onFindFolderAgain) { Text("Rechercher le dossier") }
        }
    }
}

/** Adresse Gmail avec qui partager chaque Google Doc exporté. */
@Composable
private fun ShareEmailField(current: String?, onSave: (String?) -> Unit) {
    var text by remember(current) { mutableStateOf(current.orEmpty()) }
    val trimmed = text.trim()
    val valid = trimmed.isEmpty() || Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Partager les Google Docs avec") },
            placeholder = { Text("adresse@gmail.com") },
            singleLine = true,
            isError = !valid,
            supportingText = {
                Text(if (valid) "Cette personne pourra modifier les documents exportés." else "Adresse e-mail invalide")
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        if (trimmed != current.orEmpty()) {
            Button(enabled = valid, onClick = { onSave(trimmed.ifEmpty { null }) }) { Text("Enregistrer") }
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
