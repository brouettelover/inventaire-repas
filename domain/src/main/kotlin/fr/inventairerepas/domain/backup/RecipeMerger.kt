package fr.inventairerepas.domain.backup

import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe

/** Résultat d'une fusion : recettes à enregistrer localement, et compteurs pour l'utilisateur. */
data class MergeResult(
    val toSave: List<Recipe>,
    val added: Int,
    val updated: Int,
    val unchanged: Int,
)

/**
 * Fusionne des recettes importées dans les recettes locales, rapprochées par nom normalisé :
 * - recette inconnue : ajoutée ;
 * - recette connue, modifiée plus récemment dans le fichier : remplace la version locale ;
 * - sinon : la version locale est gardée.
 * Les suppressions ne sont pas propagées : une recette absente du fichier reste en place.
 */
object RecipeMerger {

    fun merge(local: List<Recipe>, incoming: List<Recipe>): MergeResult {
        val localByName = local.associateBy { Ingredient.normalizeName(it.name) }
        // Si le fichier contient deux fois le même nom, la version la plus récente l'emporte.
        val incomingByName = incoming
            .groupBy { Ingredient.normalizeName(it.name) }
            .mapValues { (_, versions) -> versions.maxBy { it.updatedAt } }

        val toSave = mutableListOf<Recipe>()
        var added = 0
        var updated = 0
        var unchanged = 0
        incomingByName.forEach { (name, recipe) ->
            val existing = localByName[name]
            when {
                existing == null -> {
                    toSave += recipe.copy(id = 0)
                    added++
                }
                recipe.updatedAt > existing.updatedAt && !sameContent(existing, recipe) -> {
                    toSave += recipe.copy(id = existing.id)
                    updated++
                }
                else -> unchanged++
            }
        }
        return MergeResult(toSave, added, updated, unchanged)
    }

    private fun sameContent(a: Recipe, b: Recipe): Boolean =
        a.copy(id = 0, updatedAt = 0, ingredients = a.ingredients.map { it.copy(ingredient = it.ingredient.copy(id = 0)) }) ==
            b.copy(id = 0, updatedAt = 0, ingredients = b.ingredients.map { it.copy(ingredient = it.ingredient.copy(id = 0)) })
}
