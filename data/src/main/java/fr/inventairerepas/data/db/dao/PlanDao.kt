package fr.inventairerepas.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import fr.inventairerepas.data.db.entity.DayPlanEntity
import fr.inventairerepas.data.db.entity.WeekPlanEntity
import fr.inventairerepas.data.db.relation.DayPlanWithRecipe
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PlanDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWeek(week: WeekPlanEntity)

    @Upsert
    suspend fun upsertDays(days: List<DayPlanEntity>)

    @Upsert
    suspend fun upsertDay(day: DayPlanEntity)

    /**
     * Les jours sont lus par plage de dates plutôt que par semaine : si le premier jour de courses
     * change dans les réglages, les jours déjà planifiés restent visibles dans le nouveau découpage.
     */
    @Transaction
    @Query("SELECT * FROM day_plan WHERE date BETWEEN :from AND :to ORDER BY date")
    fun observeRange(from: LocalDate, to: LocalDate): Flow<List<DayPlanWithRecipe>>

    @Transaction
    @Query("SELECT * FROM day_plan WHERE date BETWEEN :from AND :to ORDER BY date")
    suspend fun getRange(from: LocalDate, to: LocalDate): List<DayPlanWithRecipe>

    @Transaction
    @Query("SELECT * FROM day_plan WHERE date = :date")
    suspend fun getDay(date: LocalDate): DayPlanWithRecipe?

    /** Recettes planifiées entre deux dates incluses, pour la règle anti-répétition. */
    @Query("SELECT recipe_id FROM day_plan WHERE date BETWEEN :from AND :to AND recipe_id IS NOT NULL")
    suspend fun recipeIdsBetween(from: LocalDate, to: LocalDate): List<Long>
}
