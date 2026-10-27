package fr.inventairerepas.domain.drive

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.Quantity
import java.math.BigDecimal
import java.time.Month

class InvalidRecipeTextException(val problems: List<String>) :
    IllegalArgumentException(problems.joinToString(" ; "))

/**
 * Format texte d'une recette, pensé pour être lu et modifié à la main dans Google Docs.
 * Le nom de la recette est le nom du fichier. Exemple :
 *
 * ```
 * Portions : 2
 * Préparation : 10 min
 * Cuisson : 1 h 15
 * Fraîcheur : J+1
 * Saison : octobre à février
 *
 * Ingrédients
 * - 200 g pâtes
 * - 1 c. à soupe huile d'olive
 * - 2 œufs
 *
 * Étapes
 * 1. Cuire les pâtes.
 * 2. Servir.
 * ```
 *
 * La lecture est tolérante : majuscules, accents, puces et numéros sont facultatifs,
 * et les champs absents prennent une valeur par défaut.
 */
object RecipeText {

    fun format(recipe: Recipe): String = buildString {
        appendLine("Portions : ${recipe.baseServings}")
        appendLine("Préparation : ${formatMinutes(recipe.prepMinutes)}")
        appendLine("Cuisson : ${formatMinutes(recipe.cookMinutes)}")
        appendLine("Fraîcheur : ${recipe.freshness.label}")
        appendLine("Saison : ${formatSeason(recipe.seasonality)}")
        appendLine()
        appendLine("Ingrédients")
        recipe.ingredients.forEach { line ->
            appendLine("- ${formatAmount(line.quantity.amount)} ${unitLabel(line.quantity)}${line.ingredient.name}")
        }
        appendLine()
        appendLine("Étapes")
        recipe.steps.forEachIndexed { index, step -> appendLine("${index + 1}. $step") }
    }

    /** Lit une recette ; lève [InvalidRecipeTextException] avec la liste des lignes incomprises. */
    fun parse(name: String, text: String): Recipe {
        val problems = mutableListOf<String>()
        var servings = DEFAULT_SERVINGS
        var prep = 0
        var cook = 0
        var freshness = Freshness.D0
        var seasonality = Seasonality.ALL_YEAR
        val ingredients = mutableListOf<RecipeIngredient>()
        val steps = mutableListOf<String>()
        var section = Section.HEADER

        text.removePrefix("﻿").lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.forEach { line ->
            sectionOf(line)?.let {
                section = it
                return@forEach
            }
            val field = if (section == Section.HEADER) fieldOf(line) else null
            if (field != null) {
                val (key, value) = field
                runCatching {
                    when (key) {
                        "portions", "personnes" -> servings = value.filter(Char::isDigit).toIntOrNull()
                            ?.takeIf { it > 0 } ?: error("nombre de portions invalide « $value »")
                        "preparation" -> prep = parseMinutes(value)
                        "cuisson" -> cook = parseMinutes(value)
                        "fraicheur" -> freshness = parseFreshness(value)
                        "saison" -> seasonality = parseSeason(value)
                    }
                }.onFailure { problems += it.message ?: line }
                return@forEach
            }
            when (section) {
                Section.HEADER -> Unit // titre ou commentaire libre : ignoré
                Section.INGREDIENTS -> runCatching { ingredients += parseIngredient(stripBullet(line)) }
                    .onFailure { problems += "ingrédient « $line » : ${it.message}" }
                Section.STEPS -> steps += stripBullet(line)
            }
        }

        val title = name.trim()
        if (title.isEmpty()) problems += "nom de recette vide"
        if (problems.isNotEmpty()) throw InvalidRecipeTextException(problems)
        return Recipe(
            name = title,
            baseServings = servings,
            prepMinutes = prep,
            cookMinutes = cook,
            seasonality = seasonality,
            freshness = freshness,
            ingredients = ingredients,
            steps = steps,
        )
    }

    private enum class Section { HEADER, INGREDIENTS, STEPS }

    private const val DEFAULT_SERVINGS = 2

