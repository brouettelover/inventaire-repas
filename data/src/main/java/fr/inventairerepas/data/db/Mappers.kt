package fr.inventairerepas.data.db

import fr.inventairerepas.data.db.entity.IngredientEntity
import fr.inventairerepas.data.db.entity.RecipeEntity
import fr.inventairerepas.data.db.entity.RecipeIngredientEntity
import fr.inventairerepas.data.db.entity.RecipeStepEntity
import fr.inventairerepas.data.db.relation.DayPlanWithRecipe
import fr.inventairerepas.data.db.relation.RecipeWithDetails
import fr.inventairerepas.data.db.relation.ShoppingListWithItems
import fr.inventairerepas.data.db.relation.StockLineWithIngredient
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.unit.Quantity

fun IngredientEntity.toDomain() = Ingredient(id = id, name = name, alwaysInStock = alwaysInStock)

fun Ingredient.toEntity() = IngredientEntity(
    id = id,
    name = name.trim(),
    normalizedName = normalizedName,
    alwaysInStock = alwaysInStock,
)

fun RecipeWithDetails.toDomain() = Recipe(
    id = recipe.id,
    name = recipe.name,
    baseServings = recipe.baseServings,
    prepMinutes = recipe.prepMinutes,
    cookMinutes = recipe.cookMinutes,
    seasonality = Seasonality.fromMask(recipe.seasonMask),
    freshness = recipe.freshness,
    ingredients = ingredients
        .sortedBy { it.line.position }
        .map { RecipeIngredient(it.ingredient.toDomain(), Quantity(it.line.amount, it.line.unit)) },
    steps = steps.sortedBy { it.position }.map { it.text },
    updatedAt = recipe.updatedAt,
)

fun Recipe.toEntity() = RecipeEntity(
    id = id,
    name = name.trim(),
    baseServings = baseServings,
    prepMinutes = prepMinutes,
    cookMinutes = cookMinutes,
    freshness = freshness,
    seasonMask = seasonality.toMask(),
    updatedAt = updatedAt,
)

/** Lignes d'ingrédients, à partir des identifiants du catalogue déjà résolus dans le même ordre. */
fun Recipe.ingredientEntities(ingredientIds: List<Long>): List<RecipeIngredientEntity> =
    ingredients.zip(ingredientIds).mapIndexed { index, (line, ingredientId) ->
        RecipeIngredientEntity(
            recipeId = id,
            ingredientId = ingredientId,
            amount = line.quantity.amount,
            unit = line.quantity.unit,
            position = index,
        )
    }

fun Recipe.stepEntities(): List<RecipeStepEntity> =
    steps.mapIndexed { index, text -> RecipeStepEntity(recipeId = id, position = index, text = text) }

fun StockLineWithIngredient.toDomain() = StockLine(
    ingredient = ingredient.toDomain(),
    family = line.family,
    baseAmount = line.amountBase,
)

fun DayPlanWithRecipe.toDomain() = DayPlan(
    id = day.id,
    date = day.date,
    mode = day.mode,
    recipe = recipe?.toDomain(),
    servings = day.servings,
    noMeal = day.noMeal,
    cooked = day.cooked,
    relaxed = day.relaxed,
)

fun ShoppingListWithItems.toDomain() = ShoppingList(
    id = list.id,
    weekStart = list.weekStart,
    blockIndex = list.blockIndex,
    shoppingDate = list.shoppingDate,
    items = items
        .sortedBy { it.ingredient.name.lowercase() }
        .map {
            ShoppingItem(
                id = it.item.id,
                ingredient = it.ingredient.toDomain(),
                quantity = Quantity(it.item.amount, it.item.unit),
                checked = it.item.checked,
            )
        },
)
