package com.delulutopia.core.ui.media

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.Uploader

/** Emails only (Q11–Q13): record or pick one video; [full] while the email already carries one. */
class VideoAdd(val full: Boolean, val onRecord: () -> Unit, val onPick: (Uri) -> Unit)

/** Letters only (Doodles D1, D2): open the pad; [full] while the letter already carries its one doodle. */
class DoodleAdd(val full: Boolean, val onOpen: () -> Unit)

/**
 * The strip under the letter (UX S4; FS-W6–W10; web media/AttachmentStrip.tsx; canvas W-Draft):
 * photo prints taped on with a progress ring while uploading, voice-note pills that play before
 * sending, the doodle, the video, and the add buttons. Refusals are lines under it.
 */
@Composable
fun AttachmentStrip(
    attachments: Attachments,
    onRemove: (Item, Int) -> Unit,
    voice: VoiceState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    doodle: DoodleAdd? = null,
    video: VideoAdd? = null,
) {
    val photosPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        uris.forEach { attachments.add(Uploader.Kind.Photo, Source.Picked(it), nameOf(it, "photo.jpg")) }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { attachments.add(Uploader.Kind.Audio, Source.Picked(it), nameOf(it, "audio.m4a")) }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let { video?.onPick?.invoke(it) } }
    val items = attachments.items
    val photoCount = items.count { it.kind == "photo" }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(start = 12.dp, end = 12.dp, top = 14.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()).padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                var photoNo = 0
                items.forEachIndexed { index, item ->
                    when (item.kind) {
                        "photo" -> {
                            photoNo++
                            Print(item, "Photo $photoNo of $photoCount", "Remove photo $photoNo") { onRemove(item, index) }
                        }
                        "doodle" -> Print(item, "Doodle", "Remove doodle") { onRemove(item, index) }
                        "video" -> Print(item, "Video", "Remove video", length = item.att?.durationMs ?: item.pending?.durationMs) { onRemove(item, index) }
                        else -> VoicePill(item) { onRemove(item, index) }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AddButton(WebIcons.Photo, "Add photo", enabled) {
                    photosPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                if (voice.recording) {
                    Row(
                        Modifier
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.errorContainer)
                            .clickable(onClick = voice.stop)
                            .semantics { contentDescription = "Stop recording" }
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(Modifier.size(10.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                        Text(mmss(voice.elapsedMs), style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"))
                        Text("Stop", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                } else {
                    AddButton(WebIcons.Mic, "Record a voice note", enabled, onClick = voice.start)
                }
                AddButton(WebIcons.AudioFile, "Attach audio file", enabled) { audioPicker.launch("audio/*") }
                if (doodle != null) {
                    AddButton(
                        WebIcons.Doodle,
                        if (doodle.full) "Doodle attached · one per letter" else "Draw a doodle",
                        enabled && !doodle.full,
                        onClick = doodle.onOpen,
                    )
                }
                if (video != null) {
                    AddButton(WebIcons.Video, "Record a video", enabled && !video.full, onClick = video.onRecord)
                    AddButton(WebIcons.VideoFile, "Pick a video", enabled && !video.full) {
                        videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                    }
                }
            }
        }
        attachments.errors.forEach { (key, text) ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                IconButton(onClick = { attachments.dismissError(key) }) { Icon(WebIcons.Close, "Dismiss", Modifier.size(14.dp)) }
            }
        }
    }
}

private fun nameOf(uri: Uri, fallback: String): String = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { '.' in it } ?: fallback

@Composable
private fun AddButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(DlTheme.colors.letterEnvelope)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = if (enabled) MaterialTheme.colorScheme.onSurface else DlTheme.colors.inkDisabled)
    }
}