    private fun sectionOf(line: String): Section? = when (normalize(line).trimEnd(':', ' ')) {
        "ingredients", "ingredient" -> Section.INGREDIENTS
        "etapes", "etape", "instructions", "preparation" -> Section.STEPS
        else -> null
    }

    private fun fieldOf(line: String): Pair<String, String>? {
        val colon = line.indexOf(':')
        if (colon <= 0) return null
        val key = normalize(line.substring(0, colon))
        if (key !in FIELDS) return null
        return key to line.substring(colon + 1).trim()
    }

    private val FIELDS = setOf("portions", "personnes", "preparation", "cuisson", "fraicheur", "saison")

    private fun stripBullet(line: String): String =
        line.replace(Regex("""^(?:[-•*●◦▪]+|\d+\s*[.)])\s*"""), "").trim()

    // --- Durées -------------------------------------------------------------------------------

    fun formatMinutes(minutes: Int): String = when {
        minutes < 60 -> "$minutes min"
        minutes % 60 == 0 -> "${minutes / 60} h"
        else -> "${minutes / 60} h ${(minutes % 60).toString().padStart(2, '0')}"
    }

    /** « 10 min », « 1 h », « 1 h 30 », « 1h30 », « 90 ». */
    fun parseMinutes(value: String): Int {
        val v = normalize(value).replace(" ", "")
        if (v.isEmpty() || v == "0") return 0
        Regex("""^(\d+)(?:min|mn|minutes?)?$""").matchEntire(v)?.let { return it.groupValues[1].toInt() }
        Regex("""^(\d+)(?:h|heures?)(\d+)?(?:min|mn|minutes?)?$""").matchEntire(v)?.let {
            return it.groupValues[1].toInt() * 60 + (it.groupValues[2].toIntOrNull() ?: 0)
        }
        error("durée invalide « $value » (exemples : 15 min, 1 h 30)")
    }

    // --- Fraîcheur et saison ------------------------------------------------------------------

    private fun parseFreshness(value: String): Freshness {
        val digit = normalize(value).replace(" ", "").removePrefix("j").removePrefix("+")
        return Freshness.entries.firstOrNull { it.daysAfterShopping.toString() == digit }
            ?: error("fraîcheur invalide « $value » (J+0, J+1 ou J+2)")
    }

