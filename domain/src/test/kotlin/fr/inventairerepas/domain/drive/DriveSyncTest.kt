package fr.inventairerepas.domain.drive

import fr.inventairerepas.domain.fixtures.FakeRecipeRepository
import fr.inventairerepas.domain.fixtures.recipe
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.usecase.SyncRecipesWithDriveUseCase
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DriveSyncTest {

    private fun named(id: Long, name: String, updatedAt: Long = 0) = recipe(id).copy(name = name, updatedAt = updatedAt)

    private val file = RemoteRecipeFile("f1", "Soupe", modifiedAt = 1_000)
    private val link = DriveLink(recipeId = 1, fileId = "f1", remoteModifiedAt = 1_000, localUpdatedAt = 500)

    @Test
    fun `nothing to do when both sides are unchanged`() {
        val plan = DriveSyncPlanner.plan(listOf(named(1, "Soupe", 500)), listOf(link), listOf(file))
        assertTrue(plan.isEmpty())
    }

    @Test
    fun `remote change downloads and local change uploads`() {
        val local = named(1, "Soupe", 500)
        assertEquals(
            listOf(SyncAction.Download(file.copy(modifiedAt = 2_000), 1)),
            DriveSyncPlanner.plan(listOf(local), listOf(link), listOf(file.copy(modifiedAt = 2_000))),
        )
        val edited = local.copy(updatedAt = 900)
        assertEquals(listOf(SyncAction.Upload(edited, "f1")), DriveSyncPlanner.plan(listOf(edited), listOf(link), listOf(file)))
    }

    @Test
    fun `when both sides changed the newest wins`() {
        val remote = file.copy(modifiedAt = 2_000)
        val olderLocal = named(1, "Soupe", 1_500)
        val newerLocal = named(1, "Soupe", 3_000)
        assertEquals(listOf(SyncAction.Download(remote, 1)), DriveSyncPlanner.plan(listOf(olderLocal), listOf(link), listOf(remote)))
        assertEquals(listOf(SyncAction.Upload(newerLocal, "f1")), DriveSyncPlanner.plan(listOf(newerLocal), listOf(link), listOf(remote)))
    }

    @Test
    fun `deletions propagate unless the other side changed`() {
        val other = RemoteRecipeFile("f9", "Autre", 1)
        val otherLink = DriveLink(9, "f9", 1, 0)
        val otherRecipe = named(9, "Autre", 0)
        assertEquals(
            listOf(SyncAction.DeleteLocal(1)),
            DriveSyncPlanner.plan(listOf(named(1, "Soupe", 500), otherRecipe), listOf(link, otherLink), listOf(other)),
        )
        val edited = named(1, "Soupe", 900)
        assertEquals(
            listOf(SyncAction.Upload(edited, null)),
            DriveSyncPlanner.plan(listOf(edited, otherRecipe), listOf(link, otherLink), listOf(other)),
        )

        assertEquals(listOf(SyncAction.TrashRemote("f1", 1)), DriveSyncPlanner.plan(emptyList(), listOf(link), listOf(file)))
        val changed = file.copy(modifiedAt = 2_000)
        assertEquals(
            listOf(SyncAction.Download(changed, null, staleLink = 1)),
            DriveSyncPlanner.plan(emptyList(), listOf(link), listOf(changed)),
        )
        assertEquals(listOf(SyncAction.DropLink(1)), DriveSyncPlanner.plan(emptyList(), listOf(link, otherLink), listOf(other)).take(1))
    }

    @Test
    fun `an empty folder never deletes recipes in the app`() {
        val soup = named(1, "Soupe", 500)
        assertEquals(listOf(SyncAction.Upload(soup, null)), DriveSyncPlanner.plan(listOf(soup), listOf(link), emptyList()))
    }

    @Test
    fun `unlinked files and recipes are matched by name or created`() {
        val seed = named(1, "SOUPE", 0)
        val mine = named(2, "Gratin", 0)
        val other = RemoteRecipeFile("f2", "Tarte", 3_000)
        val plan = DriveSyncPlanner.plan(listOf(seed, mine), emptyList(), listOf(file, other))
        assertEquals(
            listOf(
                SyncAction.Download(file, 1),
                SyncAction.Download(other, null),
                SyncAction.Upload(mine, null),
            ),
            plan,
        )
        val newer = named(1, "Soupe", 5_000)
        assertEquals(listOf(SyncAction.Upload(newer, "f1")), DriveSyncPlanner.plan(listOf(newer), emptyList(), listOf(file)))
    }

    @Test
    fun `use case applies the plan and a second run does nothing`() = runTest {
        val recipes = FakeRecipeRepository(listOf(named(1, "Gratin", 0), named(2, "À supprimer", 0)))
        val links = FakeLinks()
        val remote = FakeRemote()
        remote.files["x"] = RemoteRecipeFile("x", "Tarte", 100) to "Portions : 6\nIngrédients\n- 200 g farine"
        remote.files["bad"] = RemoteRecipeFile("bad", "Cassée", 100) to "Cuisson : longtemps"
        val sync = SyncRecipesWithDriveUseCase(recipes, links)

        val first = sync(remote)
        assertEquals(1, first.downloaded)
        assertEquals(2, first.uploaded)
        assertEquals(1, first.errors.size)
        assertTrue(first.errors.single().startsWith("« Cassée »"))
        assertEquals(6, recipes.recipes.first { it.name == "Tarte" }.baseServings)

        val second = sync(remote)
        assertEquals(0, second.downloaded + second.uploaded + second.deletedLocally + second.trashedRemotely)

        // Suppression dans l'app → fichier à la corbeille ; fichier supprimé sur Drive → recette supprimée.
        val deleted = recipes.recipes.first { it.name == "À supprimer" }
        recipes.delete(deleted.id)
        val tarteFile = links.links.getValue(recipes.recipes.first { it.name == "Tarte" }.id).fileId
        remote.files.remove(tarteFile)
        val third = sync(remote)
        assertEquals(1, third.trashedRemotely)
        assertEquals(1, third.deletedLocally)
        assertEquals(listOf("Gratin"), recipes.recipes.map(Recipe::name))
        assertNull(remote.files.values.firstOrNull { it.first.name == "À supprimer" })
    }

    private class FakeLinks : DriveLinkRepository {
        val links = mutableMapOf<Long, DriveLink>()
        override suspend fun getAll() = links.values.toList()
        override suspend fun save(link: DriveLink) {
            links[link.recipeId] = link
        }
        override suspend fun delete(recipeId: Long) {
            links.remove(recipeId)
        }
    }

    private class FakeRemote : RecipeRemote {
        val files = linkedMapOf<String, Pair<RemoteRecipeFile, String>>()
        private var clock = 1_000L
        private var nextId = 0

        override suspend fun list() = files.values.map { it.first }
        override suspend fun read(fileId: String) = files.getValue(fileId).second
        override suspend fun create(name: String, text: String) = put("n${nextId++}", name, text)
        override suspend fun update(fileId: String, name: String, text: String) = put(fileId, name, text)
        override suspend fun trash(fileId: String) {
            files.remove(fileId)
        }

        private fun put(id: String, name: String, text: String): RemoteRecipeFile {
            val file = RemoteRecipeFile(id, name, clock++)
            files[id] = file to text
            return file
        }
    }
}
