package fr.inventairerepas.domain.export

/** Bloc d'un document exporté, indépendant du format de sortie (Google Docs ou texte). */
sealed interface ExportBlock {
    val text: String

    data class Title(override val text: String) : ExportBlock
    data class Heading(override val text: String) : ExportBlock
    data class Subheading(override val text: String) : ExportBlock
    data class Paragraph(override val text: String) : ExportBlock
    data class Bullet(override val text: String) : ExportBlock
    data class Numbered(override val text: String) : ExportBlock
    data class Checkbox(override val text: String, val checked: Boolean) : ExportBlock
}

data class ExportDocument(val title: String, val blocks: List<ExportBlock>) {

    /** Version texte, pour le partage via le menu de partage Android. */
    fun toPlainText(): String = buildString {
        var number = 0
        blocks.forEachIndexed { index, block ->
            if (block !is ExportBlock.Numbered) number = 0
            val previous = blocks.getOrNull(index - 1)
            val isHeading = block is ExportBlock.Title || block is ExportBlock.Heading || block is ExportBlock.Subheading
            if (isHeading && previous != null) append('\n')
            when (block) {
                is ExportBlock.Title -> append(block.text.uppercase())
                is ExportBlock.Heading -> append("== ${block.text} ==")
                is ExportBlock.Subheading -> append("-- ${block.text} --")
                is ExportBlock.Paragraph -> append(block.text)
                is ExportBlock.Bullet -> append("• ${block.text}")
                is ExportBlock.Numbered -> append("${++number}. ${block.text}")
                is ExportBlock.Checkbox -> append(if (block.checked) "☑ " else "☐ ").append(block.text)
            }
            append('\n')
        }
    }.trimEnd()
}
