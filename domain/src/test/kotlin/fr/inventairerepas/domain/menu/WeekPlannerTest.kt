package fr.inventairerepas.domain.menu

import fr.inventairerepas.domain.fixtures.day
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Settings
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekPlannerTest {

    private val monday = LocalDate.of(2026, 10, 19)
    private val settings = Settings(
        firstShoppingDay = DayOfWeek.MONDAY,
        defaultServings = 3,
        maxMinutesByDay = DayOfWeek.entries.associateWith { null },
    )
    private val recipes = listOf(
        recipe(1, Freshness.D0), recipe(2, Freshness.D0), recipe(3, Freshness.D0),
        recipe(4, Freshness.D1), recipe(5, Freshness.D1), recipe(6, Freshness.D1),
        recipe(7, Freshness.D2), recipe(8, Freshness.D2), recipe(9, Freshness.D2), recipe(10, Freshness.D2),
    )

    private fun planner(seed: Int = 0) = WeekPlanner(MenuGenerator(Random(seed)))

    @Test
    fun `nouvelle semaine en mode aleatoire aux portions par defaut`() {
        val week = planner().newWeek(monday, settings)
        assertEquals((0L..6L).map { monday.plusDays(it) }, week.map { it.date })
        assertTrue(week.all { it.mode == DayMode.RANDOM && it.servings == 3 && it.recipe == null })
    }

    @Test
    fun `chaque jour tire dans la liste de sa position, sans doublon`() {
        repeat(50) { seed ->
            val week = planner(seed).redrawWeek(monday, planner().newWeek(monday, settings), recipes, settings, emptySet())
            val freshness = week.map { it.recipe!!.freshness }
            assertEquals(
                listOf(Freshness.D0, Freshness.D1, Freshness.D2, Freshness.D0, Freshness.D1, Freshness.D2, Freshness.D2),
                freshness,
            )
            assertEquals(7, week.map { it.recipe!!.id }.toSet().size)
        }
    }

    @Test
    fun `la duree maximale du jour de la semaine est appliquee`() {
        val limited = settings.copy(maxMinutesByDay = DayOfWeek.entries.associateWith { 30 })
        val withLong = recipes + recipe(11, Freshness.D0, minutes = 90)
        repeat(30) { seed ->
            val week = planner(seed).redrawWeek(monday, planner().newWeek(monday, limited), withLong, limited, emptySet())
            assertTrue(week.none { it.recipe?.id == 11L })
        }
    }

    @Test
    fun `les jours manuels, cuisines ou sans repas sont conserves et comptent pour l'unicite`() {
        val days = planner().newWeek(monday, settings).mapIndexed { index, day ->
            when (index) {
                0 -> day.copy(mode = DayMode.MANUAL, recipe = recipes[0])
                3 -> day.copy(recipe = recipes[1], cooked = true)
                5 -> day.copy(noMeal = true)
                else -> day
            }
        }
        repeat(30) { seed ->
            val week = planner(seed).redrawWeek(monday, days, recipes, settings, emptySet())
            assertEquals(days[0], week[0])
            assertEquals(days[3], week[3])
            assertEquals(days[5], week[5])
            val served = week.filterNot { it.noMeal }.mapNotNull { it.recipe?.id }
            assertEquals(served.size, served.toSet().size)
        }
    }

    @Test
    fun `liste epuisee laisse le jour sans recette`() {
        val onlyOneD0 = recipes.filter { it.freshness != Freshness.D0 } + recipe(1, Freshness.D0)
        val week = planner().redrawWeek(monday, planner().newWeek(monday, settings), onlyOneD0, settings, emptySet())
        assertEquals(1, listOf(week[0], week[3]).count { it.recipe != null })
    }

    @Test
    fun `retirer un seul jour ne touche pas aux autres et evite leurs recettes`() {
        val week = planner().redrawWeek(monday, planner().newWeek(monday, settings), recipes, settings, emptySet())
        repeat(30) { seed ->
            val redrawn = planner(seed).redrawDay(monday, week, monday, recipes, settings, emptySet())
            assertEquals(week.drop(1), redrawn.drop(1))
            val others = week.drop(1).mapNotNull { it.recipe?.id }
            assertTrue(redrawn[0].recipe!!.id !in others)
            assertEquals(Freshness.D0, redrawn[0].recipe!!.freshness)
        }
    }

    @Test
    fun `retirer un jour manuel le passe en aleatoire`() {
        val days = planner().newWeek(monday, settings).mapIndexed { i, d ->
            if (i == 2) d.copy(mode = DayMode.MANUAL, recipe = recipes[6], noMeal = true) else d
        }
        val redrawn = planner().redrawDay(monday, days, monday.plusDays(2), recipes, settings, emptySet())[2]
        assertEquals(DayMode.RANDOM, redrawn.mode)
        assertEquals(false, redrawn.noMeal)
    }

    @Test
    fun `un jour cuisine ne peut pas etre retire`() {
        val days = listOf(day(monday, recipes[0], cooked = true))
        assertThrows<IllegalArgumentException> {
            planner().redrawDay(monday, days, monday, recipes, settings, emptySet())
        }
    }

    @Test
    fun `aucune recette pour le septieme jour si la liste J+2 est epuisee`() {
        val fewD2 = recipes.filter { it.freshness != Freshness.D2 } + recipe(7, Freshness.D2) + recipe(8, Freshness.D2)
        val week = planner().redrawWeek(monday, planner().newWeek(monday, settings), fewD2, settings, emptySet())
        assertNull(week[6].recipe)
    }

    @Test
    fun `completer une semaine partielle ne tire que les jours manquants`() {
        val existing = listOf(day(monday, recipes[0], mode = DayMode.MANUAL), day(monday.plusDays(1), recipes[3]))
        repeat(20) { seed ->
            val week = planner(seed).completeWeek(monday, existing, recipes, settings, emptySet())
            assertEquals(7, week.size)
            assertEquals(existing, week.take(2))
            assertTrue(week.drop(2).all { it.recipe != null })
            assertEquals(7, week.map { it.recipe!!.id }.toSet().size)
        }
    }
}
