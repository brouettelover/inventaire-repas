package fr.inventairerepas.domain.sample

import fr.inventairerepas.domain.model.Freshness
import fr.inventairerepas.domain.model.Ingredient
import fr.inventairerepas.domain.model.Recipe
import fr.inventairerepas.domain.model.RecipeIngredient
import fr.inventairerepas.domain.model.Seasonality
import fr.inventairerepas.domain.unit.MeasureUnit
import fr.inventairerepas.domain.unit.MeasureUnit.CENTILITER
import fr.inventairerepas.domain.unit.MeasureUnit.GRAM
import fr.inventairerepas.domain.unit.MeasureUnit.KILOGRAM
import fr.inventairerepas.domain.unit.MeasureUnit.LITER
import fr.inventairerepas.domain.unit.MeasureUnit.MILLILITER
import fr.inventairerepas.domain.unit.MeasureUnit.PIECE
import fr.inventairerepas.domain.unit.MeasureUnit.TABLESPOON
import fr.inventairerepas.domain.unit.MeasureUnit.TEASPOON
import fr.inventairerepas.domain.unit.Quantity
import java.time.Month.APRIL
import java.time.Month.FEBRUARY
import java.time.Month.JANUARY
import java.time.Month.JUNE
import java.time.Month.MARCH
import java.time.Month.MAY
import java.time.Month.NOVEMBER
import java.time.Month.OCTOBER
import java.time.Month.SEPTEMBER

/**
 * Recettes d'exemple insérées au premier lancement, pour tester l'application tout de suite.
 * Les ingrédients n'ont pas d'identifiant : ils sont rapprochés du catalogue par leur nom normalisé.
 */
object SampleRecipes {

    private val salt = Ingredient(name = "Sel", alwaysInStock = true)
    private val pepper = Ingredient(name = "Poivre", alwaysInStock = true)
    private val oliveOil = Ingredient(name = "Huile d'olive", alwaysInStock = true)

    private fun i(name: String) = Ingredient(name = name)

    private fun line(ingredient: Ingredient, amount: Double, unit: MeasureUnit) =
        RecipeIngredient(ingredient, Quantity(amount, unit))

    private fun line(name: String, amount: Double, unit: MeasureUnit) = line(i(name), amount, unit)

