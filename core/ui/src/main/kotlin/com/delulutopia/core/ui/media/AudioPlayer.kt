package com.delulutopia.core.ui.media

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import kotlinx.coroutines.delay

/** `m:ss` of a duration (web media/duration.ts). */
fun mmss(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

/**
 * A voice note (FS-R9; web media/AudioPlayer.tsx): play/pause, a scrub bar, elapsed and total. One
 * note plays at a time across the app.
 */
@Composable
fun AudioPlayer(url: String, durationMs: Long?, modifier: Modifier = Modifier, compact: Boolean = false, label: String = "voice note") {
    val context = LocalContext.current
    val mine = OnePlayer.url == url
    val playing = mine && OnePlayer.playing
    LaunchedEffect(playing) {
        while (playing) {
            OnePlayer.tick()
            delay(100)
        }
    }
    val total = (durationMs?.takeIf { it > 0 } ?: if (mine) OnePlayer.durationMs else 0L)
    val time = if (mine) OnePlayer.positionMs else 0L
    // Web .dl-player: 48 dp pill on the page paper with a line border; 36 dp navy play button; a 4 dp
    // track (navy up to the playhead, line after) with a 14 dp navy thumb ringed in paper; 12/600 time.
    val seal = MaterialTheme.colorScheme.primary
    val line = MaterialTheme.colorScheme.outlineVariant
    val ring = DlTheme.colors.letterEnvelope
    val fraction = if (total > 0) (time.toFloat() / total).coerceIn(0f, 1f) else 0f
    Row(
        modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .height(if (compact) 40.dp else 48.dp)
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
            .border(1.dp, line, RoundedCornerShape(24.dp))
            .padding(start = 6.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
    ) {
        val button = if (compact) 28.dp else 36.dp
        Box(
            Modifier
                .size(button)
                .clip(CircleShape)
                .background(seal)
                .clickable(role = Role.Button, onClickLabel = if (playing) "Pause $label" else "Play $label") {
                    if (playing) OnePlayer.pause() else OnePlayer.play(context, url)
                }
                .semantics { contentDescription = if (playing) "Pause $label" else "Play $label" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) WebIcons.Pause else WebIcons.Play, null, Modifier.size(if (compact) 12.dp else 14.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Canvas(
            Modifier
                .weight(1f)
                .height(44.dp)
                .semantics { contentDescription = "Scrub bar, ${mmss(time)} of ${mmss(total)}" }
                .pointerInput(url, total) {
                    detectTapGestures { o -> if (total > 0) OnePlayer.seek(context, url, (o.x / size.width * total).toLong()) }
                }
                .pointerInput(url, total) {
                    detectHorizontalDragGestures { change, _ ->
                        if (total > 0) OnePlayer.seek(context, url, ((change.position.x / size.width).coerceIn(0f, 1f) * total).toLong())
                    }
                },
        ) {
            val y = size.height / 2
            val h = 4.dp.toPx()
            val x = size.width * fraction
            drawLine(line, Offset(0f, y), Offset(size.width, y), h, StrokeCap.Round)
            if (x > 0f) drawLine(seal, Offset(0f, y), Offset(x, y), h, StrokeCap.Round)
            val r = 7.dp.toPx()
            val cx = x.coerceIn(r, size.width - r)
            drawCircle(ring, r, Offset(cx, y))
            drawCircle(seal, r - 2.dp.toPx(), Offset(cx, y))
        }
        Text(
            if (playing || time > 0) "${mmss(time)} / ${mmss(total)}" else mmss(total),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
}
