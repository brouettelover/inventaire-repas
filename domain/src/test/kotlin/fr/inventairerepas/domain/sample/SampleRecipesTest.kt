package fr.inventairerepas.domain.sample

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleRecipesTest {

    private val recipes = SampleRecipes.all

    @Test
    fun `une dizaine de recettes aux noms uniques`() {
        assertTrue(recipes.size >= 10)
        assertEquals(recipes.size, recipes.map { it.name }.toSet().size)
    }

    @Test
    fun `chaque liste de fraicheur couvre ses jours dans une semaine`() {
        val byFreshness = recipes.groupingBy { it.freshness }.eachCount()
        // Une semaine tire 2 recettes en J+0, 2 en J+1 et 3 en J+2 (dont le 7e jour).
        assertTrue(byFreshness.getValue(Freshness.D0) >= 2)
        assertTrue(byFreshness.getValue(Freshness.D1) >= 2)
        assertTrue(byFreshness.getValue(Freshness.D2) >= 3)
    }

    @Test
    fun `saisons et durees variees`() {
        assertTrue(recipes.any { it.seasonality.isAllYear })
        assertTrue(recipes.any { !it.seasonality.isAllYear })
        assertTrue(recipes.any { it.totalMinutes <= 30 })
        assertTrue(recipes.any { it.totalMinutes > 60 })
    }

    @Test
    fun `chaque recette a des ingredients et des etapes`() {
        recipes.forEach {
            assertTrue(it.ingredients.isNotEmpty(), it.name)
            assertTrue(it.steps.isNotEmpty(), it.name)
        }
    }

    @Test
    fun `un meme ingredient garde le meme statut toujours en stock`() {
        val statuses = recipes.flatMap { it.ingredients }
            .map { it.ingredient }
            .groupBy(Ingredient::normalizedName) { it.alwaysInStock }
        statuses.forEach { (name, values) -> assertEquals(1, values.toSet().size, name) }
    }
}
