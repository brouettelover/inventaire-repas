package fr.inventairerepas.domain.stock

import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.portion.PortionScaler
import fr.inventairerepas.domain.shopping.IngredientNeed
import fr.inventairerepas.domain.unit.AggregatedQuantity
import fr.inventairerepas.domain.unit.Quantity
import kotlin.math.max

/** Règles de mise à jour du stock, exprimé en unité de base par ingrédient et famille. */
object StockCalculator {

    fun add(currentBase: Double, quantity: Quantity): Double = currentBase + quantity.baseAmount

    /** Le stock ne descend jamais sous zéro. */
    fun remove(currentBase: Double, quantity: Quantity): Double = max(0.0, currentBase - quantity.baseAmount)

    /**
     * Ce que la préparation d'une recette consomme pour [servings] personnes, agrégé par
     * ingrédient et famille. Les ingrédients « toujours en stock » ne sont pas suivis.
     */
    fun consumption(recipe: Recipe, servings: Int): List<IngredientNeed> =
        PortionScaler.scale(recipe, servings)
            .filterNot { it.ingredient.alwaysInStock }
            .map { IngredientNeed(it.ingredient, AggregatedQuantity.of(it.quantity)) }
            .groupBy { it.key }
            .map { (_, needs) -> needs.reduce { a, b -> a.copy(quantity = a.quantity + b.quantity) } }
}
