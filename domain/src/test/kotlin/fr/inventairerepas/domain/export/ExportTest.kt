package fr.inventairerepas.domain.export

import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.day
import fr.inventairerepas.domain.fixtures.ing
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.ShoppingItem
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.Quantity
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExportTest {

    private val monday = LocalDate.of(2026, 10, 19)
    private val pasta = recipe(
        1, baseServings = 2,
        ingredients = listOf(ing(Ingredients.pasta, 500.0, GRAM), ing(Ingredients.egg, 2.0, PIECE)),
    ).copy(name = "Carbonara")

    private val days = listOf(
        day(monday, pasta, servings = 4),
        day(monday.plusDays(1), null, noMeal = true),
    )
    private val lists = listOf(
        ShoppingList(
            weekStart = monday, blockIndex = 0, shoppingDate = monday,
            items = listOf(
                ShoppingItem(1, Ingredients.pasta, Quantity(1.0, KILOGRAM), checked = true),
                ShoppingItem(2, Ingredients.egg, Quantity(4.0, PIECE)),
            ),
        ),
        ShoppingList(weekStart = monday, blockIndex = 1, shoppingDate = monday.plusDays(3), items = emptyList()),
    )

    @Test
    fun `titre avec les dates de la semaine`() {
        assertEquals("Menu du 19/10/2026 au 25/10/2026", WeekExportBuilder.title(monday))
    }

    @Test
    fun `menu de la semaine et deux listes de courses`() {
        val text = WeekExportBuilder.build(monday, days, lists, includeRecipeDetails = false).toPlainText()
        assertTrue("• Lundi 19 octobre (courses) : Carbonara (4 pers., 20 min)" in text, text)
        assertTrue("• Mardi 20 octobre : sans repas" in text, text)
        assertTrue("• Mercredi 21 octobre : non planifié" in text, text)
        assertTrue("== Courses du Lundi 19 octobre ==" in text, text)
        assertTrue("☑ Pâtes : 1 kg" in text, text)
        assertTrue("☐ Œuf : 4 pièces" in text, text)
        assertTrue("== Courses du Jeudi 22 octobre ==" in text, text)
        assertFalse("Recettes de la semaine" in text)
    }

    @Test
    fun `detail des recettes ajuste aux portions du jour`() {
        val text = WeekExportBuilder.build(monday, days, lists, includeRecipeDetails = true).toPlainText()
        assertTrue("-- Lundi 19 octobre : Carbonara --" in text, text)
        assertTrue("• Pâtes : 1 kg" in text, text)
        assertTrue("• Œuf : 4 pièces" in text, text)
        assertTrue("1. Cuisiner" in text, text)
    }

    @Test
    fun `plan Google Docs aux index coherents`() {
        val document = ExportDocument(
            "T",
            listOf(
                ExportBlock.Title("Titre"),
                ExportBlock.Heading("Menu"),
                ExportBlock.Bullet("a"),
                ExportBlock.Bullet("bb"),
                ExportBlock.Paragraph("p"),
                ExportBlock.Checkbox("x", checked = true),
                ExportBlock.Numbered("n"),
            ),
        )
        val plan = DocsRequestPlan.from(document)
        assertEquals("Titre\nMenu\na\nbb\np\nx ✓\nn\n", plan.text)
        assertEquals(
            listOf(
                DocsRequestPlan.StyledRange(1, 7, DocsRequestPlan.TITLE),
                DocsRequestPlan.StyledRange(7, 12, DocsRequestPlan.HEADING_1),
            ),
            plan.paragraphStyles,
        )
        assertEquals(
            listOf(
                DocsRequestPlan.BulletRange(12, 17, DocsRequestPlan.BULLET_DISC),
                DocsRequestPlan.BulletRange(19, 23, DocsRequestPlan.CHECKBOX),
                DocsRequestPlan.BulletRange(23, 25, DocsRequestPlan.NUMBERED),
            ),
            plan.bullets,
        )
        // Chaque plage se termine juste après un saut de ligne.
        (plan.paragraphStyles.map { it.end } + plan.bullets.map { it.end }).forEach {
            assertEquals('\n', plan.text[it - 2])
        }
    }
}
