package fr.inventairerepas.domain.repository

import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.Settings
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.unit.UnitFamily
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface RecipeRepository {
    fun observeAll(): Flow<List<Recipe>>
    fun observe(recipeId: Long): Flow<Recipe?>
    suspend fun getAll(): List<Recipe>
    suspend fun get(recipeId: Long): Recipe?

    /** Crée ou met à jour la recette ; les ingrédients sont rapprochés du catalogue par leur nom. */
    suspend fun save(recipe: Recipe): Long
    suspend fun delete(recipeId: Long)
}

interface IngredientRepository {
    fun observeAll(): Flow<List<Ingredient>>
    suspend fun search(query: String): List<Ingredient>

    /** Identifiant de l'ingrédient du catalogue portant ce nom, créé au besoin. */
    suspend fun getOrCreate(ingredient: Ingredient): Ingredient
    suspend fun setAlwaysInStock(ingredientId: Long, alwaysInStock: Boolean)
}

interface StockRepository {
    fun observeAll(): Flow<List<StockLine>>
    suspend fun getAll(): List<StockLine>

    /** Stock en unité de base pour un ingrédient et une famille ; 0 s'il n'existe pas. */
    suspend fun getBaseAmount(ingredientId: Long, family: UnitFamily): Double

    /** Fixe le stock ; une valeur nulle supprime la ligne. */
    suspend fun setBaseAmount(ingredientId: Long, family: UnitFamily, baseAmount: Double)
}

interface PlanRepository {
    fun observeWeek(weekStart: LocalDate): Flow<List<DayPlan>>
    suspend fun getWeek(weekStart: LocalDate): List<DayPlan>

    /** Enregistre les jours de la semaine (création de la semaine incluse). */
    suspend fun saveDays(weekStart: LocalDate, days: List<DayPlan>)
    suspend fun recipeIdsBetween(from: LocalDate, to: LocalDate): Set<Long>
}

interface ShoppingRepository {
    fun observeWeek(weekStart: LocalDate): Flow<List<ShoppingList>>
    suspend fun getWeek(weekStart: LocalDate): List<ShoppingList>

    /** Remplace la liste du même bloc par cette nouvelle version figée. */
    suspend fun replace(list: ShoppingList)
    suspend fun getItem(itemId: Long): ShoppingItem?
    suspend fun setChecked(itemId: Long, checked: Boolean)
}

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}

/** Exécute un bloc dans une transaction de la base, pour les opérations qui touchent plusieurs tables. */
interface TransactionRunner {
    suspend fun <T> run(block: suspend () -> T): T
}
