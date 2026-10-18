package fr.inventairerepas.domain.backup

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Month

class InvalidBackupException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

/**
 * Fichier de recettes partagé (par exemple sur Google Drive), lisible par l'app sur plusieurs téléphones.
 * Les identifiants locaux ne sont pas exportés : les recettes sont rapprochées par leur nom.
 */
@Serializable
data class RecipeBackupFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAt: Long,
    val recipes: List<RecipeDto>,
) {
    companion object {
        const val FORMAT = "inventaire-repas/recettes"
        const val VERSION = 1
    }
}

@Serializable
data class RecipeDto(
    val name: String,
    val baseServings: Int,
    val prepMinutes: Int,
    val cookMinutes: Int,
    val freshness: String,
    /** Mois de saison, de 1 (janvier) à 12 (décembre). */
    val months: List<Int>,
    val ingredients: List<IngredientDto>,
    val steps: List<String>,
    val updatedAt: Long = 0,
)

@Serializable
data class IngredientDto(
    val name: String,
    val amount: Double,
    val unit: String,
    val alwaysInStock: Boolean = false,
)

object RecipeBackup {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(recipes: List<Recipe>, exportedAt: Long): String =
        json.encodeToString(
            RecipeBackupFile.serializer(),
            RecipeBackupFile(exportedAt = exportedAt, recipes = recipes.sortedBy { it.name.lowercase() }.map { it.toDto() }),
        )

    /** Lit un fichier de recettes ; lève [InvalidBackupException] si le contenu n'est pas reconnu. */
    fun decode(content: String): List<Recipe> {
        val file = try {
            json.decodeFromString(RecipeBackupFile.serializer(), content)
        } catch (e: SerializationException) {
            throw InvalidBackupException("Ce fichier n'est pas un fichier de recettes Inventaire repas", e)
        } catch (e: IllegalArgumentException) {
            throw InvalidBackupException("Ce fichier n'est pas un fichier de recettes Inventaire repas", e)
        }
        if (file.format != RecipeBackupFile.FORMAT) {
            throw InvalidBackupException("Ce fichier n'est pas un fichier de recettes Inventaire repas")
        }
        if (file.version > RecipeBackupFile.VERSION) {
            throw InvalidBackupException("Fichier créé par une version plus récente de l'app : mets-la à jour")
        }
        return file.recipes.map { it.toDomain() }
    }

    private fun Recipe.toDto() = RecipeDto(
        name = name,
        baseServings = baseServings,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        freshness = freshness.name,
        months = seasonality.months.map { it.value }.sorted(),
        ingredients = ingredients.map {
            IngredientDto(it.ingredient.name, it.quantity.amount, it.quantity.unit.name, it.ingredient.alwaysInStock)
        },
        steps = steps,
        updatedAt = updatedAt,
    )

    private fun RecipeDto.toDomain(): Recipe = try {
        Recipe(
            name = name.trim(),
            baseServings = baseServings,
            prepMinutes = prepMinutes,
            cookMinutes = cookMinutes,
            seasonality = if (months.isEmpty()) Seasonality.ALL_YEAR else Seasonality(months.map(Month::of).toSet()),
            freshness = Freshness.valueOf(freshness),
            ingredients = ingredients.map {
                RecipeIngredient(
                    Ingredient(name = it.name.trim(), alwaysInStock = it.alwaysInStock),
                    Quantity(it.amount, MeasureUnit.valueOf(it.unit)),
                )
            },
            steps = steps,
            updatedAt = updatedAt,
        )
    } catch (e: RuntimeException) {
        throw InvalidBackupException("Recette « $name » illisible : ${e.message}", e)
    }
}
