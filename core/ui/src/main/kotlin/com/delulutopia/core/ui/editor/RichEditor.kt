package com.delulutopia.core.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Doc

/**
 * The letter editor's state (web editor/useLetterEditor.ts): the text, its marks and bullet lines,
 * the selection, and marks toggled at a collapsed cursor for the next characters typed.
 */
@Stable
class RichEditorState(initial: Doc, private val onChange: (Doc) -> Unit) {
    var doc by mutableStateOf(RichDoc.of(initial))
        private set
    var value by mutableStateOf(TextFieldValue(doc.text))
        private set
    private var pending by mutableStateOf<Set<MarkType>?>(null)
    val focus = FocusRequester()

    /** The marks the next typed character takes: toggled ones, else those of the character before the cursor. */
    private fun typingAt(offset: Int): Set<MarkType> =
        pending ?: MarkType.entries.filter { offset > 0 && doc.text.getOrNull(offset - 1) != '\n' && doc.has(it, offset - 1) }.toSet()

    fun onValueChange(next: TextFieldValue) {
        if (next.text == value.text) {
            if (next.selection != value.selection) pending = null
            value = next
            return
        }
        val typing = typingAt(minOf(value.selection.min, next.selection.min))
        val edited = doc.edited(next.text, typing)
        doc = edited
        // Leaving a list removed the typed newline: the cursor stays where the empty bullet was.
        val sel = if (edited.text == next.text) next.selection else TextRange((next.selection.min - 1).coerceIn(0, edited.text.length))
        value = TextFieldValue(edited.text, sel, next.composition.takeIf { edited.text == next.text })
        pending = null
        onChange(edited.toDoc())
    }

    fun isActive(type: MarkType): Boolean {
        val s = value.selection
        return if (s.collapsed) type in typingAt(s.start) else doc.covers(type, s.min, s.max)
    }

    val inBullet: Boolean get() = doc.lineOf(value.selection.start) in doc.bullets

    fun toggle(type: MarkType) {
        val s = value.selection
        if (s.collapsed) {
            val now = typingAt(s.start)
            pending = if (type in now) now - type else now + type
            return
        }
        doc = doc.toggled(type, s.min, s.max)
        onChange(doc.toDoc())
    }

    fun toggleBullet() {
        doc = doc.bulletToggled(value.selection.min, value.selection.max)
        onChange(doc.toDoc())
    }

    fun insert(text: String) {
        val s = value.selection
        val next = value.text.replaceRange(s.min, s.max, text)
        onValueChange(TextFieldValue(next, TextRange(s.min + text.length)))
    }

    /** Replace everything (Load in the conflict dialog); no change event. */
    fun set(d: Doc) {
        doc = RichDoc.of(d)
        value = TextFieldValue(doc.text, TextRange(doc.text.length))
        pending = null
    }

    fun current(): Doc = doc.toDoc()
}

@Composable
fun rememberRichEditor(initial: Doc, onChange: (Doc) -> Unit): RichEditorState = remember { RichEditorState(initial, onChange) }

private val BULLET_INDENT = 22.sp

/**
 * The letter text field: the letter's own type on ruled paper, bold/italic/underline as typed,
 * bullet lines indented with a dot. One level of bullets; no hard breaks.
 */
@Composable
fun RichEditor(
    state: RichEditorState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: TextStyle = DlTheme.type.letterBody,
    colour: Color = DlTheme.colors.letterInk,
    label: String = "Letter text",
    minHeight: Dp = 240.dp,
) {
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val doc = state.doc
    val transformation = remember(doc) {
        VisualTransformation { text ->
            TransformedText(styled(doc, text.text), OffsetMapping.Identity)
        }
    }
    val dot = colour
    BasicTextField(
        value = state.value,
        onValueChange = state::onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .focusRequester(state.focus)
            .semantics { contentDescription = label }
            .drawBehind {
                val l = layout ?: return@drawBehind
                val starts = doc.lineStarts()
                doc.bullets.forEach { line ->
                    val offset = starts.getOrNull(line) ?: return@forEach
                    if (offset > l.layoutInput.text.length) return@forEach
                    val row = l.getLineForOffset(offset)
                    val y = (l.getLineTop(row) + l.getLineBottom(row)) / 2
                    drawCircle(dot, 2.6.dp.toPx(), Offset(8.dp.toPx(), y))
                }
            },
        enabled = enabled,
        textStyle = style.copy(color = colour),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        visualTransformation = transformation,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        onTextLayout = { layout = it },
    )
}

private fun styled(doc: RichDoc, text: String): AnnotatedString {
    if (text != doc.text) return AnnotatedString(text)
    val b = AnnotatedString.Builder(text)
    doc.marks.forEach { m ->
        val style = when (m.type) {
            MarkType.Bold -> SpanStyle(fontWeight = FontWeight.Bold)
            MarkType.Italic -> SpanStyle(fontStyle = FontStyle.Italic)
            MarkType.Underline -> SpanStyle(textDecoration = TextDecoration.Underline)
        }
        b.addStyle(style, m.start.coerceIn(0, text.length), m.end.coerceIn(0, text.length))
    }
    val starts = doc.lineStarts()
    doc.bullets.sorted().forEach { line ->
        val start = starts.getOrNull(line) ?: return@forEach
        val end = starts.getOrNull(line + 1) ?: text.length
        if (end > start) b.addStyle(ParagraphStyle(textIndent = TextIndent(BULLET_INDENT, BULLET_INDENT)), start, end)
    }
    return b.toAnnotatedString()
}

/** B, I, U, bullet list, emoji (FS-W2): buttons keep the editor's selection. */
@Composable
fun EditorToolbar(state: RichEditorState, modifier: Modifier = Modifier, enabled: Boolean = true, onEmoji: () -> Unit) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).semantics { contentDescription = "Formatting" },
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tool("Bold", state.isActive(MarkType.Bold), enabled, { state.toggle(MarkType.Bold) }) {
            Text("B", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold))
        }
        Tool("Italic", state.isActive(MarkType.Italic), enabled, { state.toggle(MarkType.Italic) }) {
            Text("I", style = MaterialTheme.typography.titleMedium.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.SemiBold))
        }
        Tool("Underline", state.isActive(MarkType.Underline), enabled, { state.toggle(MarkType.Underline) }) {
            Text("U", style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.Underline, fontWeight = FontWeight.SemiBold))
        }
        Tool("Bullet list", state.inBullet, enabled, state::toggleBullet) { Icon(WebIcons.List, null, Modifier.size(20.dp)) }
        Tool("Emoji", false, enabled, onEmoji) { Icon(WebIcons.Emoji, null, Modifier.size(20.dp)) }
    }
}

@Composable
private fun Tool(label: String, on: Boolean, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    val colours = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (on) colours.primaryContainer else Color.Transparent)
            .then(
                if (enabled) {
                    Modifier.clickableNoFocus(onClick)
                } else {
                    Modifier
                },
            )
            .semantics {
                contentDescription = label
                role = Role.Button
                selected = on
            },
        contentAlignment = Alignment.Center,
    ) {
        CompositionLocalProvider(LocalContentColor provides if (enabled) colours.onSurface else DlTheme.colors.inkDisabled) { content() }
    }
}
