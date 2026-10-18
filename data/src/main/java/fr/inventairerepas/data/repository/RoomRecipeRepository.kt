package fr.inventairerepas.data.repository

import androidx.room.withTransaction
import fr.inventairerepas.data.db.AppDatabase
import fr.inventairerepas.data.db.dao.IngredientDao
import fr.inventairerepas.data.db.dao.RecipeDao
import fr.inventairerepas.data.db.ingredientEntities
import fr.inventairerepas.data.db.stepEntities
import fr.inventairerepas.data.db.toDomain
import fr.inventairerepas.data.db.toEntity
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.repository.RecipeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomRecipeRepository @Inject constructor(
    private val database: AppDatabase,
    private val recipeDao: RecipeDao,
    private val ingredientDao: IngredientDao,
) : RecipeRepository {

    override fun observeAll(): Flow<List<Recipe>> =
        recipeDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observe(recipeId: Long): Flow<Recipe?> =
        recipeDao.observeById(recipeId).map { it?.toDomain() }

    override suspend fun getAll(): List<Recipe> = recipeDao.getAll().map { it.toDomain() }

    override suspend fun get(recipeId: Long): Recipe? = recipeDao.getById(recipeId)?.toDomain()

    override suspend fun save(recipe: Recipe): Long = database.withTransaction {
        val ingredientIds = recipe.ingredients.map { ingredientDao.getOrCreate(it.ingredient) }
        recipeDao.save(
            recipe = recipe.toEntity(),
            lines = recipe.ingredientEntities(ingredientIds),
            steps = recipe.stepEntities(),
        )
    }

    override suspend fun delete(recipeId: Long) = recipeDao.deleteRecipe(recipeId)
}
