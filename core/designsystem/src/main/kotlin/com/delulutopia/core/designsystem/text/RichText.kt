package com.delulutopia.core.designsystem.text

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.delulutopia.core.i18n.fillValue
import com.delulutopia.core.i18n.template

private val PLACEHOLDER = Regex("\\{([A-Za-z]+)\\}")

/**
 * [key]'s sentence in the language in force, with the [styled] placeholders in [style] (web `rich`): the bold
 * name or time sits wherever the language puts it, not where English does.
 */
fun tBold(
    key: String,
    styled: Set<String>,
    vararg args: Pair<String, Any?>,
    style: SpanStyle = SpanStyle(fontWeight = FontWeight.Bold),
): AnnotatedString {
    val values = args.toMap()
    val text = template(key)
    return buildAnnotatedString {
        var at = 0
        for (m in PLACEHOLDER.findAll(text)) {
            append(text.substring(at, m.range.first))
            val name = m.groupValues[1]
            if (!values.containsKey(name)) {
                append(m.value)
            } else if (name in styled) {
                withStyle(style) { append(fillValue(values[name])) }
            } else {
                append(fillValue(values[name]))
            }
            at = m.range.last + 1
        }
        append(text.substring(at))
    }
}