    private val MONTH_NAMES = listOf(
        "janvier", "fevrier", "mars", "avril", "mai", "juin",
        "juillet", "aout", "septembre", "octobre", "novembre", "decembre",
    )
    private val MONTH_LABELS = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    )

    fun formatSeason(seasonality: Seasonality): String =
        if (seasonality.isAllYear) "toute l'année"
        else Month.entries.filter { it in seasonality.months }.joinToString(", ") { MONTH_LABELS[it.ordinal] }

    /** « toute l'année », « janvier, mars », « octobre à février », « oct-fév ». */
    fun parseSeason(value: String): Seasonality {
        val v = normalize(value)
        if (v.isEmpty() || v.startsWith("toute") || v == "tout" || v == "toujours") return Seasonality.ALL_YEAR
        val months = mutableSetOf<Month>()
        v.split(',', ';', '/', '+').map { it.trim() }.filter { it.isNotEmpty() }.forEach { part ->
            val range = part.split(Regex("""\s+a\s+|\s*-\s*|\s+au\s+"""))
            when (range.size) {
                1 -> months += month(range[0])
                2 -> months += Seasonality.range(month(range[0]), month(range[1])).months
                else -> error("saison invalide « $value »")
            }
        }
        return Seasonality(months)
    }

    private fun month(token: String): Month {
        val t = token.trim().trimEnd('.')
        MONTH_NAMES.indexOf(t).takeIf { it >= 0 }?.let { return Month.entries[it] }
        val candidates = MONTH_NAMES.withIndex().filter { t.length >= 3 && it.value.startsWith(t) }
        return candidates.singleOrNull()?.let { Month.entries[it.index] }
            ?: error("mois inconnu « $token »")
    }

    // --- Ingrédients --------------------------------------------------------------------------

    private fun unitLabel(quantity: Quantity): String = when (quantity.unit) {
        MeasureUnit.PIECE -> "" // « 2 œufs » se lit mieux que « 2 pièces œufs »
        else -> "${quantity.unit.symbol} "
    }

    private fun formatAmount(amount: Double): String =
        BigDecimal.valueOf(amount).stripTrailingZeros().toPlainString().replace('.', ',')

    /** Libellés acceptés pour chaque unité, comparés sans accents ni majuscules ; le plus long d'abord. */
    private val UNIT_ALIASES: List<Pair<String, MeasureUnit>> = listOf(
        MeasureUnit.GRAM to listOf("g", "gr", "gramme", "grammes"),
        MeasureUnit.KILOGRAM to listOf("kg", "kilo", "kilos", "kilogramme", "kilogrammes"),
        MeasureUnit.MILLILITER to listOf("ml", "millilitre", "millilitres"),
        MeasureUnit.CENTILITER to listOf("cl", "centilitre", "centilitres"),
        MeasureUnit.LITER to listOf("l", "litre", "litres"),
        MeasureUnit.TEASPOON to listOf(
            "c. a cafe", "c.a cafe", "c a cafe", "cac", "c.a.c", "c.a.c.", "cc", "cuillere a cafe", "cuilleres a cafe",
        ),
        MeasureUnit.TABLESPOON to listOf(
            "c. a soupe", "c.a soupe", "c a soupe", "cas", "c.a.s", "c.a.s.", "cs", "cuillere a soupe", "cuilleres a soupe",
        ),
        MeasureUnit.PIECE to listOf("piece", "pieces", "pc", "pcs"),
    ).flatMap { (unit, aliases) -> aliases.map { it to unit } }.sortedByDescending { it.first.length }

    /** « 200 g pâtes », « 200g de farine », « 1,5 l lait », « 1/2 c. à café sel », « 2 œufs », « sel ». */
    fun parseIngredient(line: String): RecipeIngredient {
        val match = Regex("""^(\d+(?:[.,]\d+)?(?:\s*/\s*\d+)?)\s*(.*)$""").matchEntire(line)
        if (match == null) {
            require(line.isNotBlank()) { "ligne vide" }
            return RecipeIngredient(Ingredient(name = line), Quantity(1.0, MeasureUnit.PIECE))
        }
        val amount = parseAmount(match.groupValues[1])
        require(amount > 0) { "quantité nulle" }
        var rest = match.groupValues[2].trim()
        val normalizedRest = normalize(rest)
        val alias = UNIT_ALIASES.firstOrNull { (alias, _) ->
            normalizedRest.startsWith(alias) &&
                (normalizedRest.length == alias.length || !normalizedRest[alias.length].isLetterOrDigit())
        }
        val unit = alias?.second ?: MeasureUnit.PIECE
        if (alias != null) rest = rest.substring(originalLength(rest, alias.first.length)).trim()
        rest = rest.replace(Regex("""^(?:de |d'|d’|des )""", RegexOption.IGNORE_CASE), "").trim()
        require(rest.isNotEmpty()) { "nom d'ingrédient manquant" }
        return RecipeIngredient(Ingredient(name = rest), Quantity(amount, unit))
    }

    /** Nombre de caractères de [text] dont la forme normalisée fait [normalizedLength] caractères. */
    private fun originalLength(text: String, normalizedLength: Int): Int =
        (1..text.length).firstOrNull { normalize(text.substring(0, it)).length >= normalizedLength } ?: text.length

    private fun parseAmount(value: String): Double {
        val parts = value.replace(" ", "").split('/')
        val numerator = parts[0].replace(',', '.').toDouble()
        return if (parts.size == 2) numerator / parts[1].toDouble() else numerator
    }

    /** Sans accents ni majuscules ; garde la longueur des caractères latins usuels (é → e). */
    private fun normalize(value: String): String =
        Ingredient.normalizeName(value.replace('’', '\''))
}
