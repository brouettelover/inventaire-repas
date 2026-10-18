package fr.inventairerepas.ui.recipe.sync

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.inventairerepas.data.backup.RecipeFileStore
import fr.inventairerepas.domain.backup.MergeResult
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.usecase.ExportRecipesUseCase
import fr.inventairerepas.domain.usecase.ImportRecipesUseCase
import fr.inventairerepas.ui.week.userMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecipeSyncUiState(
    val busy: Boolean = false,
    /** Nom du fichier partagé mémorisé, ou null si aucun. */
    val fileName: String? = null,
    val hasFile: Boolean = false,
    val message: String? = null,
)

/**
 * Fichier de recettes partagé (par exemple sur Google Drive) :
 * exporter, importer, et synchroniser = importer puis réécrire le fichier avec le résultat de la fusion.
 */
@HiltViewModel
class RecipeSyncViewModel @Inject constructor(
    private val files: RecipeFileStore,
    private val settings: SettingsRepository,
    private val exportRecipes: ExportRecipesUseCase,
    private val importRecipes: ImportRecipesUseCase,
) : ViewModel() {

    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private val file = settings.settings.map { s ->
        s.recipesFileUri?.let { uri -> uri to (files.displayName(Uri.parse(uri)) ?: "fichier de recettes") }
    }

    val state: StateFlow<RecipeSyncUiState> = combine(file, busy, message) { f, b, m ->
        RecipeSyncUiState(busy = b, fileName = f?.second, hasFile = f != null, message = m)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecipeSyncUiState())

    fun exportTo(uri: Uri) = launchTask {
        files.write(uri, exportRecipes())
        rememberFile(uri)
        "Recettes exportées. Ce fichier sera utilisé par « Synchroniser »."
    }

    fun importFrom(uri: Uri) = launchTask {
        val report = importRecipes(files.read(uri))
        rememberFile(uri)
        report.summary()
    }

    fun sync() = launchTask {
        val uri = settings.settings.first().recipesFileUri?.let(Uri::parse)
            ?: return@launchTask "Choisis d'abord un fichier de recettes (exporter ou importer)"
        val report = importRecipes(files.read(uri))
        files.write(uri, exportRecipes())
        "Synchronisé. ${report.summary()}"
    }

    fun forgetFile() {
        viewModelScope.launch {
            settings.settings.first().recipesFileUri?.let { files.forget(Uri.parse(it)) }
            settings.update { it.copy(recipesFileUri = null) }
        }
    }

    fun messageShown() {
        message.value = null
    }

    private suspend fun rememberFile(uri: Uri) {
        files.remember(uri)
        settings.update { it.copy(recipesFileUri = uri.toString()) }
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
