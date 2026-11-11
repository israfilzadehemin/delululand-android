package com.delulutopia.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.theme.DlTheme

/** Caps section label: "FROM LEYLA", "WAITING FOR YOU". 11 sp 800, tracked 0.09 em, quiet ink. */
@Composable
fun DlSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Settings row (web .dl-set-nav): 64 dp, a 22 dp seal icon, label 16/600, sub 13 quiet, a quiet
 * chevron when it opens a page. Lives in a [DlGroupCard], which draws the lines between rows.
 * [destructive] turns label and icon error red (Log out); a disabled row reads quiet (Soon).
 */
@Composable
fun DlListItem(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    enabled: Boolean = true,
    destructive: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 64.dp)
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val content = when {
            destructive -> MaterialTheme.colorScheme.error
            !enabled -> MaterialTheme.colorScheme.onSurfaceVariant
            else -> MaterialTheme.colorScheme.onSurface
        }
        val iconTint = when {
            destructive -> MaterialTheme.colorScheme.error
            !enabled -> DlTheme.colors.inkDisabled
            else -> MaterialTheme.colorScheme.primary
        }
        if (icon != null) Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = content)
            if (supporting != null) {
                Text(supporting, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        when {
            trailing != null -> trailing()
            showChevron && onClick != null -> Icon(
                DlIcons.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/**
 * Web .dl-set-card: the letter surface, a 1 dp line and 10 dp corners, a line between rows.
 * [background] tints the whole card (Pause all when on).
 */
@Composable
fun DlGroupCard(
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    content: @Composable () -> Unit,
) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val shape = RoundedCornerShape(10.dp)
    val tops = remember { IntArray(64) }
    var count by remember { mutableIntStateOf(0) }
    Layout(
        content = content,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(1.dp, line, shape)
            .drawWithContent {
                drawContent()
                for (i in 1 until count) {
                    val y = tops[i].toFloat()
                    drawLine(line, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                }
            },
    ) { measurables, constraints ->
        val loose = constraints.copy(minHeight = 0)
        val placeables = measurables.map { it.measure(loose) }
        val height = placeables.sumOf { it.height }.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(constraints.maxWidth.takeIf { it != Constraints.Infinity } ?: placeables.maxOfOrNull { it.width } ?: 0, height) {
            var y = 0
            placeables.forEachIndexed { i, p ->
                if (i < tops.size) tops[i] = y
                p.place(0, y)
                y += p.height
            }
            count = minOf(placeables.count { it.height > 0 }, tops.size)
        }
    }
}

/** Web .dl-switch: a 48 x 28 track, a 24 dp knob that slides 20 dp; seal when on. */
@Composable
fun DlSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val track by animateColorAsState(
        if (checked) {
            MaterialTheme.colorScheme.primary
        } else if (DlTheme.isDark) {
            Color(0xFF4A4F5E)
        } else {
            MaterialTheme.colorScheme.outlineVariant
        },
        tween(160, easing = LinearEasing),
        label = "switch-track",
    )
    val knobX by animateDpAsState(if (checked) 20.dp else 0.dp, tween(160, easing = Motion.EaseOut), label = "switch-knob")
    val knob = if (checked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.background
    Box(
        modifier
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
                } else {
                    Modifier
                },
            )
            .alpha(if (enabled) 1f else 0.45f)
            .size(48.dp, 28.dp)
            .background(track, RoundedCornerShape(14.dp))
            .padding(2.dp),
    ) {
        Box(
            Modifier
                .offset(x = knobX)
                .size(24.dp)
                .shadow(1.5.dp, CircleShape)
                .background(knob, CircleShape),
        )
    }
}

/** A list item whose whole row toggles an M3 switch (Settings › Notifications, Appearance). */
@Composable
fun DlSwitchItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    enabled: Boolean = true,
    emphasis: Boolean = false,
) {
    // web .dl-set-toggle / .dl-set-pause: 14/16 padding, 12 dp gap; Pause all's label is 15/700.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                fontSize = if (emphasis) 15.sp else 16.sp,
                lineHeight = 22.sp,
                fontWeight = if (emphasis) FontWeight.Bold else FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supporting != null) {
                Text(supporting, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DlSwitch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
