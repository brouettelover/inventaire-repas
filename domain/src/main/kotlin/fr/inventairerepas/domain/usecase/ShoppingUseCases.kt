package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import fr.inventairerepas.domain.shopping.ShoppingListCalculator
import fr.inventairerepas.domain.week.WeekStructure
import java.time.LocalDate
import javax.inject.Inject

/**
 * Génère (ou régénère) les deux listes de courses de la semaine à partir du menu et du stock,
 * puis les fige. Une liste régénérée repart décochée.
 */
class GenerateShoppingListsUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val stock: StockRepository,
    private val shopping: ShoppingRepository,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(weekStart: LocalDate): List<ShoppingList> = transaction.run {
        val days = plans.getWeek(weekStart).associateBy { it.date }
        val slots = WeekStructure.slots(weekStart)
        val blocks = (0 until WeekStructure.BLOCK_COUNT).map { block ->
            slots.filter { it.blockIndex == block }.mapNotNull { days[it.date] }
        }
        val lines = ShoppingListCalculator.compute(blocks, stock.getAll())
        lines.mapIndexed { block, blockLines ->
            ShoppingList(
                weekStart = weekStart,
                blockIndex = block,
                shoppingDate = slots.first { it.blockIndex == block }.shoppingDate,
                items = blockLines.map { ShoppingItem(ingredient = it.ingredient, quantity = it.quantity) },
            ).also { shopping.replace(it) }
        }
    }
}
