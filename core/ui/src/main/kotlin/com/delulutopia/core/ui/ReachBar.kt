package com.delulutopia.core.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.theme.DlTheme

/**
 * The bar above a sent letter or discussion still in reach (web .dl-reach-bar): the seal tint with
 * its line, 12 dp corners, 10/12 padding, the count 13/600 and the actions at the end. Drops in.
 */
@Composable
fun ReachBar(text: String, modifier: Modifier = Modifier, actions: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val line = if (DlTheme.isDark) Color(0xFF4B4670) else Color(0xFFC9BFE0)
    Row(
        modifier
            .enter(Enter.Drop, durationMs = 280)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, shape)
            .border(1.dp, line, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text,
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = MaterialTheme.colorScheme.onSurface,
        )
        actions()
    }
}

/** Edit and Cancel in reach (web .dl-outline-btn): 36 dp, navy outline on the letter surface, 13/600. */
@Composable
fun ReachButton(text: String, modifier: Modifier = Modifier, padded: Boolean = false, onClick: () -> Unit) {
    val seal = MaterialTheme.colorScheme.primary
    val press = remember { MutableInteractionSource() }
    Box(
        modifier
            .pressScale(press)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DlTheme.colors.letterEnvelope)
            .border(1.dp, seal, RoundedCornerShape(18.dp))
            .clickable(interactionSource = press, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .then(if (padded) Modifier.padding(horizontal = 14.dp) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold), color = seal)
    }
}

/** web .dl-write-head--editing: the seal tint under a header while composing or editing, a line below. */
@Composable
fun Modifier.editingHead(on: Boolean = true): Modifier {
    if (!on) return this
    val tint = MaterialTheme.colorScheme.primaryContainer
    val line = if (DlTheme.isDark) Color(0xFF4B4670) else Color(0xFFC9BFE0)
    return this.background(tint).drawBehind { drawLine(line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1.dp.toPx()) }
}
