package fr.inventairerepas.ui.common

import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.QuantityFormatter
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

val FRENCH: Locale = Locale.FRENCH

private val dayFormatter = DateTimeFormatter.ofPattern("EEEE d MMMM", FRENCH)
private val shortFormatter = DateTimeFormatter.ofPattern("d MMM", FRENCH)
private val fullFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", FRENCH)

fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(FRENCH) else it.toString() }

/** « Lundi 19 octobre » */
fun LocalDate.dayLabel(): String = format(dayFormatter).capitalized()

/** « 19 oct. » */
fun LocalDate.shortLabel(): String = format(shortFormatter)

/** « 19/10/2026 » */
fun LocalDate.fullLabel(): String = format(fullFormatter)

fun DayOfWeek.label(): String = getDisplayName(TextStyle.FULL, FRENCH).capitalized()

fun Month.label(): String = getDisplayName(TextStyle.FULL, FRENCH).capitalized()

fun Month.shortLabel(): String = getDisplayName(TextStyle.SHORT, FRENCH).capitalized()

fun Quantity.label(): String = QuantityFormatter.format(this)

/** « 1 h 15 », « 25 min » */
fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${"%02d".format(minutes % 60)}"
}

fun Seasonality.label(): String = when {
    isAllYear -> "Toute l'année"
    else -> Month.entries.filter { it in months }.joinToString(", ") { it.shortLabel() }
}

fun servingsLabel(servings: Int) = if (servings > 1) "$servings pers." else "1 pers."
