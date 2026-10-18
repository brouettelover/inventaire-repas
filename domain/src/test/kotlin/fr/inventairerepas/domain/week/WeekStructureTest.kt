package fr.inventairerepas.domain.week

import fr.inventairerepas.domain.model.Freshness
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeekStructureTest {

    private val monday = LocalDate.of(2026, 10, 19)

    @Test
    fun `debut de semaine sur le premier jour de courses`() {
        assertEquals(monday, WeekStructure.weekStartFor(monday, DayOfWeek.MONDAY))
        assertEquals(monday, WeekStructure.weekStartFor(monday.plusDays(6), DayOfWeek.MONDAY))
        assertEquals(
            LocalDate.of(2026, 10, 14),
            WeekStructure.weekStartFor(monday, DayOfWeek.WEDNESDAY),
        )
    }

    @Test
    fun `deux blocs de trois jours et le septieme jour en J+3 du second bloc`() {
        val slots = WeekStructure.slots(monday)
        assertEquals(7, slots.size)
        assertEquals(listOf(0, 0, 0, 1, 1, 1, 1), slots.map { it.blockIndex })
        assertEquals(listOf(0, 1, 2, 0, 1, 2, 3), slots.map { it.offset })
        assertEquals(
            listOf(Freshness.D0, Freshness.D1, Freshness.D2, Freshness.D0, Freshness.D1, Freshness.D2, Freshness.D2),
            slots.map { it.freshness },
        )
        assertEquals("J+3", slots.last().offsetLabel)
    }

    @Test
    fun `jours de courses espaces de trois jours`() {
        val slots = WeekStructure.slots(monday)
        assertEquals(listOf(monday, monday.plusDays(3)), slots.filter { it.isShoppingDay }.map { it.date })
        assertTrue(slots.take(3).all { it.shoppingDate == monday })
        assertTrue(slots.drop(3).all { it.shoppingDate == LocalDate.of(2026, 10, 22) })
    }

    @Test
    fun `une date hors de la semaine est refusee`() {
        assertThrows<IllegalArgumentException> { WeekStructure.slotFor(monday, monday.plusDays(7)) }
    }

    @Test
    fun `semaines passees en lecture seule`() {
        val today = LocalDate.of(2026, 10, 21)
        assertTrue(WeekStructure.isEditable(monday, today, DayOfWeek.MONDAY))
        assertTrue(WeekStructure.isEditable(monday.plusWeeks(1), today, DayOfWeek.MONDAY))
        assertFalse(WeekStructure.isEditable(monday.minusWeeks(1), today, DayOfWeek.MONDAY))
    }
}
