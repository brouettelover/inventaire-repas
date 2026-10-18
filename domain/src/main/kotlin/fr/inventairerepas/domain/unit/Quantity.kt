package fr.inventairerepas.domain.unit

/** Une quantité exprimée dans une unité donnée. */
data class Quantity(val amount: Double, val unit: MeasureUnit) {

    init {
        require(amount >= 0.0) { "Une quantité ne peut pas être négative : $amount" }
    }

    val family: UnitFamily get() = unit.family

    /** Montant exprimé dans l'unité de base de la famille (g, ml, pièce). */
    val baseAmount: Double get() = amount * unit.toBase

    operator fun times(factor: Double): Quantity = copy(amount = amount * factor)

    /** Convertit vers [target], qui doit appartenir à la même famille. */
    fun convertTo(target: MeasureUnit): Quantity = UnitConverter.convert(this, target)
}
