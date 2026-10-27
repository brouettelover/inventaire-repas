package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.drive.DriveLink
import fr.inventairerepas.domain.drive.DriveLinkRepository
import fr.inventairerepas.domain.drive.DriveSyncPlanner
import fr.inventairerepas.domain.drive.RecipeRemote
import fr.inventairerepas.domain.drive.RecipeText
import fr.inventairerepas.domain.drive.SyncAction
import fr.inventairerepas.domain.repository.RecipeRepository
import javax.inject.Inject

data class DriveSyncReport(
    val downloaded: Int = 0,
    val uploaded: Int = 0,
    val deletedLocally: Int = 0,
    val trashedRemotely: Int = 0,
    /** Fichiers illisibles ou opérations échouées, avec la raison ; les autres recettes sont quand même synchronisées. */
    val errors: List<String> = emptyList(),
) {
    val changed: Boolean get() = downloaded + uploaded + deletedLocally + trashedRemotely > 0
}

/** Synchronise les recettes de l'app avec le dossier de recettes sur Drive, dans les deux sens. */
class SyncRecipesWithDriveUseCase @Inject constructor(
    private val recipes: RecipeRepository,
    private val links: DriveLinkRepository,
) {
    suspend operator fun invoke(remote: RecipeRemote): DriveSyncReport {
        val actions = DriveSyncPlanner.plan(recipes.getAll(), links.getAll(), remote.list())
        var report = DriveSyncReport()
        actions.forEach { action ->
            runCatching { report = execute(action, remote, report) }
                .onFailure { error -> report = report.copy(errors = report.errors + describe(action, error)) }
        }
        return report
    }

    private suspend fun execute(action: SyncAction, remote: RecipeRemote, report: DriveSyncReport): DriveSyncReport =
        when (action) {
            is SyncAction.Download -> {
                val parsed = RecipeText.parse(action.file.name, remote.read(action.file.id))
                val id = recipes.save(parsed.copy(id = action.recipeId ?: 0, updatedAt = action.file.modifiedAt))
                action.staleLink?.let { links.delete(it) }
                links.save(DriveLink(id, action.file.id, action.file.modifiedAt, action.file.modifiedAt))
                report.copy(downloaded = report.downloaded + 1)
            }
            is SyncAction.Upload -> {
                val text = RecipeText.format(action.recipe)
                val file = action.fileId?.let { remote.update(it, action.recipe.name, text) }
                    ?: remote.create(action.recipe.name, text)
                links.save(DriveLink(action.recipe.id, file.id, file.modifiedAt, action.recipe.updatedAt))
                report.copy(uploaded = report.uploaded + 1)
            }
            is SyncAction.DeleteLocal -> {
                recipes.delete(action.recipeId)
                links.delete(action.recipeId)
                report.copy(deletedLocally = report.deletedLocally + 1)
            }
            is SyncAction.TrashRemote -> {
                remote.trash(action.fileId)
                links.delete(action.recipeId)
                report.copy(trashedRemotely = report.trashedRemotely + 1)
            }
            is SyncAction.DropLink -> {
                links.delete(action.recipeId)
                report
            }
        }

    private fun describe(action: SyncAction, error: Throwable): String {
        val subject = when (action) {
            is SyncAction.Download -> "« ${action.file.name} »"
            is SyncAction.Upload -> "« ${action.recipe.name} »"
            is SyncAction.DeleteLocal -> "suppression d'une recette"
            is SyncAction.TrashRemote -> "suppression d'un fichier"
            is SyncAction.DropLink -> "nettoyage"
        }
        return "$subject : ${error.message ?: "erreur inconnue"}"
    }
}
