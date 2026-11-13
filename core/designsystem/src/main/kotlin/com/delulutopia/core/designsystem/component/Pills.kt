package com.delulutopia.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.theme.DlTheme

/** Wax-seal pill (tertiary): "NEW", "2 new" on letters and the board. 22 dp, 11 sp 800, tracked. */
@Composable
fun DlNewPill(text: String, modifier: Modifier = Modifier) {
    Pill(text, DlTheme.colors.newBadge, DlTheme.colors.onNewBadge, modifier)
}

/** Navy pill: "2 new" on discussion notes. */
@Composable
fun DlCountPill(text: String, modifier: Modifier = Modifier) {
    Pill(text, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, modifier, tracking = false)
}

/** Mail-blue pill: "2 new" on email threads. 20 dp tall. */
@Composable
fun DlMailPill(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(20.dp)
            .background(DlTheme.colors.mailBlue, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = DlTheme.type.pill.copy(letterSpacing = 0.sp), color = DlTheme.colors.onMailBlue) }
}

/** Edit-window chip on a sent letter in reach: "3:41 to edit". 26 dp, signature tint. */
@Composable
fun DlEditWindowChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(26.dp)
            .background(DlTheme.colors.userATint, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Plain dot: unread email (navy 8 dp), notifications off (red 9 dp). */
@Composable
fun DlDot(modifier: Modifier = Modifier, size: Dp = 8.dp, error: Boolean = false) {
    Box(
        modifier
            .size(size)
            .background(if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, CircleShape),
    )
}

@Composable
private fun Pill(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier,
    tracking: Boolean = true,
) {
    Box(
        modifier
            .height(22.dp)
            .background(container, RoundedCornerShape(11.dp))
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = if (tracking) DlTheme.type.pill else DlTheme.type.pill.copy(letterSpacing = 0.sp), color = content)
    }
}
