package fr.inventairerepas.domain.model

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SeasonalityTest {

    @Test
    fun `aller-retour par le masque`() {
        val seasonality = Seasonality.of(Month.JANUARY, Month.JUNE, Month.DECEMBER)
        assertEquals(seasonality, Seasonality.fromMask(seasonality.toMask()))
        assertEquals(Seasonality.ALL_YEAR_MASK, Seasonality.ALL_YEAR.toMask())
    }

    @Test
    fun `un masque vide vaut toute l annee`() {
        assertTrue(Seasonality.fromMask(0).isAllYear)
    }

    @Test
    fun `periode qui passe par la fin d annee`() {
        val winter = Seasonality.range(Month.OCTOBER, Month.FEBRUARY)
        assertEquals(
            setOf(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY),
            winter.months,
        )
        assertTrue(winter.isInSeason(LocalDate.of(2026, 1, 15)))
        assertFalse(winter.isInSeason(LocalDate.of(2026, 7, 15)))
    }
}
