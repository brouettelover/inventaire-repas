package fr.inventairerepas.domain.fixtures

import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.SettingsRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import fr.inventairerepas.domain.unit.UnitFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate

class FakeRecipeRepository(var recipes: List<Recipe> = emptyList()) : RecipeRepository {
    override fun observeAll(): Flow<List<Recipe>> = flowOf(recipes)
    override fun observe(recipeId: Long): Flow<Recipe?> = flowOf(recipes.firstOrNull { it.id == recipeId })
    override suspend fun getAll() = recipes
    override suspend fun get(recipeId: Long) = recipes.firstOrNull { it.id == recipeId }
    override suspend fun save(recipe: Recipe): Long {
        val saved = if (recipe.id == 0L) recipe.copy(id = (recipes.maxOfOrNull { it.id } ?: 0) + 1) else recipe
        recipes = recipes.filterNot { it.id == saved.id } + saved
        return saved.id
    }
    override suspend fun delete(recipeId: Long) {
        recipes = recipes.filterNot { it.id == recipeId }
    }
}

class FakeStockRepository(private val ingredients: List<Ingredient> = emptyList()) : StockRepository {
    val amounts = mutableMapOf<Pair<Long, UnitFamily>, Double>()

    override fun observeAll(): Flow<List<StockLine>> = flowOf(lines())
    override suspend fun getAll() = lines()
    override suspend fun getBaseAmount(ingredientId: Long, family: UnitFamily) = amounts[ingredientId to family] ?: 0.0
    override suspend fun setBaseAmount(ingredientId: Long, family: UnitFamily, baseAmount: Double) {
        if (baseAmount <= 0.0) amounts.remove(ingredientId to family) else amounts[ingredientId to family] = baseAmount
    }

    private fun lines() = amounts.map { (key, amount) ->
        StockLine(ingredients.first { it.id == key.first }, key.second, amount)
    }
}

class FakePlanRepository : PlanRepository {
    val weeks = mutableMapOf<LocalDate, List<DayPlan>>()

    override fun observeWeek(weekStart: LocalDate): Flow<List<DayPlan>> = flowOf(weeks[weekStart].orEmpty())
    override suspend fun getWeek(weekStart: LocalDate) = weeks[weekStart].orEmpty()
    override suspend fun saveDays(weekStart: LocalDate, days: List<DayPlan>) {
        weeks[weekStart] = days.sortedBy { it.date }
    }
    override suspend fun recipeIdsBetween(from: LocalDate, to: LocalDate) =
        weeks.values.flatten().filter { !it.date.isBefore(from) && !it.date.isAfter(to) }
            .mapNotNull { it.recipe?.id }.toSet()
}

class FakeShoppingRepository : ShoppingRepository {
    val lists = mutableMapOf<Pair<LocalDate, Int>, ShoppingList>()
    private var nextId = 1L

    override fun observeWeek(weekStart: LocalDate): Flow<List<ShoppingList>> = flowOf(lists.values.toList())
    override suspend fun getWeek(weekStart: LocalDate) =
        lists.values.filter { it.weekStart == weekStart }.sortedBy { it.blockIndex }
    override suspend fun replace(list: ShoppingList) {
        lists[list.weekStart to list.blockIndex] = list.copy(items = list.items.map { it.copy(id = nextId++) })
    }
    override suspend fun getItem(itemId: Long): ShoppingItem? =
        lists.values.flatMap { it.items }.firstOrNull { it.id == itemId }
    override suspend fun setChecked(itemId: Long, checked: Boolean) {
        lists.replaceAll { _, list ->
            list.copy(items = list.items.map { if (it.id == itemId) it.copy(checked = checked) else it })
        }
    }
}

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state
    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value)
    }
}

object DirectTransactionRunner : TransactionRunner {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
