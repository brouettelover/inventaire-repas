package fr.inventairerepas.ui.recipe.sync

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.data.backup.RecipeFileStore
import fr.inventairerepas.domain.backup.MergeResult
import fr.inventairerepas.domain.usecase.ExportRecipesUseCase
import fr.inventairerepas.domain.usecase.ImportRecipesUseCase
import fr.inventairerepas.sync.RecipeDriveSync
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeSyncUiState(
    val busy: Boolean = false,
    val driveRunning: Boolean = false,
    /** Problème de synchronisation Drive à afficher en bandeau, ou null. */
    val driveError: String? = null,
    val needsConsent: Boolean = false,
    val message: String? = null,
)

/**
 * Écran Recettes : synchronisation avec le dossier Drive « recettes » (bouton ⟳),
 * et sauvegarde ponctuelle dans un fichier JSON (exporter, importer).
 */
@HiltViewModel
class RecipeSyncViewModel @Inject constructor(
    private val files: RecipeFileStore,
    private val exportRecipes: ExportRecipesUseCase,
    private val importRecipes: ImportRecipesUseCase,
    private val driveSync: RecipeDriveSync,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    val state: StateFlow<RecipeSyncUiState> = combine(driveSync.state, busy, message) { drive, b, m ->
        RecipeSyncUiState(
            busy = b,
            driveRunning = drive.running,
            driveError = drive.error,
            needsConsent = drive.consent != null,
            message = m ?: drive.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeSyncUiState())

    /** L'intent de consentement Google à lancer, s'il y en a un. */
    val consentIntent get() = driveSync.state.value.consent

    fun syncWithDrive() = driveSync.requestSync(manual = true)

    fun onConsentResult(data: Intent?) = driveSync.onConsentResult(data)

    fun exportTo(uri: Uri) = launchTask {
        files.write(uri, exportRecipes())
        "Recettes exportées."
    }

    fun importFrom(uri: Uri) = launchTask {
        val report = importRecipes(files.read(uri))
        driveSync.requestSync()
        report.summary()
    }

    fun messageShown() {
        if (message.value != null) message.value = null else driveSync.messageShown()
    }

    private fun launchTask(block: suspend () -> String) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            message.value = runCatching { block() }.getOrElse { it.userMessage() }
            busy.value = false
        }
    }

    private fun MergeResult.summary(): String = when {
        added == 0 && updated == 0 -> "Aucune nouveauté."
        else -> buildList {
            if (added > 0) add("$added ajoutée${if (added > 1) "s" else ""}")
            if (updated > 0) add("$updated mise${if (updated > 1) "s" else ""} à jour")
        }.joinToString(", ", prefix = "Recettes : ", postfix = ".")
    }
}
