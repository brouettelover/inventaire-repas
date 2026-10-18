package fr.inventairerepas.data.seed

import androidx.room.withTransaction
import fr.inventairerepas.data.db.AppDatabase
import fr.inventairerepas.data.db.ingredientEntities
import fr.inventairerepas.data.db.stepEntities
import fr.inventairerepas.data.db.toEntity
import fr.inventairerepas.domain.sample.SampleRecipes
import javax.inject.Inject
import javax.inject.Singleton

/** Insère les recettes d'exemple quand la base ne contient encore aucune recette. */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val database: AppDatabase,
) {
    suspend fun seedIfEmpty() {
        val recipeDao = database.recipeDao()
        val ingredientDao = database.ingredientDao()
        database.withTransaction {
            if (recipeDao.count() > 0) return@withTransaction
            SampleRecipes.all.forEach { recipe ->
                val ingredientIds = recipe.ingredients.map { ingredientDao.getOrCreate(it.ingredient) }
                recipeDao.save(
                    recipe = recipe.toEntity(),
                    lines = recipe.ingredientEntities(ingredientIds),
                    steps = recipe.stepEntities(),
                )
            }
        }
    }
}
