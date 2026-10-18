package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.export.ExportDocument
import fr.inventairerepas.domain.export.WeekExportBuilder
import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.shopping.ShoppingListCalculator
import fr.inventairerepas.domain.week.WeekStructure
import java.time.LocalDate
import javax.inject.Inject

/**
 * Contenu à exporter pour une semaine. Les listes de courses enregistrées sont reprises telles
 * quelles (articles cochés compris) ; si elles n'ont pas encore été générées, elles sont calculées
 * à la volée sans être enregistrées.
 */
class BuildWeekExportUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val shopping: ShoppingRepository,
    private val stock: StockRepository,
) {
    suspend operator fun invoke(weekStart: LocalDate, includeRecipeDetails: Boolean): ExportDocument {
        val days = plans.getWeek(weekStart)
        val lists = shopping.getWeek(weekStart).ifEmpty { preview(weekStart, days) }
        return WeekExportBuilder.build(weekStart, days, lists, includeRecipeDetails)
    }

    private suspend fun preview(weekStart: LocalDate, days: List<DayPlan>): List<ShoppingList> {
        val byDate = days.associateBy { it.date }
        val slots = WeekStructure.slots(weekStart)
        val blocks = (0 until WeekStructure.BLOCK_COUNT).map { block ->
            slots.filter { it.blockIndex == block }.mapNotNull { byDate[it.date] }
        }
        return ShoppingListCalculator.compute(blocks, stock.getAll()).mapIndexed { block, lines ->
            ShoppingList(
                weekStart = weekStart,
                blockIndex = block,
                shoppingDate = slots.first { it.blockIndex == block }.shoppingDate,
                items = lines.map { ShoppingItem(ingredient = it.ingredient, quantity = it.quantity) },
            )
        }
    }
}
