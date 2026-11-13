package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.theme.DlTheme

/**
 * Web .dl-field: the label above (12/700, 0.06 em, caps, quiet), a 48 dp box on the letter surface
 * with a 1 dp line and 8 dp corners (2 dp seal when focused, 2 dp error on error), and the hint or
 * the error underneath (13). [centeredMono] is the key field: centred monospace, XXXX-XXXX-XXXX.
 */
@Composable
fun DlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    maxLength: Int? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    centeredMono: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    /** Shown at the end of the label row ("7/12"). */
    counter: String? = null,
    minLines: Int = 1,
) {
    val isError = errorText != null
    val colors = MaterialTheme.colorScheme
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val textStyle = if (centeredMono) {
        DlTheme.type.key.copy(textAlign = TextAlign.Center, color = colors.onSurface)
    } else {
        TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = colors.onSurface)
    }
    val shape = RoundedCornerShape(8.dp)
    val edge = when {
        isError -> BorderStroke(2.dp, colors.error)
        focused -> BorderStroke(2.dp, colors.primary)
        else -> BorderStroke(1.dp, colors.outlineVariant)
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // web .dl-field-labelrow: the label, and the count at the end when there is one.
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label.uppercase(),
                modifier = Modifier.weight(1f),
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.06.em),
                color = colors.onSurfaceVariant,
                textAlign = if (centeredMono) TextAlign.Center else null,
            )
            if (counter != null) {
                Text(counter, style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontFeatureSettings = "tnum"), color = colors.onSurfaceVariant)
            }
        }
        BasicTextField(
            value = value,
            onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .alpha(if (enabled) 1f else 0.6f)
                .background(colors.surfaceContainerHigh, shape)
                .border(edge, shape)
                .semantics { contentDescription = label },
            enabled = enabled,
            singleLine = singleLine,
            minLines = minLines,
            textStyle = textStyle,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = source,
            cursorBrush = SolidColor(if (isError) colors.error else colors.primary),
            decorationBox = { inner ->
                Box(
                    Modifier.heightIn(min = 48.dp).padding(horizontal = 14.dp, vertical = 13.dp),
                    contentAlignment = if (centeredMono) Alignment.Center else Alignment.CenterStart,
                ) { inner() }
            },
        )
        val hint = errorText ?: supportingText
        if (hint != null) {
            Text(hint, style = TextStyle(fontSize = 13.sp, lineHeight = 18.sp), color = if (isError) colors.error else colors.onSurfaceVariant)
        }
    }
}

/** Keys are typed and shown grouped: "k7qmwx4p2nzd" → "K7QM-WX4P-2NZD" (12 characters). */
fun formatKey(raw: String): String = raw.uppercase().filter { it.isLetterOrDigit() }.take(12).chunked(4).joinToString("-")
