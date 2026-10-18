package fr.inventairerepas.ui.recipe.sync

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

const val RECIPES_FILE_NAME = "recettes-inventaire-repas.json"

/** Actions de la barre de titre de l'écran Recettes : synchroniser, exporter, importer. */
@Composable
fun RecipeSyncMenu(state: RecipeSyncUiState, viewModel: RecipeSyncViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }

    if (state.busy) {
        Box(Modifier.size(48.dp)) {
            CircularProgressIndicator(Modifier.size(24.dp).align(Alignment.Center))
        }
    } else if (state.hasFile) {
        IconButton(onClick = viewModel::sync) {
            Icon(Icons.Filled.Refresh, contentDescription = "Synchroniser les recettes")
        }
    }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Plus d'options")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (state.hasFile) {
                DropdownMenuItem(
                    text = { Text("Synchroniser avec ${state.fileName}") },
                    enabled = !state.busy,
                    onClick = {
                        expanded = false
                        viewModel.sync()
                    },
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Exporter vers un fichier…") },
                enabled = !state.busy,
                onClick = {
                    expanded = false
                    exportLauncher.launch(RECIPES_FILE_NAME)
                },
            )
            DropdownMenuItem(
                text = { Text("Importer un fichier…") },
                enabled = !state.busy,
                onClick = {
                    expanded = false
                    // Drive renvoie parfois les fichiers JSON en text/plain ou octet-stream.
                    importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                },
            )
            if (state.hasFile) {
                DropdownMenuItem(
                    text = { Text("Oublier le fichier partagé") },
                    onClick = {
                        expanded = false
                        viewModel.forgetFile()
                    },
                )
            }
        }
    }
}
