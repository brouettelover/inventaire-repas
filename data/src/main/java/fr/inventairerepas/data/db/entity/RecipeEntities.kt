package fr.inventairerepas.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.unit.MeasureUnit

@Entity(tableName = "recipe")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "base_servings") val baseServings: Int,
    @ColumnInfo(name = "prep_minutes") val prepMinutes: Int,
    @ColumnInfo(name = "cook_minutes") val cookMinutes: Int,
    val freshness: Freshness,
    /** 12 bits, bit 0 = janvier ; 4095 = toute l'année. */
    @ColumnInfo(name = "season_mask") val seasonMask: Int,
    /** Dernière modification (millisecondes), pour la fusion des fichiers de recettes partagés. */
    @ColumnInfo(name = "updated_at", defaultValue = "0") val updatedAt: Long = 0,
)

@Entity(
    tableName = "recipe_ingredient",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = IngredientEntity::class,
            parentColumns = ["id"],
            childColumns = ["ingredient_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("recipe_id"), Index("ingredient_id")],
)
data class RecipeIngredientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    @ColumnInfo(name = "ingredient_id") val ingredientId: Long,
    /** Quantité pour les portions de base de la recette. */
    val amount: Double,
    val unit: MeasureUnit,
    val position: Int,
)

@Entity(
    tableName = "recipe_step",
    foreignKeys = [
        ForeignKey(
            entity = RecipeEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipe_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("recipe_id")],
)
data class RecipeStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "recipe_id") val recipeId: Long,
    val position: Int,
    val text: String,
)
