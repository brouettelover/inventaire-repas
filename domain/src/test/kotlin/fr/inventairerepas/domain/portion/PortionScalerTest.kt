package fr.inventairerepas.domain.portion

import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.ing
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.unit.MeasureUnit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class PortionScalerTest {

    private val base = recipe(
        id = 1,
        baseServings = 4,
        ingredients = listOf(
            ing(Ingredients.pasta, 400.0, MeasureUnit.GRAM),
            ing(Ingredients.egg, 2.0, MeasureUnit.PIECE),
            ing(Ingredients.milk, 1.0, MeasureUnit.TABLESPOON),
        ),
    )

    @Test
    fun `quantites proportionnelles au nombre de personnes`() {
        val scaled = PortionScaler.scale(base, 6)
        assertEquals(listOf(600.0, 3.0, 1.5), scaled.map { it.quantity.amount })
        assertEquals(base.ingredients.map { it.quantity.unit }, scaled.map { it.quantity.unit })
    }

    @Test
    fun `reduire les portions`() {
        assertEquals(listOf(100.0, 0.5, 0.25), PortionScaler.scale(base, 1).map { it.quantity.amount })
    }

    @Test
    fun `memes portions que la base laissent les quantites inchangees`() {
        assertEquals(base.ingredients, PortionScaler.scale(base, 4))
        assertEquals(1.0, PortionScaler.factor(base, 4))
    }

    @Test
    fun `zero personne est refuse`() {
        assertThrows<IllegalArgumentException> { PortionScaler.scale(base, 0) }
    }
}
