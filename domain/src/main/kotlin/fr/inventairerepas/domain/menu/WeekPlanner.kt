package fr.inventairerepas.domain.menu

import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.week.WeekStructure
import java.time.LocalDate
import javax.inject.Inject

/** Construit et retire les menus d'une semaine à partir du [MenuGenerator]. */
class WeekPlanner @Inject constructor(private val generator: MenuGenerator) {

    /** Semaine neuve : 7 jours en mode aléatoire, aux portions par défaut, sans recette. */
    fun newWeek(weekStart: LocalDate, settings: Settings): List<DayPlan> =
        WeekStructure.slots(weekStart).map {
            DayPlan(date = it.date, mode = DayMode.RANDOM, recipe = null, servings = settings.defaultServings)
        }

    /**
     * Complète une semaine : les jours absents sont créés et tirés, les jours existants sont
     * conservés tels quels. Une semaine vide est ainsi entièrement tirée.
     */
    fun completeWeek(
        weekStart: LocalDate,
        existing: List<DayPlan>,
        recipes: List<Recipe>,
        settings: Settings,
        previousWeekRecipeIds: Set<Long>,
    ): List<DayPlan> {
        val byDate = existing.associateBy { it.date }
        val used = existing.mapNotNull { it.recipeIdIfServed() }.toMutableSet()
        return newWeek(weekStart, settings).map { fresh ->
            byDate[fresh.date] ?: drawFor(weekStart, fresh, recipes, settings, previousWeekRecipeIds, used)
                .also { drawn -> drawn.recipe?.let { used += it.id } }
        }
    }

    /**
     * Retire tous les jours en mode aléatoire. Les jours en mode manuel, déjà cuisinés ou
     * « sans repas » sont conservés et leurs recettes comptent pour l'unicité dans la semaine.
     */
    fun redrawWeek(
        weekStart: LocalDate,
        days: List<DayPlan>,
        recipes: List<Recipe>,
        settings: Settings,
        previousWeekRecipeIds: Set<Long>,
    ): List<DayPlan> {
        val used = days.filterNot(::isRedrawable).mapNotNull { it.recipeIdIfServed() }.toMutableSet()
        return days.sortedBy { it.date }.map { day ->
            if (!isRedrawable(day)) {
                day
            } else {
                drawFor(weekStart, day, recipes, settings, previousWeekRecipeIds, used)
                    .also { drawn -> drawn.recipe?.let { used += it.id } }
            }
        }
    }

    /** Retire un seul jour (il passe en mode aléatoire), en évitant les recettes des autres jours. */
    fun redrawDay(
        weekStart: LocalDate,
        days: List<DayPlan>,
        date: LocalDate,
        recipes: List<Recipe>,
        settings: Settings,
        previousWeekRecipeIds: Set<Long>,
    ): List<DayPlan> {
        val target = days.first { it.date == date }
        require(!target.cooked) { "Un jour déjà cuisiné ne peut pas être retiré" }
        val used = days.filter { it.date != date }.mapNotNull { it.recipeIdIfServed() }.toMutableSet()
        val redrawn = drawFor(
            weekStart,
            target.copy(mode = DayMode.RANDOM, noMeal = false),
            recipes,
            settings,
            previousWeekRecipeIds,
            used,
        )
        return days.map { if (it.date == date) redrawn else it }
    }

    private fun isRedrawable(day: DayPlan) = day.mode == DayMode.RANDOM && !day.cooked && !day.noMeal

    private fun DayPlan.recipeIdIfServed(): Long? = if (noMeal) null else recipe?.id

    private fun drawFor(
        weekStart: LocalDate,
        day: DayPlan,
        recipes: List<Recipe>,
        settings: Settings,
        previousWeekRecipeIds: Set<Long>,
        used: Set<Long>,
    ): DayPlan {
        val slot = WeekStructure.slotFor(weekStart, day.date)
        val request = DrawRequest(
            date = day.date,
            freshness = slot.freshness,
            maxMinutes = settings.maxMinutesFor(day.date.dayOfWeek),
            previousWeekRecipeIds = previousWeekRecipeIds,
            usedThisWeekRecipeIds = used,
        )
        return when (val result = generator.draw(recipes, request)) {
            is DrawResult.Drawn -> day.copy(recipe = result.recipe, relaxed = result.relaxed)
            is DrawResult.NoRecipe -> day.copy(recipe = null, relaxed = emptySet())
        }
    }
}
