package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.menu.WeekPlanner
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import fr.inventairerepas.domain.week.WeekStructure
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import javax.inject.Inject

/** Recettes planifiées la semaine précédant [weekStart], pour l'anti-répétition. */
internal suspend fun PlanRepository.previousWeekRecipeIds(weekStart: LocalDate): Set<Long> =
    recipeIdsBetween(weekStart.minusDays(7), weekStart.minusDays(1))

/**
 * Crée la semaine si elle n'existe pas encore, et tire aussitôt ses jours aléatoires.
 * Une semaine partielle (après un changement du premier jour de courses) est complétée.
 */
class EnsureWeekUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val recipes: RecipeRepository,
    private val settings: SettingsRepository,
    private val planner: WeekPlanner,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(weekStart: LocalDate): List<DayPlan> = transaction.run {
        val existing = plans.getWeek(weekStart)
        if (existing.size == WeekStructure.DAYS_PER_WEEK) return@run existing
        val days = planner.completeWeek(
            weekStart = weekStart,
            existing = existing,
            recipes = recipes.getAll(),
            settings = settings.settings.first(),
            previousWeekRecipeIds = plans.previousWeekRecipeIds(weekStart),
        )
        plans.saveDays(weekStart, days)
        days
    }
}

/** Relance le tirage de toute la semaine (jours en mode aléatoire non cuisinés). */
class RedrawWeekUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val recipes: RecipeRepository,
    private val settings: SettingsRepository,
    private val planner: WeekPlanner,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(weekStart: LocalDate): List<DayPlan> = transaction.run {
        val days = planner.redrawWeek(
            weekStart = weekStart,
            days = plans.getWeek(weekStart),
            recipes = recipes.getAll(),
            settings = settings.settings.first(),
            previousWeekRecipeIds = plans.previousWeekRecipeIds(weekStart),
        )
        plans.saveDays(weekStart, days)
        days
    }
}

/** Relance le tirage d'un seul jour. */
class RedrawDayUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val recipes: RecipeRepository,
    private val settings: SettingsRepository,
    private val planner: WeekPlanner,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(weekStart: LocalDate, date: LocalDate): DayPlan = transaction.run {
        val days = planner.redrawDay(
            weekStart = weekStart,
            days = plans.getWeek(weekStart),
            date = date,
            recipes = recipes.getAll(),
            settings = settings.settings.first(),
            previousWeekRecipeIds = plans.previousWeekRecipeIds(weekStart),
        )
        plans.saveDays(weekStart, days)
        days.first { it.date == date }
    }
}

/**
 * Modifications simples d'un jour : mode, recette choisie à la main, portions, « sans repas ».
 * Un jour déjà cuisiné est verrouillé tant que « cuisinée » n'est pas annulé.
 */
class UpdateDayUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val transaction: TransactionRunner,
) {
    suspend fun setMode(weekStart: LocalDate, date: LocalDate, mode: DayMode) =
        update(weekStart, date) { it.copy(mode = mode) }

    /** Choix manuel : le jour passe en mode manuel. */
    suspend fun chooseRecipe(weekStart: LocalDate, date: LocalDate, recipe: Recipe) =
        update(weekStart, date) {
            it.copy(mode = DayMode.MANUAL, recipe = recipe, noMeal = false, relaxed = emptySet())
        }

    suspend fun setServings(weekStart: LocalDate, date: LocalDate, servings: Int) {
        require(servings > 0) { "Le nombre de personnes doit être positif" }
        update(weekStart, date) { it.copy(servings = servings) }
    }

    suspend fun setNoMeal(weekStart: LocalDate, date: LocalDate, noMeal: Boolean) =
        update(weekStart, date) { it.copy(noMeal = noMeal) }

    private suspend fun update(weekStart: LocalDate, date: LocalDate, change: (DayPlan) -> DayPlan) {
        transaction.run {
            val days = plans.getWeek(weekStart)
            val day = days.firstOrNull { it.date == date } ?: error("Jour introuvable : $date")
            check(!day.cooked) { "Ce jour est marqué « cuisinée » : annule-le avant de le modifier" }
            plans.saveDays(weekStart, days.map { if (it.date == date) change(it) else it })
        }
    }
}
