package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import fr.inventairerepas.data.db.entity.StockLineEntity
import fr.inventairerepas.data.db.relation.StockLineWithIngredient
import fr.inventairerepas.domain.unit.UnitFamily
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {

    @Transaction
    @Query(
        "SELECT stock_line.* FROM stock_line " +
            "INNER JOIN ingredient ON ingredient.id = stock_line.ingredient_id " +
            "ORDER BY ingredient.name COLLATE NOCASE",
    )
    fun observeAll(): Flow<List<StockLineWithIngredient>>

    @Transaction
    @Query("SELECT * FROM stock_line")
    suspend fun getAll(): List<StockLineWithIngredient>

    @Query("SELECT * FROM stock_line WHERE ingredient_id = :ingredientId AND family = :family")
    suspend fun get(ingredientId: Long, family: UnitFamily): StockLineEntity?

    @Upsert
    suspend fun upsert(line: StockLineEntity)

    @Upsert
    suspend fun upsertAll(lines: List<StockLineEntity>)

    @Query("DELETE FROM stock_line WHERE ingredient_id = :ingredientId AND family = :family")
    suspend fun delete(ingredientId: Long, family: UnitFamily)
}
