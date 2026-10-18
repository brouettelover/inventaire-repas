package fr.inventairerepas.data.repository

import fr.inventairerepas.data.db.dao.ShoppingDao
import fr.inventairerepas.data.db.entity.ShoppingItemEntity
import fr.inventairerepas.data.db.entity.ShoppingListEntity
import fr.inventairerepas.data.db.toDomain
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.unit.Quantity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomShoppingRepository @Inject constructor(
    private val shoppingDao: ShoppingDao,
) : ShoppingRepository {

    override fun observeWeek(weekStart: LocalDate): Flow<List<ShoppingList>> =
        shoppingDao.observeWeek(weekStart).map { list -> list.map { it.toDomain() } }

    override suspend fun getWeek(weekStart: LocalDate): List<ShoppingList> =
        shoppingDao.getWeek(weekStart).map { it.toDomain() }

    override suspend fun replace(list: ShoppingList) {
        shoppingDao.replaceList(
            list = ShoppingListEntity(
                weekStart = list.weekStart,
                blockIndex = list.blockIndex,
                shoppingDate = list.shoppingDate,
                generatedAt = System.currentTimeMillis(),
            ),
            items = list.items.map {
                ShoppingItemEntity(
                    listId = 0,
                    ingredientId = it.ingredient.id,
                    amount = it.quantity.amount,
                    unit = it.quantity.unit,
                    checked = it.checked,
                )
            },
        )
    }

    override suspend fun getItem(itemId: Long): ShoppingItem? {
        val item = shoppingDao.getItemWithIngredient(itemId) ?: return null
        return ShoppingItem(
            id = item.item.id,
            ingredient = item.ingredient.toDomain(),
            quantity = Quantity(item.item.amount, item.item.unit),
            checked = item.item.checked,
        )
    }

    override suspend fun setChecked(itemId: Long, checked: Boolean) = shoppingDao.setChecked(itemId, checked)
}
