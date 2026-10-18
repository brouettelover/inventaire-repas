package fr.inventairerepas.domain.shopping

import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.portion.PortionScaler
import fr.inventairerepas.domain.unit.AggregatedQuantity
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitFamily

/** Une ligne de liste de courses : ce qu'il reste à acheter pour un ingrédient et une famille d'unités. */
data class ShoppingLine(val ingredient: Ingredient, val quantity: Quantity)

/** Clé d'agrégation : un ingrédient du catalogue et une famille d'unités. */
data class IngredientKey(val normalizedName: String, val family: UnitFamily)

/** Besoin agrégé d'un ingrédient pour une famille d'unités. */
data class IngredientNeed(val ingredient: Ingredient, val quantity: AggregatedQuantity) {
    val key get() = IngredientKey(ingredient.normalizedName, quantity.family)
}

object ShoppingListCalculator {

    private const val EPSILON = 1e-6

    /**
     * Besoins d'un ensemble de jours, après ajustement des portions et agrégation des unités.
     * Sont ignorés : jours sans recette, « sans repas » ou déjà cuisinés (leur stock est déjà déduit),
     * et ingrédients « toujours en stock ».
     */
    fun needs(days: List<DayPlan>): List<IngredientNeed> {
        val needs = linkedMapOf<IngredientKey, IngredientNeed>()
        days.filter { !it.noMeal && !it.cooked }.forEach { day ->
            val recipe = day.recipe ?: return@forEach
            PortionScaler.scale(recipe, day.servings)
                .filterNot { it.ingredient.alwaysInStock }
                .forEach { line ->
                    val need = IngredientNeed(line.ingredient, AggregatedQuantity.of(line.quantity))
                    needs.merge(need.key, need) { a, b -> a.copy(quantity = a.quantity + b.quantity) }
                }
        }
        return needs.values.toList()
    }

    /**
     * Listes de courses de plusieurs blocs, dans l'ordre : chaque liste affiche besoin − stock,
     * et le stock est imputé au premier bloc avant d'être disponible pour le suivant.
     * Les ingrédients entièrement couverts n'apparaissent pas.
     */
    fun compute(blocks: List<List<DayPlan>>, stock: List<StockLine>): List<List<ShoppingLine>> {
        val available = stock
            .groupBy { IngredientKey(it.ingredient.normalizedName, it.family) }
            .mapValues { (_, lines) -> lines.sumOf { it.baseAmount } }
            .toMutableMap()

        return blocks.map { days ->
            needs(days).mapNotNull { need ->
                val inStock = available[need.key] ?: 0.0
                val used = minOf(inStock, need.quantity.baseAmount)
                available[need.key] = inStock - used
                val missing = need.quantity.baseAmount - used
                if (missing <= EPSILON) {
                    null
                } else {
                    ShoppingLine(need.ingredient, need.quantity.withBaseAmount(missing).toDisplayQuantity())
                }
            }.sortedBy { it.ingredient.normalizedName }
        }
    }
}
