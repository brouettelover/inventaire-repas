package fr.inventairerepas.domain.portion

import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient

/** Recalcule les quantités d'une recette proportionnellement au nombre de personnes. */
object PortionScaler {

    fun factor(recipe: Recipe, servings: Int): Double {
        require(servings > 0) { "Le nombre de personnes doit être positif : $servings" }
        return servings.toDouble() / recipe.baseServings
    }

    fun scale(recipe: Recipe, servings: Int): List<RecipeIngredient> {
        val factor = factor(recipe, servings)
        return recipe.ingredients.map { it.copy(quantity = it.quantity * factor) }
    }
}
