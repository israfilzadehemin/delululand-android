package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.theme.DlTheme

/**
 * The per-list FAB (Y-A2): Letters (pen), Discussions (note +), Emails (envelope +). 56 dp navy
 * circle. [hasDraft] adds the draft dot (10 dp paper dot with a 2 dp navy ring, top-right) and the
 * label becomes "Continue your …" for accessibility.
 */
@Composable
fun DlFab(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasDraft: Boolean = false,
) {
    val press = remember { MutableInteractionSource() }
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.pressScale(press).semantics { this.contentDescription = contentDescription },
        interactionSource = press,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp, pressedElevation = 8.dp),
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            if (hasDraft) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = (-6).dp, y = 6.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                )
            }
        }
    }
}

/** Extended FAB (Z-Components): icon + 15/700 label, e.g. "Continue your draft". */
@Composable
fun DlExtendedFab(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) },
        text = { Text(text, style = DlTheme.type.fabLabel) },
    )
}
