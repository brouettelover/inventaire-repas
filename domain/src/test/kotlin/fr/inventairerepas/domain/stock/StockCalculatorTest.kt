package fr.inventairerepas.domain.stock

import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.ing
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.MeasureUnit.TABLESPOON
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitFamily
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class StockCalculatorTest {

    @Test
    fun `ajouter convertit vers l'unite de base`() {
        assertEquals(1700.0, StockCalculator.add(200.0, Quantity(1.5, KILOGRAM)))
    }

    @Test
    fun `retirer ne descend jamais sous zero`() {
        assertEquals(100.0, StockCalculator.remove(600.0, Quantity(0.5, KILOGRAM)))
        assertEquals(0.0, StockCalculator.remove(100.0, Quantity(500.0, GRAM)))
    }

    @Test
    fun `consommation ajustee aux portions, agregee, sans les ingredients toujours en stock`() {
        val r = recipe(
            1, baseServings = 2,
            ingredients = listOf(
                ing(Ingredients.pasta, 200.0, GRAM),
                ing(Ingredients.pasta, 0.1, KILOGRAM),
                ing(Ingredients.egg, 2.0, PIECE),
                ing(Ingredients.salt, 1.0, TABLESPOON),
            ),
        )
        val consumption = StockCalculator.consumption(r, servings = 4)
            .associate { (it.ingredient.name to it.quantity.family) to it.quantity.baseAmount }
        assertEquals(
            mapOf(("Pâtes" to UnitFamily.MASS) to 600.0, ("Œuf" to UnitFamily.COUNT) to 4.0),
            consumption,
        )
    }
}
