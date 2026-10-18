package fr.inventairerepas.domain.unit

import fr.inventairerepas.domain.unit.MeasureUnit.CENTILITER
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.LITER
import fr.inventairerepas.domain.unit.MeasureUnit.MILLILITER
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.MeasureUnit.TABLESPOON
import fr.inventairerepas.domain.unit.MeasureUnit.TEASPOON
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UnitConverterTest {

    private fun assertQuantity(expectedAmount: Double, expectedUnit: MeasureUnit, actual: Quantity) {
        assertEquals(expectedUnit, actual.unit)
        assertEquals(expectedAmount, actual.amount, 1e-9)
    }

    @Test
    fun `chaque unite appartient a la bonne famille`() {
        assertEquals(UnitFamily.MASS, GRAM.family)
        assertEquals(UnitFamily.MASS, KILOGRAM.family)
        listOf(MILLILITER, CENTILITER, LITER, TEASPOON, TABLESPOON).forEach {
            assertEquals(UnitFamily.VOLUME, it.family)
        }
        assertEquals(UnitFamily.COUNT, PIECE.family)
    }

    @Test
    fun `conversion dans la famille masse`() {
        assertQuantity(1.5, KILOGRAM, Quantity(1500.0, GRAM).convertTo(KILOGRAM))
        assertQuantity(250.0, GRAM, Quantity(0.25, KILOGRAM).convertTo(GRAM))
    }

    @Test
    fun `conversion dans la famille volume, cuilleres comprises`() {
        assertQuantity(50.0, CENTILITER, Quantity(0.5, LITER).convertTo(CENTILITER))
        assertQuantity(15.0, MILLILITER, Quantity(1.0, TABLESPOON).convertTo(MILLILITER))
        assertQuantity(3.0, TEASPOON, Quantity(1.0, TABLESPOON).convertTo(TEASPOON))
        assertQuantity(2.0, TABLESPOON, Quantity(3.0, CENTILITER).convertTo(TABLESPOON))
    }

    @Test
    fun `convertir vers la meme unite rend la quantite inchangee`() {
        val q = Quantity(3.0, PIECE)
        assertEquals(q, q.convertTo(PIECE))
    }

    @Test
    fun `pas de conversion entre familles`() {
        assertThrows<IncompatibleUnitsException> { Quantity(100.0, GRAM).convertTo(MILLILITER) }
        assertThrows<IncompatibleUnitsException> { Quantity(2.0, PIECE).convertTo(GRAM) }
    }

    @Test
    fun `500 g plus 1 kg donnent 1,5 kg`() {
        val result = UnitConverter.aggregate(listOf(Quantity(500.0, GRAM), Quantity(1.0, KILOGRAM)))
        val mass = result.getValue(UnitFamily.MASS)
        assertEquals(1500.0, mass.baseAmount, 1e-9)
        assertNull(mass.sameUnit)
        assertQuantity(1.5, KILOGRAM, mass.toDisplayQuantity())
    }

    @Test
    fun `meme ingredient en grammes et en pieces donne deux lignes`() {
        val result = UnitConverter.aggregate(listOf(Quantity(200.0, GRAM), Quantity(2.0, PIECE), Quantity(100.0, GRAM)))
        assertEquals(setOf(UnitFamily.MASS, UnitFamily.COUNT), result.keys)
        assertQuantity(300.0, GRAM, result.getValue(UnitFamily.MASS).toDisplayQuantity())
        assertQuantity(2.0, PIECE, result.getValue(UnitFamily.COUNT).toDisplayQuantity())
    }

    @Test
    fun `une unite commune est conservee a l affichage`() {
        val result = UnitConverter.aggregate(listOf(Quantity(1.0, TABLESPOON), Quantity(2.0, TABLESPOON)))
        assertQuantity(3.0, TABLESPOON, result.getValue(UnitFamily.VOLUME).toDisplayQuantity())
    }

    @Test
    fun `des volumes melanges sont convertis vers l unite la plus lisible`() {
        val small = UnitConverter.aggregate(listOf(Quantity(2.0, TABLESPOON), Quantity(20.0, CENTILITER)))
        assertQuantity(230.0, MILLILITER, small.getValue(UnitFamily.VOLUME).toDisplayQuantity())

        val large = UnitConverter.aggregate(listOf(Quantity(75.0, CENTILITER), Quantity(500.0, MILLILITER)))
        assertQuantity(1.25, LITER, large.getValue(UnitFamily.VOLUME).toDisplayQuantity())
    }

    @Test
    fun `meilleure unite selon le montant`() {
        assertEquals(GRAM, UnitConverter.bestUnit(999.0, UnitFamily.MASS))
        assertEquals(KILOGRAM, UnitConverter.bestUnit(1000.0, UnitFamily.MASS))
        assertEquals(MILLILITER, UnitConverter.bestUnit(999.0, UnitFamily.VOLUME))
        assertEquals(LITER, UnitConverter.bestUnit(1000.0, UnitFamily.VOLUME))
        assertEquals(PIECE, UnitConverter.bestUnit(12.0, UnitFamily.COUNT))
    }

    @Test
    fun `une unite preferee d une autre famille est ignoree`() {
        assertQuantity(1.2, KILOGRAM, UnitConverter.toDisplay(1200.0, UnitFamily.MASS, preferred = LITER))
    }

    @Test
    fun `multiplier une quantite par un facteur`() {
        assertQuantity(300.0, GRAM, Quantity(200.0, GRAM) * 1.5)
    }

    @Test
    fun `une quantite negative est refusee`() {
        assertThrows<IllegalArgumentException> { Quantity(-1.0, GRAM) }
    }

    @Test
    fun `affichage lisible des unites metriques, cuilleres et pieces gardees`() {
        assertQuantity(1.2, KILOGRAM, UnitConverter.readable(Quantity(1200.0, GRAM)))
        assertQuantity(500.0, MILLILITER, UnitConverter.readable(Quantity(50.0, CENTILITER)))
        assertQuantity(4.0, TABLESPOON, UnitConverter.readable(Quantity(4.0, TABLESPOON)))
        assertQuantity(12.0, PIECE, UnitConverter.readable(Quantity(12.0, PIECE)))
    }

    @Test
    fun `une unite metrique commune est ramenee a la plus lisible`() {
        val result = UnitConverter.aggregate(listOf(Quantity(600.0, GRAM), Quantity(600.0, GRAM)))
        assertQuantity(1.2, KILOGRAM, result.getValue(UnitFamily.MASS).toDisplayQuantity())
    }
}
