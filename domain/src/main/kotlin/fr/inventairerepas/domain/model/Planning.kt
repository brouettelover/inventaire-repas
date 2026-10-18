package fr.inventairerepas.domain.model

import fr.inventairerepas.domain.unit.Quantity
import java.time.DayOfWeek
import java.time.LocalDate

enum class DayMode { MANUAL, RANDOM }

/** Contraintes du tirage aléatoire qui peuvent être relâchées, dans cet ordre. */
enum class RelaxedConstraint(val label: String) {
    PREVIOUS_WEEK("anti-répétition de la semaine précédente"),
    DURATION("durée maximale"),
    SEASON("saison"),
}

data class DayPlan(
    val id: Long = 0,
    val date: LocalDate,
    val mode: DayMode,
    val recipe: Recipe?,
    val servings: Int,
    val noMeal: Boolean = false,
    val cooked: Boolean = false,
    /** Contraintes relâchées lors du dernier tirage, pour les signaler à l'utilisateur. */
    val relaxed: Set<RelaxedConstraint> = emptySet(),
)

data class WeekPlan(
    /** Premier jour de la semaine, qui est aussi le premier jour de courses. */
    val startDate: LocalDate,
    val days: List<DayPlan>,
)

data class ShoppingItem(
    val id: Long = 0,
    val ingredient: Ingredient,
    val quantity: Quantity,
    val checked: Boolean = false,
)

data class ShoppingList(
    val id: Long = 0,
    val weekStart: LocalDate,
    /** 0 pour le premier jour de courses, 1 pour le second. */
    val blockIndex: Int,
    val shoppingDate: LocalDate,
    val items: List<ShoppingItem>,
)

data class Settings(
    val firstShoppingDay: DayOfWeek = DayOfWeek.MONDAY,
    val defaultServings: Int = 2,
    /** Durée maximale en minutes par jour de la semaine ; null = illimitée. */
    val maxMinutesByDay: Map<DayOfWeek, Int?> = DayOfWeek.entries.associateWith { day ->
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) null else 30
    },
    val googleAccountEmail: String? = null,
) {
    fun maxMinutesFor(day: DayOfWeek): Int? = maxMinutesByDay[day]
}
