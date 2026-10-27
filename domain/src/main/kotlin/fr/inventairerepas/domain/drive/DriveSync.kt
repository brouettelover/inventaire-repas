package fr.inventairerepas.domain.drive

import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe

/** Fichier de recette dans le dossier Drive. [modifiedAt] en millisecondes. */
data class RemoteRecipeFile(val id: String, val name: String, val modifiedAt: Long)

/**
 * Lien entre une recette locale et son fichier Drive, avec l'état des deux côtés lors de la dernière synchronisation.
 * Le lien survit à la suppression de la recette : c'est ainsi qu'une suppression dans l'app est propagée à Drive.
 */
data class DriveLink(
    val recipeId: Long,
    val fileId: String,
    val remoteModifiedAt: Long,
    val localUpdatedAt: Long,
)

/** Accès au dossier de recettes sur Drive (une instance par synchronisation, avec un jeton valide). */
interface RecipeRemote {
    suspend fun list(): List<RemoteRecipeFile>
    suspend fun read(fileId: String): String
    suspend fun create(name: String, text: String): RemoteRecipeFile
    suspend fun update(fileId: String, name: String, text: String): RemoteRecipeFile
    suspend fun trash(fileId: String)
}

interface DriveLinkRepository {
    suspend fun getAll(): List<DriveLink>
    suspend fun save(link: DriveLink)
    suspend fun delete(recipeId: Long)
}

sealed interface SyncAction {
    /**
     * Lire le fichier et créer la recette ([recipeId] null) ou la remplacer.
     * [staleLink] : lien d'une recette supprimée dans l'app, à retirer une fois la recette recréée.
     */
    data class Download(val file: RemoteRecipeFile, val recipeId: Long?, val staleLink: Long? = null) : SyncAction

    /** Écrire la recette dans un nouveau fichier ([fileId] null) ou dans le fichier existant. */
    data class Upload(val recipe: Recipe, val fileId: String?) : SyncAction

    data class DeleteLocal(val recipeId: Long) : SyncAction

    data class TrashRemote(val fileId: String, val recipeId: Long) : SyncAction

    data class DropLink(val recipeId: Long) : SyncAction
}

/**
 * Décide quoi faire pour mettre l'app et le dossier Drive d'accord. Drive fait foi pour les suppressions :
 * - fichier modifié sur Drive → recette mise à jour dans l'app, et inversement ;
 * - modifiée des deux côtés depuis la dernière synchronisation → la plus récente gagne ;
 * - fichier supprimé sur Drive → recette supprimée dans l'app (sauf si elle a été modifiée entre-temps) ;
 * - recette supprimée dans l'app → fichier mis à la corbeille (sauf s'il a été modifié entre-temps) ;
 * - nouveaux fichiers et nouvelles recettes sans lien : rapprochés par nom, sinon créés de l'autre côté ;
 * - dossier vide : rien n'est supprimé dans l'app, toutes les recettes y sont envoyées.
 */
object DriveSyncPlanner {

    fun plan(local: List<Recipe>, allLinks: List<DriveLink>, files: List<RemoteRecipeFile>): List<SyncAction> {
        // Garde-fou : un dossier entièrement vide ressemble plus à un dossier remplacé qu'à des suppressions voulues.
        // On ne supprime alors rien dans l'app ; les recettes sont simplement renvoyées dans le dossier.
        val links = if (files.isEmpty()) emptyList() else allLinks
        val actions = mutableListOf<SyncAction>()
        val localById = local.associateBy { it.id }
        val filesById = files.associateBy { it.id }
        val linkedRecipes = mutableSetOf<Long>()
        val linkedFiles = mutableSetOf<String>()

        links.forEach { link ->
            val recipe = localById[link.recipeId]
            val file = filesById[link.fileId]
            linkedRecipes += link.recipeId
            linkedFiles += link.fileId
            val localChanged = recipe != null && recipe.updatedAt > link.localUpdatedAt
            val remoteChanged = file != null && file.modifiedAt != link.remoteModifiedAt
            when {
                recipe == null && file == null -> actions += SyncAction.DropLink(link.recipeId)
                recipe == null && file != null ->
                    actions += if (remoteChanged) {
                        SyncAction.Download(file, null, staleLink = link.recipeId)
                    } else {
                        SyncAction.TrashRemote(file.id, link.recipeId)
                    }
                recipe != null && file == null ->
                    actions += if (localChanged) SyncAction.Upload(recipe, null) else SyncAction.DeleteLocal(recipe.id)
                recipe != null && file != null -> when {
                    localChanged && remoteChanged ->
                        actions += if (recipe.updatedAt > file.modifiedAt) {
                            SyncAction.Upload(recipe, file.id)
                        } else {
                            SyncAction.Download(file, recipe.id)
                        }
                    localChanged -> actions += SyncAction.Upload(recipe, file.id)
                    remoteChanged -> actions += SyncAction.Download(file, recipe.id)
                }
            }
        }

        val unlinkedLocal = local.filter { it.id !in linkedRecipes }
            .groupBy { Ingredient.normalizeName(it.name) }
            .mapValues { it.value.toMutableList() }
        files.filter { it.id !in linkedFiles }.sortedBy { it.name }.forEach { file ->
            val match = unlinkedLocal[Ingredient.normalizeName(file.name)]?.removeFirstOrNull()
            actions += when {
                match == null -> SyncAction.Download(file, null)
                match.updatedAt > file.modifiedAt -> SyncAction.Upload(match, file.id)
                else -> SyncAction.Download(file, match.id)
            }
        }
        unlinkedLocal.values.flatten().sortedBy { it.name }.forEach { actions += SyncAction.Upload(it, null) }
        return actions
    }
}
