package fr.inventairerepas.domain.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IngredientTest {

    @Test
    fun `le nom normalise ignore casse, accents et espaces`() {
        assertEquals("creme fraiche", Ingredient.normalizeName("  Crème   Fraîche "))
        assertEquals(Ingredient(name = "Tomates").normalizedName, Ingredient(name = "tomates").normalizedName)
        assertEquals("epices a pain d'epice", Ingredient.normalizeName("Épices à pain d'épice"))
    }
}
