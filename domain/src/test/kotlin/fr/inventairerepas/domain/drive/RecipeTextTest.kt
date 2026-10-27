package fr.inventairerepas.domain.drive

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.sample.SampleRecipes
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeTextTest {

    @Test
    fun `all sample recipes survive format then parse`() {
        SampleRecipes.all.forEach { recipe ->
            val parsed = RecipeText.parse(recipe.name, RecipeText.format(recipe))
            val expected = recipe.copy(
                id = 0,
                ingredients = recipe.ingredients.map { it.copy(ingredient = it.ingredient.copy(id = 0, alwaysInStock = false)) },
            )
            assertEquals(expected, parsed, recipe.name)
        }
    }

    @Test
    fun `parses a hand written recipe with loose formatting`() {
        val text = """
            ﻿Ma super recette
            PORTIONS: 4
            Préparation : 1h30
            cuisson :45
            Fraicheur : j+2
            Saison : oct-fév, juin

            Ingrédients :
            • 200g de farine
            * 1,5 L lait
            - 1/2 c. à café sel
            - 2 cuillères à soupe d’huile d'olive
            - 3 œufs
            - 1 pièce citron
            - Poivre

            Préparation
            1) Mélanger.
            2. Cuire.
            Servir chaud
        """.trimIndent()

        val recipe = RecipeText.parse("Crêpes ", text)

        assertEquals("Crêpes", recipe.name)
        assertEquals(4, recipe.baseServings)
        assertEquals(90, recipe.prepMinutes)
        assertEquals(45, recipe.cookMinutes)
        assertEquals(Freshness.D2, recipe.freshness)
        assertEquals(
            setOf(Month.OCTOBER, Month.NOVEMBER, Month.DECEMBER, Month.JANUARY, Month.FEBRUARY, Month.JUNE),
            recipe.seasonality.months,
        )
        assertEquals(
            listOf(
                "farine" to Quantity(200.0, MeasureUnit.GRAM),
                "lait" to Quantity(1.5, MeasureUnit.LITER),
                "sel" to Quantity(0.5, MeasureUnit.TEASPOON),
                "huile d'olive" to Quantity(2.0, MeasureUnit.TABLESPOON),
                "œufs" to Quantity(3.0, MeasureUnit.PIECE),
                "citron" to Quantity(1.0, MeasureUnit.PIECE),
                "Poivre" to Quantity(1.0, MeasureUnit.PIECE),
            ),
            recipe.ingredients.map { it.ingredient.name to it.quantity },
        )
        assertEquals(listOf("Mélanger.", "Cuire.", "Servir chaud"), recipe.steps)
    }

    @Test
    fun `missing fields take default values`() {
        val recipe = RecipeText.parse("Salade", "Ingrédients\n- 1 salade")
        assertEquals(2, recipe.baseServings)
        assertEquals(0, recipe.totalMinutes)
        assertEquals(Freshness.D0, recipe.freshness)
        assertEquals(Seasonality.ALL_YEAR, recipe.seasonality)
        assertTrue(recipe.steps.isEmpty())
    }

    @Test
    fun `unit words are not confused with ingredient names`() {
        val recipe = RecipeText.parse("Test", "Ingrédients\n- 100 lardons\n- 2 cassis\n- 1 l lait")
        assertEquals(
            listOf(MeasureUnit.PIECE, MeasureUnit.PIECE, MeasureUnit.LITER),
            recipe.ingredients.map { it.quantity.unit },
        )
        assertEquals("lardons", recipe.ingredients[0].ingredient.name)
    }

    @Test
    fun `reports every problem at once`() {
        val error = assertThrows<InvalidRecipeTextException> {
            RecipeText.parse("X", "Portions : zéro\nCuisson : longtemps\nFraîcheur : J+5\nSaison : printemps\nIngrédients\n- 0 g sucre")
        }
        assertEquals(5, error.problems.size)
    }

    @Test
    fun `durations are formatted readably`() {
        assertEquals("0 min", RecipeText.formatMinutes(0))
        assertEquals("45 min", RecipeText.formatMinutes(45))
        assertEquals("2 h", RecipeText.formatMinutes(120))
        assertEquals("1 h 05", RecipeText.formatMinutes(65))
        assertEquals(65, RecipeText.parseMinutes("1 h 05"))
    }
}
