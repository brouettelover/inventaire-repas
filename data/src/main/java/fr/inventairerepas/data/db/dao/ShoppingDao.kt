package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import fr.inventairerepas.data.db.entity.ShoppingItemEntity
import fr.inventairerepas.data.db.entity.ShoppingListEntity
import fr.inventairerepas.data.db.relation.ShoppingListWithItems
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
abstract class ShoppingDao {

    @Insert
    abstract suspend fun insertList(list: ShoppingListEntity): Long

    @Insert
    abstract suspend fun insertItems(items: List<ShoppingItemEntity>)

    @Query("DELETE FROM shopping_list WHERE week_start = :weekStart AND block_index = :blockIndex")
    abstract suspend fun deleteList(weekStart: LocalDate, blockIndex: Int)

    @Query("UPDATE shopping_item SET checked = :checked WHERE id = :itemId")
    abstract suspend fun setChecked(itemId: Long, checked: Boolean)

    @Query("SELECT * FROM shopping_item WHERE id = :itemId")
    abstract suspend fun getItem(itemId: Long): ShoppingItemEntity?

    @Transaction
    @Query("SELECT * FROM shopping_list WHERE week_start = :weekStart ORDER BY block_index")
    abstract fun observeWeek(weekStart: LocalDate): Flow<List<ShoppingListWithItems>>

    @Transaction
    @Query("SELECT * FROM shopping_list WHERE week_start = :weekStart ORDER BY block_index")
    abstract suspend fun getWeek(weekStart: LocalDate): List<ShoppingListWithItems>

    /** Remplace la liste d'un jour de courses par une nouvelle version figée. */
    @Transaction
    open suspend fun replaceList(list: ShoppingListEntity, items: List<ShoppingItemEntity>): Long {
        deleteList(list.weekStart, list.blockIndex)
        val listId = insertList(list.copy(id = 0))
        insertItems(items.map { it.copy(id = 0, listId = listId) })
        return listId
    }
}
