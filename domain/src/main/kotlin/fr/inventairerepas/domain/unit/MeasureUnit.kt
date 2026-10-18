package fr.inventairerepas.domain.unit

/** Famille d'unités : la conversion n'est possible qu'à l'intérieur d'une même famille. */
enum class UnitFamily(val baseUnit: MeasureUnit) {
    MASS(MeasureUnit.GRAM),
    VOLUME(MeasureUnit.MILLILITER),
    COUNT(MeasureUnit.PIECE),
}

/**
 * Unités gérées par l'application.
 *
 * [toBase] est le facteur vers l'unité de base de la famille (g, ml, pièce).
 * Les cuillères sont rattachées au volume : 1 c. à café = 5 ml, 1 c. à soupe = 15 ml.
 */
enum class MeasureUnit(val symbol: String, val toBase: Double) {
    GRAM("g", 1.0),
    KILOGRAM("kg", 1000.0),
    MILLILITER("ml", 1.0),
    CENTILITER("cl", 10.0),
    LITER("l", 1000.0),
    TEASPOON("c. à café", 5.0),
    TABLESPOON("c. à soupe", 15.0),
    PIECE("pièce", 1.0),
    ;

    val family: UnitFamily
        get() = when (this) {
            GRAM, KILOGRAM -> UnitFamily.MASS
            MILLILITER, CENTILITER, LITER, TEASPOON, TABLESPOON -> UnitFamily.VOLUME
            PIECE -> UnitFamily.COUNT
        }
}
