package fr.inventairerepas.domain.model

/** Liste de fraîcheur : nombre de jours entre les courses et le moment où la recette doit être cuisinée. */
enum class Freshness(val daysAfterShopping: Int, val label: String) {
    D0(0, "J+0"),
    D1(1, "J+1"),
    D2(2, "J+2"),
}
