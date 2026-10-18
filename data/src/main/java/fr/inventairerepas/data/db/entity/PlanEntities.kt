package fr.inventairerepas.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.inventairerepas.domain.model.DayMode
import fr.inventairerepas.domain.model.RelaxedConstraint
import fr.inventairerepas.domain.unit.MeasureUnit
import java.time.LocalDate

@Entity(tableName = "week_plan")
data class WeekPlanEntity(
    @PrimaryKey @ColumnInfo(name = "start_date") val startDate: LocalDate,
)

/** Un jour du menu. Les lignes des semaines passées servent d'historique pour l'anti-répétition. */
@Entity(
    tableName = "day_plan",
    foreignKeys = [
        ForeignKey(
            entity = WeekPlanEntity::class,
            parentColumns = ["start_date"],
            childColumns = ["week_start"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("week_start"), Index(value = ["date"], unique = true), Index("recipe_id")],
)
data class DayPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "week_start") val weekStart: LocalDate,
    val date: LocalDate,
    val mode: DayMode,
    @ColumnInfo(name = "recipe_id") val recipeId: Long?,
    val servings: Int,
    @ColumnInfo(name = "no_meal") val noMeal: Boolean,
    val cooked: Boolean,
    val relaxed: Set<RelaxedConstraint>,
)

@Entity(
    tableName = "shopping_list",
    foreignKeys = [
        ForeignKey(
            entity = WeekPlanEntity::class,
            parentColumns = ["start_date"],
            childColumns = ["week_start"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["week_start", "block_index"], unique = true)],
)
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "week_start") val weekStart: LocalDate,
    @ColumnInfo(name = "block_index") val blockIndex: Int,
    @ColumnInfo(name = "shopping_date") val shoppingDate: LocalDate,
    @ColumnInfo(name = "generated_at") val generatedAt: Long,
)

@Entity(
    tableName = "shopping_item",
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["list_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("list_id"), Index("ingredient_id")],
)
data class ShoppingItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "list_id") val listId: Long,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Long,
    /** Quantité affichée, qui est aussi celle ajoutée au stock au cochage. */
    val amount: Double,
    val unit: MeasureUnit,
    val checked: Boolean,
)
