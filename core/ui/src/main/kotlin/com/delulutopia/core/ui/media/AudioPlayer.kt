package com.delulutopia.core.ui.media

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
    Row(
        modifier
            .then(if (compact) Modifier else Modifier.fillMaxWidth())
            .background(DlTheme.colors.letterEnvelope, RoundedCornerShape(24.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IconButton(onClick = { if (playing) OnePlayer.pause() else OnePlayer.play(context, url) }) {
            Box(Modifier.size(36.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
                Icon(
                    if (playing) WebIcons.Pause else WebIcons.Play,
                    if (playing) "Pause $label" else "Play $label",
                    Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
        Slider(
            value = if (total > 0) (time.toFloat() / total).coerceIn(0f, 1f) else 0f,
            onValueChange = { f -> if (total > 0) OnePlayer.seek(context, url, (f * total).toLong()) },
            modifier = Modifier.weight(1f).semantics { contentDescription = "Scrub bar, ${mmss(time)} of ${mmss(total)}" },
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
        )
        Text(
            if (playing || time > 0) "${mmss(time)} / ${mmss(total)}" else mmss(total),
            style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 14.dp),
        )
    }
}
