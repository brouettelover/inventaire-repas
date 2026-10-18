package fr.inventairerepas.data.repository

import fr.inventairerepas.data.db.dao.StockDao
import fr.inventairerepas.data.db.entity.StockLineEntity
import fr.inventairerepas.data.db.toDomain
import fr.inventairerepas.domain.model.StockLine
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.unit.UnitFamily
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoomStockRepository @Inject constructor(
    private val stockDao: StockDao,
) : StockRepository {

    override fun observeAll(): Flow<List<StockLine>> =
        stockDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<StockLine> = stockDao.getAll().map { it.toDomain() }

    override suspend fun getBaseAmount(ingredientId: Long, family: UnitFamily): Double =
        stockDao.get(ingredientId, family)?.amountBase ?: 0.0

    override suspend fun setBaseAmount(ingredientId: Long, family: UnitFamily, baseAmount: Double) {
        if (baseAmount <= 0.0) {
            stockDao.delete(ingredientId, family)
        } else {
            stockDao.upsert(StockLineEntity(ingredientId, family, baseAmount))
        }
    }
}
