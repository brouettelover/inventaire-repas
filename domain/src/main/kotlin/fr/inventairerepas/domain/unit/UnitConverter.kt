package fr.inventairerepas.domain.unit

class IncompatibleUnitsException(from: MeasureUnit, to: MeasureUnit) :
    IllegalArgumentException("Conversion impossible de ${from.symbol} vers ${to.symbol}")

/**
 * Somme de quantités d'une même famille, gardée en unité de base.
 *
 * [sameUnit] est l'unité commune quand toutes les quantités additionnées
 * avaient la même unité, ce qui permet de garder les cuillères à l'affichage
 * (2 c. à soupe restent 2 c. à soupe plutôt que 30 ml).
 */
data class AggregatedQuantity(
    val family: UnitFamily,
    val baseAmount: Double,
    val sameUnit: MeasureUnit?,
) {
    /** Quantité à afficher : cuillères ou pièces communes gardées, sinon l'unité la plus lisible. */
    fun toDisplayQuantity(): Quantity = UnitConverter.toDisplay(baseAmount, family, sameUnit)

    operator fun plus(other: AggregatedQuantity): AggregatedQuantity {
        require(family == other.family) { "Familles différentes : $family et ${other.family}" }
        return AggregatedQuantity(
            family = family,
            baseAmount = baseAmount + other.baseAmount,
            sameUnit = if (sameUnit == other.sameUnit) sameUnit else null,
        )
    }

    fun withBaseAmount(newBaseAmount: Double): AggregatedQuantity = copy(baseAmount = newBaseAmount)

    companion object {
        fun of(quantity: Quantity) =
            AggregatedQuantity(quantity.family, quantity.baseAmount, quantity.unit)
    }
}

object UnitConverter {

    fun convert(quantity: Quantity, target: MeasureUnit): Quantity {
        if (quantity.unit == target) return quantity
        if (quantity.family != target.family) throw IncompatibleUnitsException(quantity.unit, target)
        return Quantity(quantity.baseAmount / target.toBase, target)
    }

    /**
     * Additionne des quantités en les regroupant par famille.
     * Aucune conversion n'a lieu entre familles : 200 g et 2 pièces donnent deux résultats.
     */
    fun aggregate(quantities: Iterable<Quantity>): Map<UnitFamily, AggregatedQuantity> =
        quantities
            .groupBy { it.family }
            .mapValues { (_, list) -> list.map(AggregatedQuantity::of).reduce(AggregatedQuantity::plus) }

    /** Unité la plus lisible pour un montant de base : 1500 g → kg, 250 ml → ml. */
    fun bestUnit(baseAmount: Double, family: UnitFamily): MeasureUnit = when (family) {
        UnitFamily.MASS -> if (baseAmount >= 1000.0) MeasureUnit.KILOGRAM else MeasureUnit.GRAM
        UnitFamily.VOLUME -> if (baseAmount >= 1000.0) MeasureUnit.LITER else MeasureUnit.MILLILITER
        UnitFamily.COUNT -> MeasureUnit.PIECE
    }

    /**
     * Quantité lisible : les cuillères sont gardées telles quelles (« 3 c. à soupe »), les unités
     * métriques sont ramenées à la plus lisible (1200 g → 1,2 kg, 50 cl → 500 ml).
     */
    fun toDisplay(baseAmount: Double, family: UnitFamily, preferred: MeasureUnit? = null): Quantity {
        val unit = preferred?.takeIf { it.family == family && it in KEPT_UNITS } ?: bestUnit(baseAmount, family)
        return Quantity(baseAmount / unit.toBase, unit)
    }

    fun readable(quantity: Quantity): Quantity = toDisplay(quantity.baseAmount, quantity.family, quantity.unit)

    private val KEPT_UNITS = setOf(MeasureUnit.TEASPOON, MeasureUnit.TABLESPOON, MeasureUnit.PIECE)
}