    val all: List<Recipe> = listOf(
        Recipe(
            name = "Saumon grillé, riz et brocolis",
            baseServings = 2, prepMinutes = 10, cookMinutes = 15,
            seasonality = Seasonality.ALL_YEAR, freshness = Freshness.D0,
            ingredients = listOf(
                line("Pavé de saumon", 2.0, PIECE),
                line("Riz basmati", 150.0, GRAM),
                line("Brocoli", 1.0, PIECE),
                line("Citron", 1.0, PIECE),
                line(oliveOil, 1.0, TABLESPOON),
                line(salt, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Cuire le riz selon les indications du paquet.",
                "Détailler le brocoli en fleurettes et le cuire 6 minutes à la vapeur.",
                "Saisir les pavés de saumon 4 minutes de chaque côté dans l'huile.",
                "Servir avec un filet de citron.",
            ),
        ),
        Recipe(
            name = "Salade niçoise",
            baseServings = 2, prepMinutes = 20, cookMinutes = 10,
            seasonality = Seasonality.range(MAY, SEPTEMBER), freshness = Freshness.D0,
            ingredients = listOf(
                line("Salade verte", 1.0, PIECE),
                line("Tomate", 3.0, PIECE),
                line("Œuf", 2.0, PIECE),
                line("Thon au naturel", 140.0, GRAM),
                line("Haricots verts", 150.0, GRAM),
                line("Olives noires", 50.0, GRAM),
                line(oliveOil, 3.0, TABLESPOON),
            ),
            steps = listOf(
                "Cuire les œufs 10 minutes, les écaler et les couper en quartiers.",
                "Cuire les haricots verts 8 minutes à l'eau salée puis les refroidir.",
                "Laver la salade, couper les tomates.",
                "Assembler tous les ingrédients et assaisonner d'huile d'olive.",
            ),
        ),
        Recipe(
            name = "Moules marinières",
            baseServings = 2, prepMinutes = 15, cookMinutes = 10,
            seasonality = Seasonality.range(SEPTEMBER, FEBRUARY), freshness = Freshness.D0,
            ingredients = listOf(
                line("Moules", 1.5, KILOGRAM),
                line("Échalote", 2.0, PIECE),
                line("Vin blanc", 20.0, CENTILITER),
                line("Persil", 1.0, PIECE),
                line("Beurre", 20.0, GRAM),
                line(pepper, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Gratter et rincer les moules.",
                "Faire suer les échalotes émincées dans le beurre.",
                "Ajouter le vin blanc et les moules, couvrir et cuire 8 minutes en remuant.",
                "Parsemer de persil ciselé et servir aussitôt.",
            ),
        ),
        Recipe(
            name = "Poulet rôti aux légumes",
            baseServings = 4, prepMinutes = 15, cookMinutes = 60,
            seasonality = Seasonality.ALL_YEAR, freshness = Freshness.D1,
            ingredients = listOf(
                line("Poulet entier", 1.0, PIECE),
                line("Pomme de terre", 800.0, GRAM),
                line("Carotte", 4.0, PIECE),
                line("Oignon", 2.0, PIECE),
                line("Ail", 4.0, PIECE),
                line(oliveOil, 2.0, TABLESPOON),
                line(salt, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Préchauffer le four à 200 °C.",
                "Éplucher et couper les légumes en morceaux, les disposer dans un plat avec l'ail.",
                "Poser le poulet dessus, arroser d'huile, saler.",
                "Enfourner 1 heure en arrosant à mi-cuisson.",
            ),
        ),
        Recipe(
            name = "Taboulé libanais",
            baseServings = 4, prepMinutes = 25, cookMinutes = 0,
            seasonality = Seasonality.range(MAY, SEPTEMBER), freshness = Freshness.D1,
            ingredients = listOf(
                line("Boulgour", 100.0, GRAM),
                line("Persil", 2.0, PIECE),
                line("Menthe", 1.0, PIECE),
                line("Tomate", 3.0, PIECE),
                line("Citron", 2.0, PIECE),
                line("Oignon", 1.0, PIECE),
                line(oliveOil, 4.0, TABLESPOON),
            ),
            steps = listOf(
                "Faire gonfler le boulgour 15 minutes dans de l'eau froide, puis l'égoutter.",
                "Ciseler finement le persil, la menthe et l'oignon ; couper les tomates en dés.",
                "Mélanger avec le jus des citrons et l'huile d'olive.",
                "Réserver au frais avant de servir.",
            ),
        ),
        Recipe(
            name = "Risotto aux champignons",
            baseServings = 2, prepMinutes = 10, cookMinutes = 30,
            seasonality = Seasonality.range(SEPTEMBER, NOVEMBER), freshness = Freshness.D1,
            ingredients = listOf(
                line("Riz arborio", 160.0, GRAM),
                line("Champignons de Paris", 250.0, GRAM),
                line("Oignon", 1.0, PIECE),
                line("Bouillon de légumes", 75.0, CENTILITER),
                line("Parmesan", 40.0, GRAM),
                line("Vin blanc", 10.0, CENTILITER),
                line("Beurre", 20.0, GRAM),
            ),
            steps = listOf(
                "Faire revenir l'oignon émincé dans le beurre, ajouter le riz et le nacrer 2 minutes.",
                "Déglacer au vin blanc.",
                "Ajouter le bouillon chaud louche par louche en remuant, pendant 18 minutes.",
                "Incorporer les champignons poêlés et le parmesan râpé hors du feu.",
            ),
        ),
        Recipe(
            name = "Dahl de lentilles corail",
            baseServings = 4, prepMinutes = 10, cookMinutes = 25,
            seasonality = Seasonality.ALL_YEAR, freshness = Freshness.D2,
            ingredients = listOf(
                line("Lentilles corail", 250.0, GRAM),
                line("Lait de coco", 400.0, MILLILITER),
                line("Tomates concassées", 400.0, GRAM),
                line("Oignon", 1.0, PIECE),
                line("Ail", 2.0, PIECE),
                line("Curry en poudre", 2.0, TEASPOON),
                line(oliveOil, 1.0, TABLESPOON),
            ),
            steps = listOf(
                "Faire revenir l'oignon et l'ail émincés dans l'huile avec le curry.",
                "Ajouter les lentilles rincées, les tomates et le lait de coco.",
                "Laisser mijoter 20 minutes en remuant, jusqu'à ce que les lentilles soient fondantes.",
            ),
        ),
        Recipe(
            name = "Soupe de potiron",
            baseServings = 4, prepMinutes = 15, cookMinutes = 30,
            seasonality = Seasonality.range(OCTOBER, JANUARY), freshness = Freshness.D2,
            ingredients = listOf(
                line("Potiron", 1.2, KILOGRAM),
                line("Pomme de terre", 2.0, PIECE),
                line("Oignon", 1.0, PIECE),
                line("Bouillon de légumes", 1.0, LITER),
                line("Crème fraîche", 10.0, CENTILITER),
                line(salt, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Éplucher et couper le potiron, les pommes de terre et l'oignon.",
                "Cuire 25 minutes dans le bouillon.",
                "Mixer, ajouter la crème et rectifier l'assaisonnement.",
            ),
        ),
        Recipe(
            name = "Chili con carne",
            baseServings = 4, prepMinutes = 15, cookMinutes = 60,
            seasonality = Seasonality.range(OCTOBER, MARCH), freshness = Freshness.D2,
            ingredients = listOf(
                line("Bœuf haché", 500.0, GRAM),
                line("Haricots rouges", 400.0, GRAM),
                line("Tomates concassées", 800.0, GRAM),
                line("Oignon", 2.0, PIECE),
                line("Poivron rouge", 1.0, PIECE),
                line("Cumin", 1.0, TEASPOON),
                line(oliveOil, 2.0, TABLESPOON),
            ),
            steps = listOf(
                "Faire revenir les oignons et le poivron en dés dans l'huile.",
                "Ajouter la viande et la faire dorer, puis le cumin.",
                "Ajouter les tomates et laisser mijoter 45 minutes.",
                "Ajouter les haricots rouges égouttés 10 minutes avant la fin.",
            ),
        ),
        Recipe(
            name = "Pâtes à la carbonara",
            baseServings = 2, prepMinutes = 10, cookMinutes = 12,
            seasonality = Seasonality.ALL_YEAR, freshness = Freshness.D2,
            ingredients = listOf(
                line("Spaghetti", 200.0, GRAM),
                line("Lardons", 150.0, GRAM),
                line("Œuf", 3.0, PIECE),
                line("Parmesan", 50.0, GRAM),
                line(pepper, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Cuire les pâtes al dente.",
                "Faire dorer les lardons à sec.",
                "Battre les œufs avec le parmesan râpé et le poivre.",
                "Hors du feu, mélanger les pâtes, les lardons et les œufs avec un peu d'eau de cuisson.",
            ),
        ),
        Recipe(
            name = "Gratin dauphinois",
            baseServings = 4, prepMinutes = 20, cookMinutes = 75,
            seasonality = Seasonality.range(OCTOBER, MARCH), freshness = Freshness.D2,
            ingredients = listOf(
                line("Pomme de terre", 1.0, KILOGRAM),
                line("Lait", 50.0, CENTILITER),
                line("Crème fraîche", 20.0, CENTILITER),
                line("Ail", 1.0, PIECE),
                line(salt, 1.0, TEASPOON),
            ),
            steps = listOf(
                "Préchauffer le four à 160 °C et frotter le plat avec l'ail.",
                "Éplucher les pommes de terre et les couper en fines rondelles.",
                "Les disposer dans le plat, couvrir du mélange lait et crème, saler.",
                "Enfourner 1 h 15, jusqu'à ce que le dessus soit doré.",
            ),
        ),
        Recipe(
            name = "Asperges, œuf mollet et jambon",
            baseServings = 2, prepMinutes = 10, cookMinutes = 15,
            seasonality = Seasonality.range(APRIL, JUNE), freshness = Freshness.D0,
            ingredients = listOf(
                line("Asperges vertes", 500.0, GRAM),
                line("Œuf", 2.0, PIECE),
                line("Jambon cru", 4.0, PIECE),
                line(oliveOil, 1.0, TABLESPOON),
            ),
            steps = listOf(
                "Cuire les asperges 10 minutes à l'eau bouillante salée.",
                "Cuire les œufs 6 minutes, les rafraîchir et les écaler.",
                "Servir les asperges avec l'œuf mollet, le jambon et un filet d'huile.",
            ),
        ),
    )
}