/** A photo, doodle or video poster taped onto the strip, with its × and the upload ring. */
@Composable
private fun Print(item: Item, label: String, removeLabel: String, length: Long? = null, onRemove: () -> Unit) {
    val base = LocalServerBaseUrl.current
    val model: Any? = item.att?.let { a -> (a.thumbUrl ?: a.url.takeIf { a.kind != "video" })?.let { resolveMedia(base, it) } } ?: item.local
    // A new tile or pill pops in with a little spring (web .dl-tile, .dl-pill).
    Box(Modifier.enter(Enter.PopIn, durationMs = 280, easing = Motion.Spring).size(60.dp)) {
        AsyncImage(
            model = model,
            contentDescription = label,
            modifier = Modifier.size(60.dp).shadow(2.dp).background(DlTheme.colors.letterSheet),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-6).dp).size(26.dp, 10.dp).rotate(-2f).background(DlTheme.colors.letterTape))
        if (length != null && length > 0) {
            Text(
                mmss(length),
                Modifier.align(
                    Alignment.BottomStart,
                ).padding(3.dp).background(DlTheme.colors.lightbox.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = DlTheme.colors.onLightbox,
            )
        }
        item.pending?.let { Ring(it.progress, it.processing, Modifier.align(Alignment.Center)) }
        RemoveX(removeLabel, Modifier.align(Alignment.TopEnd).offset(x = 12.dp, y = (-12).dp), onRemove)
    }
}

@Composable
private fun RemoveX(label: String, modifier: Modifier, onRemove: () -> Unit) {
    Box(modifier.size(36.dp).clip(CircleShape).clickable(onClick = onRemove).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(24.dp).background(MaterialTheme.colorScheme.surface, CircleShape).border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WebIcons.Close, null, Modifier.size(10.dp))
        }
    }
}

/** Fills while bytes go out; once all are sent it spins while the server converts. */
@Composable
private fun Ring(fraction: Float, busy: Boolean, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val hue = DlTheme.colors.postmarkHue
    val paper = DlTheme.colors.letterSheet
    val spin by rememberInfiniteTransition(
        label = "ring",
    ).animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "spin")
    Canvas(modifier.size(36.dp).semantics { contentDescription = if (busy) "Processing" else "Uploading" }) {
        val r = 14.dp.toPx()
        drawCircle(paper.copy(alpha = 0.85f), r)
        drawCircle(line, r, style = Stroke(3.dp.toPx()))
        val sweep = if (busy) 90f else 360f * fraction
        drawArc(
            hue,
            if (busy) spin else -90f,
            sweep,
            false,
            topLeft = center.copy(x = center.x - r, y = center.y - r),
            size = Size(r * 2, r * 2),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
    }
}

/** A voice note on the strip: play it before sending, its length, ×. */
@Composable
private fun VoicePill(item: Item, onRemove: () -> Unit) {
    val context = LocalContext.current
    val base = LocalServerBaseUrl.current
    val url = item.att?.url?.let { resolveMedia(base, it) }
    val ms = item.att?.durationMs ?: item.pending?.durationMs
    val playing = url != null && OnePlayer.url == url && OnePlayer.playing
    Row(
        Modifier
            .enter(Enter.PopIn, durationMs = 280, easing = Motion.Spring)
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DlTheme.colors.letterEnvelope)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(enabled = url != null) { if (playing) OnePlayer.pause() else OnePlayer.play(context, url!!) }
                .semantics { contentDescription = if (playing) "Pause voice note" else "Play voice note" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (playing) WebIcons.Pause else WebIcons.Play, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.onPrimary)
        }
        Text(
            if (ms !=
                null
            ) {
                mmss(ms + 500)
            } else {
                "…"
            },
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
        )
        if (item.pending != null) Ring(item.pending.progress, item.pending.processing, Modifier.size(20.dp))
        Box(
            Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onRemove).semantics {
                contentDescription = "Remove voice note"
            },
            contentAlignment = Alignment.Center,
        ) {
            Icon(WebIcons.Close, null, Modifier.size(11.dp))
        }
    }
}
