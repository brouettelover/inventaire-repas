package fr.inventairerepas.domain.shopping

import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.day
import fr.inventairerepas.domain.fixtures.ing
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.LITER
import fr.inventairerepas.domain.unit.MeasureUnit.MILLILITER
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.MeasureUnit.TABLESPOON
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitFamily
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShoppingListCalculatorTest {

    private val d1 = LocalDate.of(2026, 10, 19)
    private val d2 = d1.plusDays(1)
    private val d4 = d1.plusDays(3)

    private val pastaRecipe = recipe(
        1, baseServings = 2,
        ingredients = listOf(
            ing(Ingredients.pasta, 500.0, GRAM),
            ing(Ingredients.tomato, 2.0, PIECE),
            ing(Ingredients.salt, 1.0, TABLESPOON),
        ),
    )
    private val bigPastaRecipe = recipe(
        2, baseServings = 4,
        ingredients = listOf(ing(Ingredients.pasta, 1.0, KILOGRAM), ing(Ingredients.tomato, 300.0, GRAM)),
    )
    private val milkRecipe = recipe(
        3, baseServings = 1,
        ingredients = listOf(ing(Ingredients.milk, 2.0, TABLESPOON), ing(Ingredients.milk, 1.0, TABLESPOON)),
    )

    private fun stock(ingredient: Ingredient, family: UnitFamily, base: Double) =
        StockLine(ingredient, family, base)

    private fun List<ShoppingLine>.byName() = associate { it.ingredient.name to it.quantity }

    @Test
    fun `agrege les ingredients apres portions et conversion`() {
        // 500 g pour 2 + 1 kg pour 4 servi à 4 = 1,5 kg de pâtes.
        val (list) = ShoppingListCalculator.compute(
            listOf(listOf(day(d1, pastaRecipe, servings = 2), day(d2, bigPastaRecipe, servings = 4))),
            emptyList(),
        )
        assertEquals(Quantity(1.5, KILOGRAM), list.byName()["Pâtes"])
    }

    @Test
    fun `meme ingredient en grammes et en pieces donne deux lignes`() {
        val (list) = ShoppingListCalculator.compute(
            listOf(listOf(day(d1, pastaRecipe), day(d2, bigPastaRecipe, servings = 4))),
            emptyList(),
        )
        val tomatoes = list.filter { it.ingredient == Ingredients.tomato }.map { it.quantity }.toSet()
        assertEquals(setOf(Quantity(2.0, PIECE), Quantity(300.0, GRAM)), tomatoes)
    }

    @Test
    fun `les portions du jour multiplient les quantites`() {
        val (list) = ShoppingListCalculator.compute(listOf(listOf(day(d1, pastaRecipe, servings = 3))), emptyList())
        assertEquals(Quantity(750.0, GRAM), list.byName()["Pâtes"])
        assertEquals(Quantity(3.0, PIECE), list.byName()["Tomate"])
    }

    @Test
    fun `les ingredients toujours en stock n'apparaissent jamais`() {
        val (list) = ShoppingListCalculator.compute(listOf(listOf(day(d1, pastaRecipe))), emptyList())
        assertTrue(list.none { it.ingredient.alwaysInStock })
    }

    @Test
    fun `affiche besoin moins stock et masque ce qui est couvert`() {
        val (list) = ShoppingListCalculator.compute(
            listOf(listOf(day(d1, pastaRecipe))),
            listOf(stock(Ingredients.pasta, UnitFamily.MASS, 200.0), stock(Ingredients.tomato, UnitFamily.COUNT, 5.0)),
        )
        assertEquals(mapOf("Pâtes" to Quantity(300.0, GRAM)), list.byName())
    }

    @Test
    fun `le stock d'une autre famille ne couvre pas le besoin`() {
        val (list) = ShoppingListCalculator.compute(
            listOf(listOf(day(d1, pastaRecipe))),
            listOf(stock(Ingredients.tomato, UnitFamily.MASS, 1000.0)),
        )
        assertEquals(Quantity(2.0, PIECE), list.byName()["Tomate"])
    }

    @Test
    fun `l'unite commune est gardee a l'affichage`() {
        val (list) = ShoppingListCalculator.compute(listOf(listOf(day(d1, milkRecipe, servings = 1))), emptyList())
        assertEquals(Quantity(3.0, TABLESPOON), list.byName()["Lait"])
    }

    @Test
    fun `le stock couvre d'abord le premier bloc puis le second`() {
        val (first, second) = ShoppingListCalculator.compute(
            listOf(listOf(day(d1, pastaRecipe)), listOf(day(d4, pastaRecipe))),
            listOf(stock(Ingredients.pasta, UnitFamily.MASS, 700.0)),
        )
        assertTrue(first.none { it.ingredient == Ingredients.pasta })
        assertEquals(Quantity(300.0, GRAM), second.byName()["Pâtes"])
    }

    @Test
    fun `jours cuisines, sans repas ou vides ignores`() {
        val (list) = ShoppingListCalculator.compute(
            listOf(
                listOf(
                    day(d1, pastaRecipe, cooked = true),
                    day(d2, bigPastaRecipe, noMeal = true),
                    day(d1.plusDays(2), null),
                ),
            ),
            emptyList(),
        )
        assertTrue(list.isEmpty())
    }

    @Test
    fun `des volumes melanges deviennent des litres`() {
        val soup = recipe(4, baseServings = 1, ingredients = listOf(ing(Ingredients.milk, 0.5, LITER), ing(Ingredients.milk, 750.0, MILLILITER)))
        val (list) = ShoppingListCalculator.compute(listOf(listOf(day(d1, soup, servings = 1))), emptyList())
        assertEquals(Quantity(1.25, LITER), list.byName()["Lait"])
    }
}
