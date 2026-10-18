package fr.inventairerepas.data.repository

import androidx.room.withTransaction
import fr.inventairerepas.data.db.AppDatabase
import fr.inventairerepas.data.db.dao.PlanDao
import fr.inventairerepas.data.db.entity.DayPlanEntity
import fr.inventairerepas.data.db.entity.WeekPlanEntity
import fr.inventairerepas.data.db.toDomain
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.week.WeekStructure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class RoomPlanRepository @Inject constructor(
    private val database: AppDatabase,
    private val planDao: PlanDao,
) : PlanRepository {

    override fun observeWeek(weekStart: LocalDate): Flow<List<DayPlan>> =
        planDao.observeRange(weekStart, weekEnd(weekStart)).map { list -> list.map { it.toDomain() } }

    override suspend fun getWeek(weekStart: LocalDate): List<DayPlan> =
        planDao.getRange(weekStart, weekEnd(weekStart)).map { it.toDomain() }

    override suspend fun saveDays(weekStart: LocalDate, days: List<DayPlan>) {
        database.withTransaction {
            planDao.insertWeek(WeekPlanEntity(weekStart))
            // L'identifiant existant est retrouvé par la date, unique, pour mettre à jour la ligne.
            val existingIds = planDao.getRange(weekStart, weekEnd(weekStart)).associate { it.day.date to it.day.id }
            planDao.upsertDays(
                days.map { day ->
                    DayPlanEntity(
                        id = existingIds[day.date] ?: day.id,
                        weekStart = weekStart,
                        date = day.date,
                        mode = day.mode,
                        recipeId = day.recipe?.id,
                        servings = day.servings,
                        noMeal = day.noMeal,
                        cooked = day.cooked,
                        relaxed = day.relaxed,
                    )
                },
            )
        }
    }

    private fun weekEnd(weekStart: LocalDate) = weekStart.plusDays(WeekStructure.DAYS_PER_WEEK - 1L)

    override suspend fun recipeIdsBetween(from: LocalDate, to: LocalDate): Set<Long> =
        planDao.recipeIdsBetween(from, to).toSet()
}
