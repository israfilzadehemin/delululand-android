package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/*
 * Navigation bar (Y-A1): Home · Letters · Discussions · Emails · You. Paper background with a 1 dp
 * line on top, 80 dp tall above the gesture inset, a 56 × 32 dp lavender pill behind the active
 * icon, labels 12 sp in normal case (700 active, 600 otherwise). An 8 dp navy dot marks new items.
 * The You destination shows the user's picture at 24 dp; while notifications are off on this device
 * it carries a 9 dp red dot.
 */

private val BarHeight = 80.dp
private val PillWidth = 56.dp
private val PillHeight = 32.dp

@Composable
fun DlNavigationBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .fillMaxWidth()
                .height(BarHeight)
                .selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            content = content,
        )
    }
}

/** A destination with a line icon; [hasNew] draws the navy dot at the pill's top-right. */
@Composable
fun RowScope.DlNavigationItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    activeIcon: ImageVector,
    modifier: Modifier = Modifier,
    hasNew: Boolean = false,
    newDescription: String? = null,
) {
    NavItemFrame(selected, onClick, label, modifier, newDescription.takeIf { hasNew }) { tint ->
        Box {
            Icon(if (selected) activeIcon else icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        if (hasNew) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-16).dp, y = 2.dp)
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        }
    }
}

/**
 * The You destination: [avatar] is the user's picture (usually [DlAvatar] at 24 dp). Unselected it
 * has a 1 dp line ring; selected, the pill plus a 2 dp navy ring. [alert] draws the red dot used
 * while notifications are off on this device.
 */
@Composable
fun RowScope.DlNavigationYouItem(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    alert: Boolean = false,
    alertDescription: String? = null,
    avatar: @Composable () -> Unit,
) {
    NavItemFrame(selected, onClick, label, modifier, alertDescription.takeIf { alert }) { _ ->
        val ring = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        val ringWidth = if (selected) 2.dp else 1.dp
        Box {
            Box(
                Modifier
                    .size(24.dp + ringWidth * 2)
                    .border(ringWidth, ring, CircleShape)
                    .padding(ringWidth),
                contentAlignment = Alignment.Center,
            ) { avatar() }
            if (alert) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 5.dp, y = (-3).dp)
                        .size(13.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .padding(2.dp)
                        .background(MaterialTheme.colorScheme.error, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun RowScope.NavItemFrame(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier,
    extraDescription: String?,
    pill: @Composable BoxScope.(tint: Color) -> Unit,
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .weight(1f)
            .height(BarHeight)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.Tab,
                interactionSource = null,
                indication = ripple(bounded = false, radius = 40.dp),
            )
            .semantics { if (extraDescription != null) contentDescription = "$label, $extraDescription" }
            .padding(top = 12.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            Modifier
                .width(PillWidth)
                .height(PillHeight)
                .background(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) { pill(tint) }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            color = tint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
        )
    }
}
