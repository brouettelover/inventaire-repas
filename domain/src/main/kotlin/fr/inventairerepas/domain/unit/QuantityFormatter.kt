package fr.inventairerepas.domain.unit

import java.math.BigDecimal
import java.math.RoundingMode

/** Mise en forme française des quantités : « 1,5 kg », « 2 c. à soupe », « 3 pièces ». */
object QuantityFormatter {

    fun format(quantity: Quantity): String {
        val amount = formatAmount(quantity.amount)
        val unit = when {
            quantity.unit == MeasureUnit.PIECE && quantity.amount > 1.0 -> "pièces"
            else -> quantity.unit.symbol
        }
        return "$amount $unit"
    }

    /**
     * Arrondit selon l'ordre de grandeur pour éviter « 333,33 g » :
     * 0 décimale à partir de 100, 1 à partir de 10, 2 en dessous.
     */
    fun formatAmount(amount: Double): String {
        val decimals = when {
            amount >= 100.0 -> 0
            amount >= 10.0 -> 1
            else -> 2
        }
        val rounded = BigDecimal.valueOf(amount).setScale(decimals, RoundingMode.HALF_UP).stripTrailingZeros()
        val plain = if (rounded.signum() == 0) "0" else rounded.toPlainString()
        return plain.replace('.', ',')
    }
}
