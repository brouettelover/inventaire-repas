package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.repository.PlanRepository
import fr.inventairerepas.domain.repository.ShoppingRepository
import fr.inventairerepas.domain.repository.StockRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import fr.inventairerepas.domain.stock.StockCalculator
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitFamily
import java.time.LocalDate
import javax.inject.Inject

/**
 * Marque la recette d'un jour comme cuisinée (ingrédients ajustés aux portions déduits du stock)
 * ou annule ce marquage (ingrédients remis en stock).
 */
class SetCookedUseCase @Inject constructor(
    private val plans: PlanRepository,
    private val stock: StockRepository,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(weekStart: LocalDate, date: LocalDate, cooked: Boolean) {
        transaction.run {
            val days = plans.getWeek(weekStart)
            val day = days.firstOrNull { it.date == date } ?: error("Jour introuvable : $date")
            if (day.cooked == cooked) return@run
            val recipe = day.recipe ?: error("Aucune recette prévue le $date")

            StockCalculator.consumption(recipe, day.servings).forEach { need ->
                val ingredientId = need.ingredient.id
                val family = need.quantity.family
                val current = stock.getBaseAmount(ingredientId, family)
                val quantity = Quantity(need.quantity.baseAmount, family.baseUnit)
                val updated = if (cooked) {
                    StockCalculator.remove(current, quantity)
                } else {
                    StockCalculator.add(current, quantity)
                }
                stock.setBaseAmount(ingredientId, family, updated)
            }
            plans.saveDays(weekStart, days.map { if (it.date == date) it.copy(cooked = cooked) else it })
        }
    }
}

/** Cocher un article l'ajoute au stock ; le décocher le retire. */
class ToggleShoppingItemUseCase @Inject constructor(
    private val shopping: ShoppingRepository,
    private val stock: StockRepository,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(itemId: Long, checked: Boolean) {
        transaction.run {
            val item = shopping.getItem(itemId) ?: error("Article introuvable : $itemId")
            if (item.checked == checked) return@run
            val family = item.quantity.family
            val current = stock.getBaseAmount(item.ingredient.id, family)
            val updated = if (checked) {
                StockCalculator.add(current, item.quantity)
            } else {
                StockCalculator.remove(current, item.quantity)
            }
            stock.setBaseAmount(item.ingredient.id, family, updated)
            shopping.setChecked(itemId, checked)
        }
    }
}

/** Correction manuelle du stock. */
class AdjustStockUseCase @Inject constructor(private val stock: StockRepository) {
    suspend operator fun invoke(ingredientId: Long, quantity: Quantity) {
        stock.setBaseAmount(ingredientId, quantity.family, quantity.baseAmount)
    }

    suspend fun clear(ingredientId: Long, family: UnitFamily) {
        stock.setBaseAmount(ingredientId, family, 0.0)
    }
}
