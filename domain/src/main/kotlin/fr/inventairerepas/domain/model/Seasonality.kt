package fr.inventairerepas.domain.model

import java.time.LocalDate
import java.time.Month

/** Mois pendant lesquels une recette est de saison. */
data class Seasonality(val months: Set<Month>) {

    val isAllYear: Boolean get() = months.size == Month.entries.size

    fun isInSeason(date: LocalDate): Boolean = date.month in months

    /** Masque sur 12 bits : bit 0 = janvier, bit 11 = décembre. */
    fun toMask(): Int = months.fold(0) { mask, month -> mask or (1 shl month.ordinal) }

    companion object {
        const val ALL_YEAR_MASK = 0xFFF

        val ALL_YEAR = Seasonality(Month.entries.toSet())

        fun of(vararg months: Month) = Seasonality(months.toSet())

        /** Période circulaire, par ex. octobre → février. */
        fun range(from: Month, to: Month): Seasonality {
            val months = mutableSetOf<Month>()
            var current = from
            while (true) {
                months += current
                if (current == to) break
                current = current.plus(1)
            }
            return Seasonality(months)
        }

        /** Un masque vide est traité comme « toute l'année » pour ne jamais rendre une recette introuvable. */
        fun fromMask(mask: Int): Seasonality {
            val months = Month.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet()
            return if (months.isEmpty()) ALL_YEAR else Seasonality(months)
        }
    }
}
