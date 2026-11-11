package com.delulutopia.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.BulletList
import com.delulutopia.core.network.model.Doc
import com.delulutopia.core.network.model.Paragraph
import com.delulutopia.core.network.model.TextNode

/** The whole letter takes at most this long to write itself onto the page (web text/LetterBody.tsx). */
const val REVEAL_MS = 2_600
private const val STEP_MAX_MS = 45

/**
 * The one renderer for letter JSON (Technical Design §6.4): paragraphs, bullet lists, bold, italic,
 * underline. Each paragraph takes the direction of its first strong character (the platform does
 * this for plain text). An empty paragraph keeps a short gap, not a full empty line. [reveal] writes
 * the letter onto the page word by word: every word is laid out from the start, only its ink fades in.
 */
@Composable
fun LetterBody(
    doc: Doc,
    modifier: Modifier = Modifier,
    style: TextStyle = DlTheme.type.letterBody,
    colour: Color = DlTheme.colors.letterInk,
    reveal: Boolean = false,
) {
    val words = remember(doc) { countWords(doc) }
    val step = minOf(STEP_MAX_MS.toFloat(), REVEAL_MS.toFloat() / maxOf(1, words))
    val ink = remember(doc, reveal) { Animatable(if (reveal) 0f else words.toFloat()) }
    LaunchedEffect(doc, reveal) {
        if (reveal) ink.animateTo(words.toFloat(), tween((step * words).toInt() + 300, easing = LinearEasing)) else ink.snapTo(words.toFloat())
    }
    val shown = ink.value
    var index = 0
    Column(modifier) {
        doc.content.forEach { block ->
            when (block) {
                is Paragraph -> {
                    val (text, n) = paragraphText(block, index, shown, colour)
                    index += n
                    if (block.content.isNullOrEmpty()) Spacer(Modifier.height(style.lineHeight.value.dp * 0.45f)) else Text(text, style = style, color = colour)
                }
                is BulletList -> Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    block.content.forEach { item ->
                        Row {
                            Text("•", style = style, color = colour, modifier = Modifier.padding(start = 6.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                item.content.filterIsInstance<Paragraph>().forEach { p ->
                                    val (text, n) = paragraphText(p, index, shown, colour)
                                    index += n
                                    Text(text, style = style, color = colour)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun countWords(doc: Doc): Int {
    fun para(p: Paragraph) = p.content.orEmpty().sumOf { t -> t.text.split(Regex("\\s+")).count { it.isNotEmpty() } }
    return doc.content.sumOf { b ->
        when (b) {
            is Paragraph -> para(b)
            is BulletList -> b.content.sumOf { li -> li.content.filterIsInstance<Paragraph>().sumOf(::para) }
        }
    }
}

/** A paragraph as styled text; words past [shown] (a word index) are transparent ink. Returns its word count. */
private fun paragraphText(p: Paragraph, start: Int, shown: Float, colour: Color): Pair<AnnotatedString, Int> {
    var i = start
    val text = buildAnnotatedString {
        p.content.orEmpty().forEach { t: TextNode ->
            val style = SpanStyle(
                fontWeight = if (t.has("bold")) FontWeight.Bold else null,
                fontStyle = if (t.has("italic")) FontStyle.Italic else null,
                textDecoration = if (t.has("underline")) TextDecoration.Underline else null,
            )
            withStyle(style) {
                Regex("(\\s+)|(\\S+)").findAll(t.text).forEach { m ->
                    if (m.groupValues[1].isNotEmpty()) {
                        append(m.value)
                    } else {
                        val alpha = (shown - i).coerceIn(0f, 1f)
                        i++
                        if (alpha >= 1f) append(m.value) else withStyle(SpanStyle(color = colour.copy(alpha = colour.alpha * alpha))) { append(m.value) }
                    }
                }
            }
        }
    }
    return text to (i - start)
}
