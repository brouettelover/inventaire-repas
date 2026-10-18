package fr.inventairerepas.domain.backup

import fr.inventairerepas.domain.fixtures.DirectTransactionRunner
import fr.inventairerepas.domain.fixtures.FakeRecipeRepository
import fr.inventairerepas.domain.fixtures.Ingredients
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.sample.SampleRecipes
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import fr.inventairerepas.domain.usecase.ExportRecipesUseCase
import fr.inventairerepas.domain.usecase.ImportRecipesUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Month
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecipeBackupTest {

    private val pasta = recipe(
        id = 4,
        freshness = Freshness.D2,
        seasonality = Seasonality.range(Month.OCTOBER, Month.FEBRUARY),
        ingredients = listOf(
            RecipeIngredient(Ingredients.pasta, Quantity(200.0, MeasureUnit.GRAM)),
            RecipeIngredient(Ingredients.salt, Quantity(1.0, MeasureUnit.TEASPOON)),
        ),
    ).copy(name = "Pâtes à la crème", steps = listOf("Cuire", "Servir"), updatedAt = 1_000)

    @Test
    fun `encode then decode keeps recipe content without local ids`() {
        val decoded = RecipeBackup.decode(RecipeBackup.encode(listOf(pasta), exportedAt = 5))

        val recipe = decoded.single()
        assertEquals(0L, recipe.id)
        assertEquals("Pâtes à la crème", recipe.name)
        assertEquals(pasta.seasonality, recipe.seasonality)
        assertEquals(Freshness.D2, recipe.freshness)
        assertEquals(listOf("Cuire", "Servir"), recipe.steps)
        assertEquals(1_000L, recipe.updatedAt)
        assertEquals(listOf("Pâtes", "Sel"), recipe.ingredients.map { it.ingredient.name })
        assertEquals(Quantity(1.0, MeasureUnit.TEASPOON), recipe.ingredients[1].quantity)
        assertTrue(recipe.ingredients[1].ingredient.alwaysInStock)
    }

    @Test
    fun `all sample recipes survive a round trip`() {
        val samples = SampleRecipes.all
        val decoded = RecipeBackup.decode(RecipeBackup.encode(samples, exportedAt = 0))
        assertEquals(samples.map { it.name }.sorted(), decoded.map { it.name }.sorted())
    }

    @Test
    fun `decode ignores unknown fields`() {
        val json = """
            {"format":"inventaire-repas/recettes","version":1,"exportedAt":0,"futur":true,
             "recipes":[{"name":"Soupe","baseServings":2,"prepMinutes":5,"cookMinutes":10,"freshness":"D1",
               "months":[],"ingredients":[],"steps":[],"note":"x"}]}
        """.trimIndent()
        val recipe = RecipeBackup.decode(json).single()
        assertEquals("Soupe", recipe.name)
        assertEquals(Seasonality.ALL_YEAR, recipe.seasonality)
    }

    @Test
    fun `decode rejects other files and newer versions`() {
        assertThrows<InvalidBackupException> { RecipeBackup.decode("pas du json") }
        assertThrows<InvalidBackupException> { RecipeBackup.decode("""{"format":"autre","exportedAt":0,"recipes":[]}""") }
        assertThrows<InvalidBackupException> {
            RecipeBackup.decode("""{"format":"inventaire-repas/recettes","version":99,"exportedAt":0,"recipes":[]}""")
        }
        assertThrows<InvalidBackupException> {
            RecipeBackup.decode(
                """{"format":"inventaire-repas/recettes","exportedAt":0,"recipes":[{"name":"X","baseServings":0,
                "prepMinutes":0,"cookMinutes":0,"freshness":"D0","months":[],"ingredients":[],"steps":[]}]}""",
            )
        }
    }

    @Test
    fun `merge adds unknown recipes and matches names ignoring case and accents`() {
        val incoming = listOf(
            pasta.copy(id = 0, name = "PATES A LA CREME", updatedAt = 2_000, steps = listOf("Nouveau")),
            recipe(id = 0).copy(name = "Nouvelle"),
        )
        val result = RecipeMerger.merge(listOf(pasta), incoming)

        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        assertEquals(0, result.unchanged)
        val updated = result.toSave.first { it.steps == listOf("Nouveau") }
        assertEquals(pasta.id, updated.id)
        assertEquals(0L, result.toSave.first { it.name == "Nouvelle" }.id)
    }

    @Test
    fun `merge keeps local recipe when it is newer or identical`() {
        val older = pasta.copy(id = 0, updatedAt = 500, steps = listOf("Ancien"))
        val sameButNewer = pasta.copy(id = 0, updatedAt = 9_000)
        assertEquals(1, RecipeMerger.merge(listOf(pasta), listOf(older)).unchanged)
        val result = RecipeMerger.merge(listOf(pasta), listOf(sameButNewer))
        assertEquals(1, result.unchanged)
        assertTrue(result.toSave.isEmpty())
    }

    @Test
    fun `merge keeps the newest duplicate from the file and never deletes`() {
        val local = listOf(pasta, recipe(id = 9).copy(name = "Seulement ici"))
        val incoming = listOf(
            pasta.copy(id = 0, updatedAt = 3_000, steps = listOf("A")),
            pasta.copy(id = 0, updatedAt = 4_000, steps = listOf("B")),
        )
        val result = RecipeMerger.merge(local, incoming)
        assertEquals(listOf("B"), result.toSave.single().steps)
        assertEquals(0, result.added)
    }

    @Test
    fun `import use case saves merged recipes and export writes them all`() = runTest {
        val repo = FakeRecipeRepository(listOf(pasta))
        val file = RecipeBackup.encode(listOf(pasta.copy(updatedAt = 2_000, steps = listOf("Neuf")), recipe(id = 7)), 0)

        val report = ImportRecipesUseCase(repo, DirectTransactionRunner)(file)

        assertEquals(1, report.added)
        assertEquals(1, report.updated)
        assertEquals(2, repo.recipes.size)
        assertEquals(listOf("Neuf"), repo.recipes.first { it.id == pasta.id }.steps)

        val exported = RecipeBackup.decode(ExportRecipesUseCase(repo)(now = 1))
        assertEquals(setOf(pasta.name, "Recette 7"), exported.map { it.name }.toSet())
    }
}
