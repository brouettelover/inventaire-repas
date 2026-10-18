package fr.inventairerepas.domain.menu

import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RelaxedConstraint.DURATION
import fr.inventairerepas.domain.model.RelaxedConstraint.PREVIOUS_WEEK
import fr.inventairerepas.domain.model.RelaxedConstraint.SEASON
import fr.inventairerepas.domain.model.Seasonality
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Month
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MenuGeneratorTest {

    private val october = LocalDate.of(2026, 10, 19)
    private val summerOnly = Seasonality.range(Month.JUNE, Month.AUGUST)

    private fun request(
        freshness: Freshness = Freshness.D0,
        maxMinutes: Int? = 30,
        previousWeek: Set<Long> = emptySet(),
        usedThisWeek: Set<Long> = emptySet(),
    ) = DrawRequest(october, freshness, maxMinutes, previousWeek, usedThisWeek)

    /** Tire de nombreuses fois avec des graines différentes et renvoie toutes les recettes obtenues. */
    private fun drawMany(recipes: List<Recipe>, request: DrawRequest) =
        (0 until 200).map { seed ->
            assertIs<DrawResult.Drawn>(MenuGenerator(Random(seed)).draw(recipes, request))
        }

    @Test
    fun `tire uniquement dans la liste de fraicheur du jour`() {
        val recipes = listOf(recipe(1, Freshness.D0), recipe(2, Freshness.D1), recipe(3, Freshness.D2))
        assertEquals(setOf(2L), drawMany(recipes, request(Freshness.D1)).map { it.recipe.id }.toSet())
    }

    @Test
    fun `toutes les recettes eligibles peuvent sortir`() {
        val recipes = (1L..4L).map { recipe(it) }
        assertEquals(setOf(1L, 2L, 3L, 4L), drawMany(recipes, request()).map { it.recipe.id }.toSet())
    }

    @Test
    fun `seulement des recettes de saison quand il y en a`() {
        val recipes = listOf(recipe(1, seasonality = summerOnly), recipe(2))
        val results = drawMany(recipes, request())
        assertTrue(results.all { it.recipe.id == 2L && it.relaxed.isEmpty() })
    }

    @Test
    fun `duree inferieure ou egale au maximum du jour`() {
        val recipes = listOf(recipe(1, minutes = 31), recipe(2, minutes = 30))
        assertTrue(drawMany(recipes, request(maxMinutes = 30)).all { it.recipe.id == 2L })
    }

    @Test
    fun `duree illimitee quand aucun maximum n'est regle`() {
        val recipes = listOf(recipe(1, minutes = 180), recipe(2, minutes = 10))
        val ids = drawMany(recipes, request(maxMinutes = null)).map { it.recipe.id }.toSet()
        assertEquals(setOf(1L, 2L), ids)
    }

    @Test
    fun `evite de preference les recettes de la semaine precedente`() {
        val recipes = listOf(recipe(1), recipe(2))
        assertTrue(drawMany(recipes, request(previousWeek = setOf(1))).all { it.recipe.id == 2L })
    }

    @Test
    fun `jamais deux fois la meme recette dans la semaine, meme en relachant tout`() {
        val recipes = listOf(recipe(1), recipe(2, minutes = 90, seasonality = summerOnly))
        val results = drawMany(recipes, request(usedThisWeek = setOf(1)))
        assertTrue(results.all { it.recipe.id == 2L })

        val none = MenuGenerator(Random(0)).draw(listOf(recipe(1)), request(usedThisWeek = setOf(1)))
        assertEquals(DrawResult.NoRecipe(Freshness.D0), none)
    }

    @Test
    fun `liste vide donne aucun resultat`() {
        val result = MenuGenerator(Random(0)).draw(listOf(recipe(1, Freshness.D2)), request(Freshness.D0))
        assertEquals(DrawResult.NoRecipe(Freshness.D0), result)
    }

    @Test
    fun `relache d'abord l'anti-repetition`() {
        val result = MenuGenerator(Random(0)).draw(listOf(recipe(1)), request(previousWeek = setOf(1)))
        assertIs<DrawResult.Drawn>(result)
        assertEquals(setOf(PREVIOUS_WEEK), result.relaxed)
    }

    @Test
    fun `relache l'anti-repetition avant la duree`() {
        // Recette 1 : servie la semaine dernière mais courte. Recette 2 : jamais servie mais trop longue.
        val recipes = listOf(recipe(1, minutes = 20), recipe(2, minutes = 60))
        val results = drawMany(recipes, request(previousWeek = setOf(1)))
        assertTrue(results.all { it.recipe.id == 1L && it.relaxed == setOf(PREVIOUS_WEEK) })
    }

    @Test
    fun `relache la duree avant la saison`() {
        // Recette 1 : trop longue mais de saison. Recette 2 : courte mais hors saison.
        val recipes = listOf(recipe(1, minutes = 60), recipe(2, minutes = 20, seasonality = summerOnly))
        val results = drawMany(recipes, request())
        assertTrue(results.all { it.recipe.id == 1L && it.relaxed == setOf(DURATION) })
    }

    @Test
    fun `relache la duree en gardant l'anti-repetition si possible`() {
        // Toutes trop longues ; la 1 a été servie la semaine dernière, pas la 2.
        val recipes = listOf(recipe(1, minutes = 60), recipe(2, minutes = 60))
        val results = drawMany(recipes, request(previousWeek = setOf(1)))
        assertTrue(results.all { it.recipe.id == 2L && it.relaxed == setOf(DURATION) })
    }

    @Test
    fun `relache la saison en dernier`() {
        val recipes = listOf(recipe(1, minutes = 20, seasonality = summerOnly))
        val result = MenuGenerator(Random(0)).draw(recipes, request())
        assertIs<DrawResult.Drawn>(result)
        assertEquals(setOf(SEASON), result.relaxed)
    }

    @Test
    fun `signale toutes les contraintes non respectees quand il faut tout relacher`() {
        val recipes = listOf(recipe(1, minutes = 60, seasonality = summerOnly))
        val result = MenuGenerator(Random(0)).draw(recipes, request(previousWeek = setOf(1)))
        assertIs<DrawResult.Drawn>(result)
        assertEquals(setOf(PREVIOUS_WEEK, DURATION, SEASON), result.relaxed)
    }

    @Test
    fun `la saison est evaluee a la date du jour concerne`() {
        val recipes = listOf(recipe(1, seasonality = summerOnly))
        val july = DrawRequest(LocalDate.of(2026, 7, 14), Freshness.D0, 30)
        val result = MenuGenerator(Random(0)).draw(recipes, july)
        assertIs<DrawResult.Drawn>(result)
        assertTrue(result.relaxed.isEmpty())
    }
}
