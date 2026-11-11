package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.theme.DlShape
import com.delulutopia.core.designsystem.theme.DlTheme

/*
 * Buttons (Z-Components): 40 dp tall (48–52 dp for primary actions via [height]), 20 dp corners,
 * Manrope 14/700. The touch target is at least 48 dp (P11) through Material's minimum interactive size.
 */

private val ButtonHeight = 40.dp
private val ContentPadding = PaddingValues(horizontal = 24.dp)
private val TextContentPadding = PaddingValues(horizontal = 12.dp)

/** Navy fill, white label: Send, Save, Enter. */
@Composable
fun DlFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    height: Dp = ButtonHeight,
) {
    val press = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(press, 0.97f).defaultMinSize(minHeight = height),
        interactionSource = press,
        enabled = enabled,
        shape = DlShape.button,
        contentPadding = ContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContentColor = DlTheme.colors.inkDisabled,
        ),
    ) { ButtonContent(text, icon) }
}

/** Lavender fill, navy label: Draw. */
@Composable
fun DlTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val press = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        modifier = modifier.pressScale(press, 0.97f).defaultMinSize(minHeight = ButtonHeight),
        interactionSource = press,
        enabled = enabled,
        shape = DlShape.button,
        contentPadding = ContentPadding,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContentColor = DlTheme.colors.inkDisabled,
        ),
    ) { ButtonContent(text, icon) }
}

/** Outline border, navy label: Edit, Remove reaction, Unarchive. */
@Composable
fun DlOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val press = remember { MutableInteractionSource() }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.pressScale(press).defaultMinSize(minHeight = ButtonHeight),
        interactionSource = press,
        enabled = enabled,
        shape = DlShape.button,
        contentPadding = ContentPadding,
        border = BorderStroke(1.dp, if (enabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary,
            disabledContentColor = DlTheme.colors.inkDisabled,
        ),
    ) { ButtonContent(text, icon) }
}

/** Text only: Keep writing, Cancel; [destructive] turns it error red (Log out, Delete). */
@Composable
fun DlTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false,
    enabled: Boolean = true,
) {
    val press = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        modifier = modifier.pressScale(press).defaultMinSize(minHeight = ButtonHeight),
        interactionSource = press,
        enabled = enabled,
        shape = DlShape.button,
        contentPadding = TextContentPadding,
        colors = ButtonDefaults.textButtonColors(
            contentColor = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            disabledContentColor = DlTheme.colors.inkDisabled,
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

@Composable
private fun ButtonContent(text: String, icon: ImageVector?) {
    if (icon != null) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text, style = MaterialTheme.typography.labelLarge)
}
