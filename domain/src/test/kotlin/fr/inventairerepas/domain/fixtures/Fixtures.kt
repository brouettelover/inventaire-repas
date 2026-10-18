package fr.inventairerepas.domain.fixtures

import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import java.time.LocalDate

object Ingredients {
    val pasta = Ingredient(id = 1, name = "Pâtes")
    val tomato = Ingredient(id = 2, name = "Tomate")
    val rice = Ingredient(id = 3, name = "Riz")
    val milk = Ingredient(id = 4, name = "Lait")
    val egg = Ingredient(id = 5, name = "Œuf")
    val salt = Ingredient(id = 6, name = "Sel", alwaysInStock = true)
    val flour = Ingredient(id = 7, name = "Farine")
}

fun ing(ingredient: Ingredient, amount: Double, unit: MeasureUnit) =
    RecipeIngredient(ingredient, Quantity(amount, unit))

fun recipe(
    id: Long,
    freshness: Freshness = Freshness.D0,
    minutes: Int = 20,
    seasonality: Seasonality = Seasonality.ALL_YEAR,
    baseServings: Int = 2,
    ingredients: List<RecipeIngredient> = emptyList(),
) = Recipe(
    id = id,
    name = "Recette $id",
    baseServings = baseServings,
    prepMinutes = minutes,
    cookMinutes = 0,
    seasonality = seasonality,
    freshness = freshness,
    ingredients = ingredients,
    steps = listOf("Cuisiner"),
)

fun day(
    date: LocalDate,
    recipe: Recipe?,
    servings: Int = 2,
    mode: DayMode = DayMode.RANDOM,
    cooked: Boolean = false,
    noMeal: Boolean = false,
) = DayPlan(date = date, mode = mode, recipe = recipe, servings = servings, cooked = cooked, noMeal = noMeal)
