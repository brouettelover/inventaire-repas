package fr.inventairerepas.data.repository

import fr.inventairerepas.data.db.dao.IngredientDao
import fr.inventairerepas.data.db.toDomain
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.repository.IngredientRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomIngredientRepository @Inject constructor(
    private val ingredientDao: IngredientDao,
) : IngredientRepository {

    override fun observeAll(): Flow<List<Ingredient>> =
        ingredientDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun search(query: String): List<Ingredient> =
        ingredientDao.search(Ingredient.normalizeName(query)).map { it.toDomain() }

    override suspend fun getOrCreate(ingredient: Ingredient): Ingredient {
        val id = ingredientDao.getOrCreate(ingredient)
        return requireNotNull(ingredientDao.getById(id)).toDomain()
    }

    override suspend fun setAlwaysInStock(ingredientId: Long, alwaysInStock: Boolean) {
        val current = ingredientDao.getById(ingredientId) ?: return
        ingredientDao.update(current.copy(alwaysInStock = alwaysInStock))
    }
}
