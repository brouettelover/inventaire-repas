package fr.inventairerepas.domain.model

import fr.inventairerepas.domain.unit.Quantity

data class RecipeIngredient(
    val ingredient: Ingredient,
    /** Quantité pour les [Recipe.baseServings] portions de base. */
    val quantity: Quantity,
)

data class Recipe(
    val id: Long = 0,
    val name: String,
    val baseServings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val seasonality: Seasonality,
    val freshness: Freshness,
    val ingredients: List<RecipeIngredient>,
    val steps: List<String>,
) {
    init {
        require(baseServings > 0) { "Les portions de base doivent être positives" }
        require(prepMinutes >= 0 && cookMinutes >= 0) { "Les durées ne peuvent pas être négatives" }
    }

    val totalMinutes: Int get() = prepMinutes + cookMinutes
}
