package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import fr.inventairerepas.data.db.entity.IngredientEntity
import fr.inventairerepas.domain.model.Ingredient
import kotlinx.coroutines.flow.Flow

@Dao
abstract class IngredientDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    abstract suspend fun insert(ingredient: IngredientEntity): Long

    @Update
    abstract suspend fun update(ingredient: IngredientEntity)

    @Query("SELECT * FROM ingredient WHERE id = :id")
    abstract suspend fun getById(id: Long): IngredientEntity?

    @Query("SELECT * FROM ingredient WHERE normalized_name = :normalizedName")
    abstract suspend fun findByNormalizedName(normalizedName: String): IngredientEntity?

    @Query("SELECT * FROM ingredient ORDER BY name COLLATE NOCASE")
    abstract fun observeAll(): Flow<List<IngredientEntity>>

    /** Autocomplétion : [normalizedQuery] doit être normalisé avec [Ingredient.normalizeName]. */
    @Query(
        "SELECT * FROM ingredient WHERE normalized_name LIKE '%' || :normalizedQuery || '%' " +
            "ORDER BY name COLLATE NOCASE LIMIT 20",
    )
    abstract suspend fun search(normalizedQuery: String): List<IngredientEntity>

    /** Renvoie l'identifiant de l'ingrédient du catalogue portant ce nom, en le créant au besoin. */
    @Transaction
    open suspend fun getOrCreate(ingredient: Ingredient): Long {
        val normalized = ingredient.normalizedName
        findByNormalizedName(normalized)?.let { return it.id }
        return insert(
            IngredientEntity(
                name = ingredient.name.trim(),
                normalizedName = normalized,
                alwaysInStock = ingredient.alwaysInStock,
            ),
        )
    }
}
