package fr.inventairerepas.domain.week

import fr.inventairerepas.domain.model.Freshness
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Place d'un jour dans la semaine.
 *
 * [blockIndex] : 0 pour le premier jour de courses, 1 pour le second.
 * [offset] : position dans le bloc, de 0 à 2, et 3 pour le 7e jour rattaché au second bloc.
 */
data class DaySlot(
    val dayIndex: Int,
    val date: LocalDate,
    val blockIndex: Int,
    val offset: Int,
    val shoppingDate: LocalDate,
) {
    /** Liste de fraîcheur où tirer la recette : le 7e jour (J+3) tire dans J+2. */
    val freshness: Freshness
        get() = when (offset) {
            0 -> Freshness.D0
            1 -> Freshness.D1
            else -> Freshness.D2
        }

    val isShoppingDay: Boolean get() = offset == 0

    val offsetLabel: String get() = "J+$offset"
}

/**
 * Découpage d'une semaine : deux jours de courses espacés de 3 jours, chacun couvrant un bloc
 * J+0, J+1, J+2 ; le 7e jour prolonge le second bloc en J+3.
 */
object WeekStructure {

    const val DAYS_PER_WEEK = 7
    const val DAYS_PER_BLOCK = 3
    const val BLOCK_COUNT = 2

    /** Début de la semaine contenant [date] : le dernier [firstShoppingDay] à cette date ou avant. */
    fun weekStartFor(date: LocalDate, firstShoppingDay: DayOfWeek): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(firstShoppingDay))

    fun slots(weekStart: LocalDate): List<DaySlot> = (0 until DAYS_PER_WEEK).map { index ->
        val blockIndex = if (index < DAYS_PER_BLOCK) 0 else 1
        DaySlot(
            dayIndex = index,
            date = weekStart.plusDays(index.toLong()),
            blockIndex = blockIndex,
            offset = index - blockIndex * DAYS_PER_BLOCK,
            shoppingDate = weekStart.plusDays((blockIndex * DAYS_PER_BLOCK).toLong()),
        )
    }

    fun slotFor(weekStart: LocalDate, date: LocalDate): DaySlot =
        slots(weekStart).firstOrNull { it.date == date }
            ?: throw IllegalArgumentException("$date n'appartient pas à la semaine du $weekStart")

    /** Les semaines passées sont en lecture seule ; la semaine en cours et les suivantes sont modifiables. */
    fun isEditable(weekStart: LocalDate, today: LocalDate, firstShoppingDay: DayOfWeek): Boolean =
        !weekStart.isBefore(weekStartFor(today, firstShoppingDay))
}
