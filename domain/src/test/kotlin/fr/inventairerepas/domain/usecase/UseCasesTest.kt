package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.fixtures.DirectTransactionRunner
import fr.inventairerepas.domain.fixtures.FakePlanRepository
import fr.inventairerepas.domain.fixtures.FakeRecipeRepository
import fr.inventairerepas.domain.fixtures.FakeSettingsRepository
import fr.inventairerepas.domain.fixtures.FakeShoppingRepository
import fr.inventairerepas.domain.fixtures.FakeStockRepository
import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.day
import fr.inventairerepas.domain.fixtures.ing
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.menu.MenuGenerator
import fr.inventairerepas.domain.menu.WeekPlanner
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.RelaxedConstraint
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.MeasureUnit.TABLESPOON
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitFamily
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UseCasesTest {

    private val monday = LocalDate.of(2026, 10, 19)
    private val allIngredients = listOf(Ingredients.pasta, Ingredients.tomato, Ingredients.egg, Ingredients.salt)

    private val pasta = recipe(
        1, Freshness.D0, baseServings = 2,
        ingredients = listOf(
            ing(Ingredients.pasta, 500.0, GRAM),
            ing(Ingredients.egg, 2.0, PIECE),
            ing(Ingredients.salt, 1.0, TABLESPOON),
        ),
    )

    private val plans = FakePlanRepository()
    private val stock = FakeStockRepository(allIngredients)
    private val shopping = FakeShoppingRepository()
    private val settings = FakeSettingsRepository(
        Settings(DayOfWeek.MONDAY, defaultServings = 2, maxMinutesByDay = DayOfWeek.entries.associateWith { null }),
    )

    private fun week(vararg days: DayPlan) = plans.weeks.put(monday, days.toList())

    @Test
    fun `cuisiner deduit les ingredients ajustes aux portions, annuler les remet`() = runTest {
        stock.setBaseAmount(Ingredients.pasta.id, UnitFamily.MASS, 1000.0)
        stock.setBaseAmount(Ingredients.egg.id, UnitFamily.COUNT, 2.0)
        week(day(monday, pasta, servings = 3))
        val setCooked = SetCookedUseCase(plans, stock, DirectTransactionRunner)

        setCooked(monday, monday, cooked = true)
        assertEquals(250.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
        // 3 œufs nécessaires pour 2 en stock : le stock tombe à zéro, pas en négatif.
        assertEquals(0.0, stock.getBaseAmount(Ingredients.egg.id, UnitFamily.COUNT))
        assertTrue(plans.getWeek(monday).single().cooked)

        setCooked(monday, monday, cooked = false)
        assertEquals(1000.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
        assertEquals(3.0, stock.getBaseAmount(Ingredients.egg.id, UnitFamily.COUNT))
        assertEquals(false, plans.getWeek(monday).single().cooked)
    }

    @Test
    fun `marquer deux fois cuisinee ne deduit qu'une fois`() = runTest {
        stock.setBaseAmount(Ingredients.pasta.id, UnitFamily.MASS, 1000.0)
        week(day(monday, pasta))
        val setCooked = SetCookedUseCase(plans, stock, DirectTransactionRunner)
        setCooked(monday, monday, true)
        setCooked(monday, monday, true)
        assertEquals(500.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
    }

    @Test
    fun `un jour cuisine est verrouille`() = runTest {
        week(day(monday, pasta, cooked = true))
        val update = UpdateDayUseCase(plans, DirectTransactionRunner)
        assertThrows<IllegalStateException> { update.setServings(monday, monday, 4) }
    }

    @Test
    fun `listes de courses generees puis cochage qui alimente le stock`() = runTest {
        stock.setBaseAmount(Ingredients.pasta.id, UnitFamily.MASS, 700.0)
        week(day(monday, pasta), day(monday.plusDays(3), pasta))
        val lists = GenerateShoppingListsUseCase(plans, stock, shopping, DirectTransactionRunner)(monday)

        assertEquals(listOf(monday, monday.plusDays(3)), lists.map { it.shoppingDate })
        assertEquals(listOf(Ingredients.egg), shopping.getWeek(monday)[0].items.map { it.ingredient })
        val secondPasta = shopping.getWeek(monday)[1].items.first { it.ingredient == Ingredients.pasta }
        assertEquals(Quantity(300.0, GRAM), secondPasta.quantity)

        val toggle = ToggleShoppingItemUseCase(shopping, stock, DirectTransactionRunner)
        toggle(secondPasta.id, checked = true)
        assertEquals(1000.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
        assertTrue(shopping.getItem(secondPasta.id)!!.checked)

        toggle(secondPasta.id, checked = false)
        assertEquals(700.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
    }

    @Test
    fun `cocher deux fois n'ajoute qu'une fois`() = runTest {
        week(day(monday, pasta))
        GenerateShoppingListsUseCase(plans, stock, shopping, DirectTransactionRunner)(monday)
        val item = shopping.getWeek(monday)[0].items.first { it.ingredient == Ingredients.pasta }
        val toggle = ToggleShoppingItemUseCase(shopping, stock, DirectTransactionRunner)
        toggle(item.id, true)
        toggle(item.id, true)
        assertEquals(500.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
    }

    @Test
    fun `correction manuelle du stock`() = runTest {
        val adjust = AdjustStockUseCase(stock)
        adjust(Ingredients.pasta.id, Quantity(1.2, KILOGRAM))
        assertEquals(1200.0, stock.getBaseAmount(Ingredients.pasta.id, UnitFamily.MASS))
        adjust.clear(Ingredients.pasta.id, UnitFamily.MASS)
        assertTrue(stock.getAll().isEmpty())
    }

    @Test
    fun `nouvelle semaine tiree en evitant la semaine precedente`() = runTest {
        val recipes = listOf(recipe(1, Freshness.D0), recipe(2, Freshness.D0), recipe(3, Freshness.D0)) +
            (4L..6L).map { recipe(it, Freshness.D1) } + (7L..10L).map { recipe(it, Freshness.D2) }
        val previous = monday.minusWeeks(1)
        plans.weeks[previous] = listOf(day(previous, recipes[0]), day(previous.plusDays(3), recipes[1]))
        val ensure = EnsureWeekUseCase(
            plans, FakeRecipeRepository(recipes), settings,
            WeekPlanner(MenuGenerator(Random(1))), DirectTransactionRunner,
        )

        val days = ensure(monday)
        assertEquals(7, days.size)
        // Seule la recette 3 est hors semaine précédente en J+0 : le second jour J+0 doit relâcher.
        assertEquals(3L, listOf(days[0], days[3]).first { it.relaxed.isEmpty() }.recipe!!.id)
        assertEquals(setOf(RelaxedConstraint.PREVIOUS_WEEK), listOf(days[0], days[3]).first { it.relaxed.isNotEmpty() }.relaxed)

        // Un second appel ne retire rien.
        assertEquals(days, ensure(monday))
    }

    @Test
    fun `choisir une recette passe le jour en manuel`() = runTest {
        week(day(monday, null))
        UpdateDayUseCase(plans, DirectTransactionRunner).chooseRecipe(monday, monday, pasta)
        val updated = plans.getWeek(monday).single()
        assertEquals(DayMode.MANUAL, updated.mode)
        assertEquals(pasta, updated.recipe)
    }
}
