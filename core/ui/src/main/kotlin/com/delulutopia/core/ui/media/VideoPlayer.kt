package com.delulutopia.core.ui.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.ui.httpClient

/** Emails Q12: at most 15 s; the server allows 15.5 s for a container that runs past the stop. */
const val MAX_VIDEO_MS = 15_000L
const val VIDEO_TOO_LONG = "This video is longer than 15 seconds"

/** A picked video's length, or null when the phone cannot tell (the server decides then). */
fun videoMs(context: Context, uri: Uri): Long? {
    val r = MediaMetadataRetriever()
    return try {
        r.setDataSource(context, uri)
        r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
    } catch (_: Exception) {
        null
    } finally {
        r.release()
    }
}

fun tooLong(ms: Long?): Boolean = ms != null && ms > MAX_VIDEO_MS + 500

/**
 * An email's video (Emails Q15; web media/VideoPlayer.tsx): the poster with ▶ and its length,
 * played in place with sound and the player's own controls. One medium plays at a time.
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, posterUrl: String?, durationMs: Long?, width: Int?, height: Int?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var started by remember(url) { mutableStateOf(false) }
    val ratio = if (width != null && height != null && width > 0 && height > 0) width.toFloat() / height else 16f / 9f
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .fillMaxWidth(if (ratio < 1f) 0.6f else 1f)
            .aspectRatio(ratio)
            .clip(shape)
            .background(DlTheme.colors.lightbox)
            .semantics { contentDescription = durationMs?.let { "Video, ${mmss(it)}" } ?: "Video" },
    ) {
        if (started) {
            val player = remember(url) {
                ExoPlayer.Builder(context)
                    .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(context.httpClient())))
                    .build()
                    .apply {
                        setMediaItem(MediaItem.fromUri(url))
                        prepare()
                        playWhenReady = true
                    }
            }
            LaunchedEffect(OnePlayer.playing) { if (OnePlayer.playing) player.pause() }
            DisposableEffect(player) { onDispose { player.release() } }
            AndroidView(factory = { PlayerView(it).apply { this.player = player } }, modifier = Modifier.fillMaxSize())
        } else {
            if (posterUrl != null) AsyncImage(model = posterUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Box(
                Modifier
                    .align(Alignment.Center)
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(DlTheme.colors.lightbox.copy(alpha = 0.7f))
                    .clickable {
                        OnePlayer.stop()
                        started = true
                    }
                    .semantics { contentDescription = "Play video" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(WebIcons.Play, null, Modifier.size(22.dp), tint = DlTheme.colors.onLightbox)
            }
            if (durationMs != null && durationMs > 0) {
                Text(
                    mmss(durationMs),
                    Modifier.align(
                        Alignment.BottomEnd,
                    ).padding(8.dp).background(DlTheme.colors.lightbox.copy(alpha = 0.7f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = DlTheme.colors.onLightbox,
                )
            }
        }
    }
}
