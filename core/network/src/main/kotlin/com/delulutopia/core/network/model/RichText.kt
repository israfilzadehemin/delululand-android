package com.delulutopia.core.network.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/*
 * The letter format (Technical Design §4.3, O5; web src/text/richText.ts): a ProseMirror document with
 * exactly paragraphs, bullet lists, bold, italic and underline. The server validates the same grammar.
 */

@Serializable
data class Doc(val type: String = "doc", val content: List<Block> = listOf(Paragraph())) {
    companion object {
        val EMPTY = Doc()

        fun of(vararg paragraphs: String): Doc = Doc(content = paragraphs.map { Paragraph.of(it) })
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface Block

@Serializable
@SerialName("paragraph")
data class Paragraph(val content: List<TextNode>? = null) : Block {
    val text: String get() = content.orEmpty().joinToString("") { it.text }

    companion object {
        fun of(text: String) = Paragraph(if (text.isEmpty()) null else listOf(TextNode(text = text)))
    }
}

/** Only paragraphs inside (§4.3); typed as [Block] so each carries its `"type"`. */
@Serializable
@SerialName("bulletList")
data class BulletList(val content: List<ListItem>) : Block

@Serializable
data class ListItem(val type: String = "listItem", val content: List<Block>)

@Serializable
data class TextNode(val type: String = "text", val text: String, val marks: List<Mark>? = null) {
    fun has(mark: String) = marks?.any { it.type == mark } == true
}

@Serializable
data class Mark(val type: String) {
    companion object {
        const val BOLD = "bold"
        const val ITALIC = "italic"
        const val UNDERLINE = "underline"
    }
}

private fun Block.texts(): List<String> = when (this) {
    is Paragraph -> listOf(text)
    is BulletList -> content.flatMap { li -> li.content.filterIsInstance<Paragraph>().map { it.text } }
}

/** FS-W11, AT-20: empty means no visible character anywhere (whitespace does not count). */
fun Doc?.isEmptyDoc(): Boolean = this == null || content.all { b -> b.texts().all { it.isBlank() } }

/** The plain text, paragraphs joined by newlines (previews, the reference panel's first line). */
fun Doc.plainText(): String = content.flatMap { it.texts() }.joinToString("\n")
