package fr.inventairerepas.domain.usecase

import fr.inventairerepas.domain.backup.MergeResult
import fr.inventairerepas.domain.backup.RecipeBackup
import fr.inventairerepas.domain.backup.RecipeMerger
import fr.inventairerepas.domain.repository.RecipeRepository
import fr.inventairerepas.domain.repository.TransactionRunner
import javax.inject.Inject

/** Contenu du fichier de recettes à écrire (export). */
class ExportRecipesUseCase @Inject constructor(private val recipes: RecipeRepository) {
    suspend operator fun invoke(now: Long = System.currentTimeMillis()): String =
        RecipeBackup.encode(recipes.getAll(), exportedAt = now)
}

/** Importe un fichier de recettes en le fusionnant avec les recettes locales. */
class ImportRecipesUseCase @Inject constructor(
    private val recipes: RecipeRepository,
    private val transaction: TransactionRunner,
) {
    suspend operator fun invoke(content: String): MergeResult {
        val incoming = RecipeBackup.decode(content)
        return transaction.run {
            val result = RecipeMerger.merge(recipes.getAll(), incoming)
            result.toSave.forEach { recipes.save(it) }
            result
        }
    }
}
