package fr.inventairerepas.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import fr.inventairerepas.domain.unit.UnitFamily

/** Stock d'un ingrédient pour une famille d'unités, en unité de base (g, ml, pièce). */
@Entity(
    tableName = "stock_line",
    primaryKeys = ["ingredient_id", "family"],
    foreignKeys = [
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class StockLineEntity(
    @ColumnInfo(name = "ingredient_id") val ingredientId: Long,
    val family: UnitFamily,
    @ColumnInfo(name = "amount_base") val amountBase: Double,
)
