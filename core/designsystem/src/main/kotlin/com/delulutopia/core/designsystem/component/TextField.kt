package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.theme.DlShape
import com.delulutopia.core.designsystem.theme.DlTheme

/**
 * M3 filled text field (canvas §3): label inside (12/600), active indicator (outline 1 dp, navy 2 dp
 * focused, error 2 dp), supporting text and an `n/max` counter underneath. [centeredMono] is the
 * key field: centred monospace, XXXX-XXXX-XXXX.
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
    /** Shown at the end of the supporting line in place of the length count ("12/16"). */
    counter: String? = null,
    minLines: Int = 1,
) {
    val isError = errorText != null
    val colors = MaterialTheme.colorScheme
    val textStyle = if (centeredMono) {
        DlTheme.type.key.copy(textAlign = TextAlign.Center, color = colors.onSurface)
    } else {
        MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, color = colors.onSurface)
    }
    TextField(
        value = value,
        onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = singleLine,
        minLines = minLines,
        textStyle = textStyle,
        isError = isError,
        shape = DlShape.textField,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        label = {
            Text(
                label,
                style = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold),
                textAlign = if (centeredMono) TextAlign.Center else null,
                modifier = if (centeredMono) Modifier.fillMaxWidth() else Modifier,
            )
        },
        supportingText = if (supportingText != null || errorText != null || maxLength != null || counter != null) {
            {
                Row(Modifier.fillMaxWidth()) {
                    Text(errorText ?: supportingText.orEmpty(), modifier = Modifier.weight(1f))
                    val count = counter ?: maxLength?.let { "${value.length}/$it" }
                    if (count != null) Text(count, style = TextStyle(fontFeatureSettings = "tnum"))
                }
            }
        } else {
            null
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHigh,
            unfocusedContainerColor = colors.surfaceContainerHigh,
            disabledContainerColor = colors.surfaceContainerHigh,
            errorContainerColor = colors.surfaceContainerHigh,
            focusedIndicatorColor = colors.primary,
            unfocusedIndicatorColor = colors.outline,
            errorIndicatorColor = colors.error,
            focusedLabelColor = colors.primary,
            unfocusedLabelColor = colors.onSurfaceVariant,
            errorLabelColor = colors.error,
            focusedSupportingTextColor = colors.onSurfaceVariant,
            unfocusedSupportingTextColor = colors.onSurfaceVariant,
            errorSupportingTextColor = colors.error,
            cursorColor = colors.primary,
            errorCursorColor = colors.error,
        ),
    )
}

/** Keys are typed and shown grouped: "k7qmwx4p2nzd" → "K7QM-WX4P-2NZD" (12 characters). */
fun formatKey(raw: String): String = raw.uppercase().filter { it.isLetterOrDigit() }.take(12).chunked(4).joinToString("-")
