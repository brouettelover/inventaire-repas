package fr.inventairerepas.domain.unit

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QuantityFormatterTest {

    @Test
    fun `virgule decimale et zeros inutiles supprimes`() {
        assertEquals("1,5 kg", QuantityFormatter.format(Quantity(1.5, MeasureUnit.KILOGRAM)))
        assertEquals("2 c. à soupe", QuantityFormatter.format(Quantity(2.0, MeasureUnit.TABLESPOON)))
    }

    @Test
    fun `pluriel des pieces`() {
        assertEquals("1 pièce", QuantityFormatter.format(Quantity(1.0, MeasureUnit.PIECE)))
        assertEquals("3 pièces", QuantityFormatter.format(Quantity(3.0, MeasureUnit.PIECE)))
        assertEquals("0,5 pièce", QuantityFormatter.format(Quantity(0.5, MeasureUnit.PIECE)))
    }

    @Test
    fun `arrondi selon l ordre de grandeur`() {
        assertEquals("333", QuantityFormatter.formatAmount(333.333))
        assertEquals("33,3", QuantityFormatter.formatAmount(33.333))
        assertEquals("3,33", QuantityFormatter.formatAmount(3.333))
        assertEquals("1,25", QuantityFormatter.formatAmount(1.25))
        assertEquals("0", QuantityFormatter.formatAmount(0.0))
        assertEquals("100", QuantityFormatter.formatAmount(100.0))
    }
}
