package fr.inventairerepas.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.inventairerepas.data.db.dao.IngredientDao
import fr.inventairerepas.data.db.dao.PlanDao
import fr.inventairerepas.data.db.dao.RecipeDao
import fr.inventairerepas.data.db.dao.ShoppingDao
import fr.inventairerepas.data.db.dao.StockDao
import fr.inventairerepas.data.db.entity.DayPlanEntity
import fr.inventairerepas.data.db.entity.IngredientEntity
import fr.inventairerepas.data.db.entity.RecipeEntity
import fr.inventairerepas.data.db.entity.RecipeIngredientEntity
import fr.inventairerepas.data.db.entity.RecipeStepEntity
import fr.inventairerepas.data.db.entity.ShoppingItemEntity
import fr.inventairerepas.data.db.entity.ShoppingListEntity
import fr.inventairerepas.data.db.entity.StockLineEntity
import fr.inventairerepas.data.db.entity.WeekPlanEntity

@Database(
    entities = [
        IngredientEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class,
        RecipeStepEntity::class,
        StockLineEntity::class,
        WeekPlanEntity::class,
        DayPlanEntity::class,
        ShoppingListEntity::class,
        ShoppingItemEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ingredientDao(): IngredientDao
    abstract fun recipeDao(): RecipeDao
    abstract fun stockDao(): StockDao
    abstract fun planDao(): PlanDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        const val NAME = "inventaire-repas.db"

        /** v2 : date de modification des recettes (fusion des fichiers partagés). */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE recipe ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Toutes les migrations, dans l'ordre. Chaque changement de schéma en ajoute une : aucune donnée n'est jamais effacée. */
        val MIGRATIONS = arrayOf(MIGRATION_1_2)
    }
}
