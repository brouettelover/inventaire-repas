package fr.inventairerepas.domain.model

import java.text.Normalizer

/**
 * Ingrédient du catalogue partagé par les recettes, le stock et les courses.
 *
 * [alwaysInStock] : ingrédient de base (sel, poivre, huile…) jamais ajouté aux listes de courses.
 */
data class Ingredient(
    val id: Long = 0,
    val name: String,
    val alwaysInStock: Boolean = false,
) {
    val normalizedName: String get() = normalizeName(name)

    companion object {
        private val DIACRITICS = "\\p{Mn}+".toRegex()
        private val SPACES = "\\s+".toRegex()

        /** Clé d'unicité : minuscules, sans accents, espaces réduits. « Crème fraîche » → « creme fraiche ». */
        fun normalizeName(name: String): String =
            Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
                .replace(DIACRITICS, "")
                .replace(SPACES, " ")
                .lowercase()
    }
}
