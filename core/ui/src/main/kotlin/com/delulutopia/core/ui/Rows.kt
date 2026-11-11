package com.delulutopia.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.network.model.LetterMedia
import com.delulutopia.core.network.model.Row as LetterRow

/** What a row says when there is no text (FS-R12). */
fun firstLineOf(firstLine: String?, media: LetterMedia?): String = when {
    !firstLine.isNullOrEmpty() -> firstLine
    media != null && media.photos > 0 -> "Photo"
    media != null && media.audios > 0 -> "Voice note"
    media != null && media.doodles > 0 -> "Doodle"
    else -> ""
}

fun firstLineOf(row: LetterRow): String = firstLineOf(row.firstLine, row.media)

/**
 * Which paper object a tile is (Letter tiles story): a sealed envelope until the letter has arrived
 * and been opened, an unfolded sheet after. A sent letter that has arrived but is unread is a sheet.
 */
fun tileShapeOf(row: LetterRow, state: String): TileShape {
    val arrived = state == LetterState.DELIVERED || state == LetterState.READ
    if (!arrived) return TileShape.Envelope
    return if (row.direction == "from" && state == LetterState.DELIVERED) TileShape.Envelope else TileShape.Sheet
}

/** The offline line (UX S2 offline): shown over what was loaded last. */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, brand: String = LocalBrand.current) {
    val c = MaterialTheme.colorScheme
    Row(
        modifier
            .enter(Enter.Drop, durationMs = 280)
            .fillMaxWidth()
            .background(c.errorContainer, RoundedCornerShape(12.dp))
            .border(1.dp, c.error.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(WebIcons.Offline, null, Modifier.size(18.dp), tint = c.error)
        Text("Can't reach $brand. Showing what we had.", style = MaterialTheme.typography.bodyMedium, color = c.onErrorContainer)
    }
}

/** The universe's name, wherever the web says "Delulutopia" (Universe C48). */
val LocalBrand = staticCompositionLocalOf { "Delulutopia" }
