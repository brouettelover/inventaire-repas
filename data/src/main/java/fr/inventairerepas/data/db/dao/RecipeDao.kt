package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.inventairerepas.data.db.entity.RecipeEntity
import fr.inventairerepas.data.db.entity.RecipeIngredientEntity
import fr.inventairerepas.data.db.entity.RecipeStepEntity
import fr.inventairerepas.data.db.relation.RecipeWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
abstract class RecipeDao {

    @Insert
    abstract suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Update
    abstract suspend fun updateRecipe(recipe: RecipeEntity)

    @Insert
    abstract suspend fun insertIngredients(lines: List<RecipeIngredientEntity>)

    @Insert
    abstract suspend fun insertSteps(steps: List<RecipeStepEntity>)

    @Query("DELETE FROM recipe_ingredient WHERE recipe_id = :recipeId")
    abstract suspend fun deleteIngredients(recipeId: Long)

    @Query("DELETE FROM recipe_step WHERE recipe_id = :recipeId")
    abstract suspend fun deleteSteps(recipeId: Long)

    @Query("DELETE FROM recipe WHERE id = :recipeId")
    abstract suspend fun deleteRecipe(recipeId: Long)

    @Query("SELECT COUNT(*) FROM recipe")
    abstract suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM recipe ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<RecipeWithDetails>>

    @Transaction
    @Query("SELECT * FROM recipe ORDER BY name COLLATE NOCASE")
    abstract suspend fun getAll(): List<RecipeWithDetails>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :recipeId")
    abstract fun observeById(recipeId: Long): Flow<RecipeWithDetails?>

    @Transaction
    @Query("SELECT * FROM recipe WHERE id = :recipeId")
    abstract suspend fun getById(recipeId: Long): RecipeWithDetails?

    /**
     * Crée ou remplace une recette avec ses ingrédients et ses étapes.
     * Les lignes reçues peuvent avoir un `recipeId` quelconque : il est fixé ici.
     */
    @Transaction
    open suspend fun save(
        recipe: RecipeEntity,
        lines: List<RecipeIngredientEntity>,
        steps: List<RecipeStepEntity>,
    ): Long {
        val recipeId = if (recipe.id == 0L) {
            insertRecipe(recipe)
        } else {
            updateRecipe(recipe)
            deleteIngredients(recipe.id)
            deleteSteps(recipe.id)
            recipe.id
        }
        insertIngredients(lines.map { it.copy(id = 0, recipeId = recipeId) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = recipeId) })
        return recipeId
    }
}
