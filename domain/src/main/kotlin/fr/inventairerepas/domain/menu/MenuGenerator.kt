package fr.inventairerepas.domain.menu

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RelaxedConstraint
import java.time.LocalDate
import kotlin.random.Random

/** Paramètres d'un tirage pour un jour donné. */
data class DrawRequest(
    val date: LocalDate,
    val freshness: Freshness,
    /** Durée totale maximale en minutes ; null = illimitée. */
    val maxMinutes: Int?,
    /** Recettes servies la semaine précédente, à éviter de préférence. */
    val previousWeekRecipeIds: Set<Long> = emptySet(),
    /** Recettes déjà présentes dans la semaine : jamais deux fois la même. */
    val usedThisWeekRecipeIds: Set<Long> = emptySet(),
)

sealed interface DrawResult {
    /** [relaxed] liste les contraintes qu'il a fallu abandonner pour trouver une recette. */
    data class Drawn(val recipe: Recipe, val relaxed: Set<RelaxedConstraint>) : DrawResult

    /** Aucune recette disponible dans la liste de fraîcheur, même en relâchant tout. */
    data class NoRecipe(val freshness: Freshness) : DrawResult
}

/**
 * Tirage aléatoire d'une recette dans la liste de fraîcheur du jour.
 *
 * Contraintes : de saison à la date, durée ≤ maximum du jour, absente de la semaine précédente,
 * absente de la semaine en cours. Si rien ne convient, les trois premières sont relâchées dans
 * l'ordre de [RelaxedConstraint] (anti-répétition, puis durée, puis saison), de façon cumulative.
 * À un niveau de relâchement donné, les recettes qui respectent le plus de contraintes sont
 * préférées, et le résultat signale les contraintes que la recette tirée ne respecte pas.
 * L'unicité dans la semaine n'est jamais relâchée.
 */
class MenuGenerator(private val random: Random = Random.Default) {

    fun draw(recipes: List<Recipe>, request: DrawRequest): DrawResult {
        val pool = recipes.filter {
            it.freshness == request.freshness && it.id !in request.usedThisWeekRecipeIds
        }
        for (level in 0..RelaxedConstraint.entries.size) {
            val relaxed = RelaxedConstraint.entries.take(level).toSet()
            val candidates = pool.associateWith { violations(it, request) }
                .filterValues { relaxed.containsAll(it) }
            if (candidates.isNotEmpty()) {
                val fewest = candidates.values.minOf { it.size }
                val (recipe, violated) = candidates.entries.filter { it.value.size == fewest }.random(random)
                return DrawResult.Drawn(recipe, violated)
            }
        }
        return DrawResult.NoRecipe(request.freshness)
    }

    /** Contraintes relâchables que [recipe] ne respecte pas pour ce tirage. */
    fun violations(recipe: Recipe, request: DrawRequest): Set<RelaxedConstraint> = buildSet {
        if (recipe.id in request.previousWeekRecipeIds) add(RelaxedConstraint.PREVIOUS_WEEK)
        if (request.maxMinutes != null && recipe.totalMinutes > request.maxMinutes) add(RelaxedConstraint.DURATION)
        if (!recipe.seasonality.isInSeason(request.date)) add(RelaxedConstraint.SEASON)
    }
}
