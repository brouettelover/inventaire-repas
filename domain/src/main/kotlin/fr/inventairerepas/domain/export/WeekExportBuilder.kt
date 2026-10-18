package fr.inventairerepas.domain.export

import fr.inventairerepas.domain.model.DayPlan
import fr.inventairerepas.domain.model.ShoppingList
import fr.inventairerepas.domain.portion.PortionScaler
import fr.inventairerepas.domain.unit.QuantityFormatter
import fr.inventairerepas.domain.unit.UnitConverter
import fr.inventairerepas.domain.week.WeekStructure
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Construit le contenu exporté d'une semaine : menu, deux listes de courses, détail optionnel des recettes. */
object WeekExportBuilder {

    private val dayFormat = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.FRENCH)
    private val titleFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.FRENCH)

    fun title(weekStart: LocalDate): String =
        "Menu du ${weekStart.format(titleFormat)} au ${weekStart.plusDays(6).format(titleFormat)}"

    fun build(
        weekStart: LocalDate,
        days: List<DayPlan>,
        shoppingLists: List<ShoppingList>,
        includeRecipeDetails: Boolean,
    ): ExportDocument {
        val byDate = days.associateBy { it.date }
        val slots = WeekStructure.slots(weekStart)
        val blocks = mutableListOf<ExportBlock>()
        val title = title(weekStart)
        blocks += ExportBlock.Title(title)

        blocks += ExportBlock.Heading("Menu de la semaine")
        slots.forEach { slot ->
            val day = byDate[slot.date]
            val label = slot.date.dayLabel() + if (slot.isShoppingDay) " (courses)" else ""
            blocks += ExportBlock.Bullet("$label : ${describe(day)}")
        }

        shoppingLists.sortedBy { it.blockIndex }.forEach { list ->
            blocks += ExportBlock.Heading("Courses du ${list.shoppingDate.dayLabel()}")
            if (list.items.isEmpty()) {
                blocks += ExportBlock.Paragraph("Rien à acheter : le stock couvre tout.")
            }
            list.items.forEach { item ->
                blocks += ExportBlock.Checkbox(
                    "${item.ingredient.name} : ${QuantityFormatter.format(item.quantity)}",
                    item.checked,
                )
            }
        }

        if (includeRecipeDetails) {
            blocks += ExportBlock.Heading("Recettes de la semaine")
            slots.mapNotNull { byDate[it.date] }
                .filter { !it.noMeal && it.recipe != null }
                .forEach { day ->
                    val recipe = day.recipe!!
                    blocks += ExportBlock.Subheading("${day.date.dayLabel()} : ${recipe.name}")
                    blocks += ExportBlock.Paragraph(
                        "Pour ${day.servings} pers. · préparation ${recipe.prepMinutes} min · " +
                            "cuisson ${recipe.cookMinutes} min · total ${recipe.totalMinutes} min",
                    )
                    PortionScaler.scale(recipe, day.servings).forEach {
                        blocks += ExportBlock.Bullet(
                            "${it.ingredient.name} : ${QuantityFormatter.format(UnitConverter.readable(it.quantity))}",
                        )
                    }
                    recipe.steps.forEach { blocks += ExportBlock.Numbered(it) }
                }
        }
        return ExportDocument(title, blocks)
    }

    private fun describe(day: DayPlan?): String {
        val recipe = day?.recipe
        return when {
            day == null -> "non planifié"
            day.noMeal -> "sans repas"
            recipe == null -> "aucune recette"
            else -> "${recipe.name} (${day.servings} pers., ${recipe.totalMinutes} min)"
        }
    }

    private fun LocalDate.dayLabel(): String =
        format(dayFormat).replaceFirstChar { it.titlecase(Locale.FRENCH) }
}
