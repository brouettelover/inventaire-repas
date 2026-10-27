package fr.inventairerepas.ui.recipe.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val RECIPES_FILE_NAME = "recettes-inventaire-repas.json"

/** Actions de la barre de titre de l'écran Recettes : synchroniser avec Drive, exporter, importer. */
@Composable
fun RecipeSyncMenu(state: RecipeSyncUiState, viewModel: RecipeSyncViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }

    if (state.busy || state.driveRunning) {
        Box(Modifier.size(48.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center))
        }
    } else {
        IconButton(onClick = viewModel::syncWithDrive) {
            Icon(Icons.Filled.Refresh, contentDescription = "Synchroniser avec Google Drive")
        }
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("Exporter une sauvegarde…") },
                enabled = !state.busy,
                onClick = {
                    expanded = false
                    exportLauncher.launch(RECIPES_FILE_NAME)
                },
            )
            DropdownMenuItem(
                text = { Text("Importer une sauvegarde…") },
                enabled = !state.busy,
                onClick = {
                    expanded = false
                    // Drive renvoie parfois les fichiers JSON en text/plain ou octet-stream.
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
            )
        }
    }
}

/** Bandeau en haut de la liste quand la synchronisation Drive demande une action ou a rencontré un problème. */
@Composable
fun DriveSyncBanner(state: RecipeSyncUiState, viewModel: RecipeSyncViewModel) {
    val error = state.driveError ?: return
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onConsentResult(result.data)
    }
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(error, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            if (state.needsConsent) {
                TextButton(onClick = {
                    viewModel.consentIntent?.let {
                        consentLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build())
                    }
                }) { Text("Autoriser") }
            } else {
                TextButton(onClick = viewModel::syncWithDrive) { Text("Réessayer") }
            }
        }
    }
}
