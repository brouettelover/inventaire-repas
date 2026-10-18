package fr.inventairerepas.domain.export

/**
 * Traduction d'un [ExportDocument] en opérations pour l'API Google Docs, sans dépendance réseau :
 * un seul texte inséré à l'index 1, puis des styles appliqués sur des plages de cet index.
 * Les index de l'API Docs comptent en unités UTF-16, comme les chaînes Kotlin.
 */
data class DocsRequestPlan(
    val text: String,
    val paragraphStyles: List<StyledRange>,
    val bullets: List<BulletRange>,
) {
    data class StyledRange(val start: Int, val end: Int, val namedStyle: String)

    data class BulletRange(val start: Int, val end: Int, val preset: String)

    companion object {
        const val TITLE = "TITLE"
        const val HEADING_1 = "HEADING_1"
        const val HEADING_2 = "HEADING_2"
        const val BULLET_DISC = "BULLET_DISC_CIRCLE_SQUARE"
        const val NUMBERED = "NUMBERED_DECIMAL_ALPHA_ROMAN"
        const val CHECKBOX = "BULLET_CHECKBOX"

        /** Premier index du corps d'un document Google Docs vide. */
        private const val BODY_START = 1

        fun from(document: ExportDocument): DocsRequestPlan {
            val text = StringBuilder()
            val styles = mutableListOf<StyledRange>()
            val bullets = mutableListOf<BulletRange>()
            document.blocks.forEach { block ->
                val start = BODY_START + text.length
                // Une case cochée est marquée dans le texte : l'API ne permet pas de cocher à la création.
                val content = if (block is ExportBlock.Checkbox && block.checked) "${block.text} ✓" else block.text
                text.append(content).append('\n')
                val end = BODY_START + text.length
                when (block) {
                    is ExportBlock.Title -> styles += StyledRange(start, end, TITLE)
                    is ExportBlock.Heading -> styles += StyledRange(start, end, HEADING_1)
                    is ExportBlock.Subheading -> styles += StyledRange(start, end, HEADING_2)
                    is ExportBlock.Bullet -> bullets.addOrExtend(start, end, BULLET_DISC)
                    is ExportBlock.Numbered -> bullets.addOrExtend(start, end, NUMBERED)
                    is ExportBlock.Checkbox -> bullets.addOrExtend(start, end, CHECKBOX)
                    is ExportBlock.Paragraph -> Unit
                }
            }
            return DocsRequestPlan(text.toString(), styles, bullets)
        }

        /** Les paragraphes consécutifs d'un même type de liste forment une seule liste. */
        private fun MutableList<BulletRange>.addOrExtend(start: Int, end: Int, preset: String) {
            val last = lastOrNull()
            if (last != null && last.end == start && last.preset == preset) {
                this[lastIndex] = last.copy(end = end)
            } else {
                add(BulletRange(start, end, preset))
            }
        }
    }
}
