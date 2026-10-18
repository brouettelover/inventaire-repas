package fr.inventairerepas.domain.model

import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.unit.UnitConverter
import fr.inventairerepas.domain.unit.UnitFamily

/**
 * Stock d'un ingrédient pour une famille d'unités, en unité de base (g, ml, pièce).
 * Un même ingrédient peut avoir une ligne en masse et une ligne en pièces.
 */
data class StockLine(
    val ingredient: Ingredient,
    val family: UnitFamily,
    val baseAmount: Double,
) {
    fun toDisplayQuantity(): Quantity = UnitConverter.toDisplay(baseAmount, family)
}
