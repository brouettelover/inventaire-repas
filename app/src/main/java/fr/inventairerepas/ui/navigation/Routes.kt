package fr.inventairerepas.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object WeekRoute

@Serializable
object RecipesRoute

@Serializable
object ShoppingRoute

@Serializable
object StockRoute

@Serializable
object SettingsRoute

/** [dateEpochDay] : jour du menu d'où l'on vient, pour les portions et « cuisinée » ; [NO_DATE] sinon. */
@Serializable
data class RecipeDetailRoute(val recipeId: Long, val dateEpochDay: Long = NO_DATE)

/** [recipeId] : [NEW_RECIPE] pour une création. */
@Serializable
data class RecipeEditRoute(val recipeId: Long = NEW_RECIPE)

/** Choix manuel de la recette d'un jour du menu. */
@Serializable
data class PickRecipeRoute(val weekStartEpochDay: Long, val dateEpochDay: Long)

const val NO_DATE = Long.MIN_VALUE
const val NEW_RECIPE = 0L
