package com.delulutopia.core.ui.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.view.LayoutInflater
import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.VideoLimits
import com.delulutopia.core.ui.R
import com.delulutopia.core.ui.httpClient
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * Emails Q12, Subscription Tiers ST4: at most the tier's length (15 s Free, 60 s Premium); the
 * server allows half a second more for a container that runs past the stop.
 */
val MAX_VIDEO_MS: Long get() = VideoLimits.maxMs
val VIDEO_TOO_LONG: String get() = t("media.videoTooLong", "n" to VideoLimits.seconds)

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
 * The controls' full-screen button opens [FullscreenVideo]: the same player, uninterrupted, grown
 * from this frame to the whole screen (and shrunk back on exit or Back).
 */
@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(url: String, posterUrl: String?, durationMs: Long?, width: Int?, height: Int?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var started by remember(url) { mutableStateOf(false) }
    var fullscreen by remember(url) { mutableStateOf(false) }
    // Where the frame is on screen: the full-screen transition grows from it and shrinks back to it.
    var frame by remember { mutableStateOf(Rect.Zero) }
    val ratio = if (width != null && height != null && width > 0 && height > 0) width.toFloat() / height else 16f / 9f
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .fillMaxWidth(if (ratio < 1f) 0.6f else 1f)
            .aspectRatio(ratio)
            .clip(shape)
            .background(DlTheme.colors.lightbox)
            .onGloballyPositioned { frame = Rect(it.positionOnScreen(), it.size.toSize()) }
            .semantics { contentDescription = durationMs?.let { t("media.videoWithLength", "length" to mmss(it)) } ?: t("media.video") },
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
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        // Setting the listener shows the controls' full-screen button.
                        setFullscreenButtonClickListener { full -> if (full) fullscreen = true }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                // While full screen, the player draws there; back here the button shows "enter" again.
                update = { view ->
                    view.player = if (fullscreen) null else player
                    view.setFullscreenButtonState(false)
                },
            )
            if (fullscreen) FullscreenVideo(player, ratio, frame) { fullscreen = false }
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
                    .semantics { contentDescription = t("media.playVideo") },
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

/** The grow/shrink transition: emphasized-decelerate in, a little quicker out. */
private const val ENTER_MS = 320
private const val EXIT_MS = 260

/**
 * The video full screen (black, system bars hidden, Back or the controls' exit button leaves): the
 * playing [player] moves to a TextureView here without a pause, and the picture grows from [from]
 * (the in-place frame, screen coordinates) to the screen, fitted by [ratio]; leaving runs it back
 * before [onClosed] hands the player back to the frame.
 */
@OptIn(UnstableApi::class)
@Composable
private fun FullscreenVideo(player: ExoPlayer, ratio: Float, from: Rect, onClosed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            playerView?.hideController()
            scope.launch {
                progress.animateTo(0f, tween(EXIT_MS, easing = FastOutSlowInEasing))
                onClosed()
            }
        }
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(ENTER_MS, easing = EmphasizedDecelerate))
        playerView?.showController()
    }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        DisposableEffect(window) {
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    hide(WindowInsetsCompat.Type.systemBars())
                }
            }
            onDispose {}
        }
        var origin by remember { mutableStateOf(Offset.Unspecified) }
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { origin = it.positionOnScreen() }
                .drawBehind { drawRect(Color.Black.copy(alpha = progress.value)) },
        ) {
            val screenW = constraints.maxWidth.toFloat()
            val screenH = constraints.maxHeight.toFloat()
            // The picture's final rectangle: the video fitted to the screen, centred.
            val w = if (screenW / screenH > ratio) screenH * ratio else screenW
            val h = w / ratio
            val target = Rect(Offset((screenW - w) / 2, (screenH - h) / 2), Size(w, h))
            val density = LocalDensity.current
            AndroidView(
                factory = { ctx ->
                    (LayoutInflater.from(ctx).inflate(R.layout.dl_texture_player_view, null) as PlayerView).apply {
                        this.player = player
                        controllerAutoShow = false
                        hideController()
                        setFullscreenButtonState(true)
                        setFullscreenButtonClickListener { full -> if (!full) close() }
                        playerView = this
                    }
                },
                modifier = Modifier
                    .offset { IntOffset(target.left.roundToInt(), target.top.roundToInt()) }
                    .size(with(density) { w.toDp() }, with(density) { h.toDp() })
                    .graphicsLayer {
                        val p = progress.value
                        val start = if (origin.isSpecified && from.width > 0f && from.isFinite) from.translate(-origin) else null
                        if (start == null) {
                            alpha = p
                        } else {
                            // From the in-place frame (fitted to the same ratio) to the full rectangle.
                            scaleX = mix(start.width / w, 1f, p)
                            scaleY = mix(start.height / h, 1f, p)
                            translationX = mix(start.center.x - target.center.x, 0f, p)
                            translationY = mix(start.center.y - target.center.y, 0f, p)
                        }
                    },
                onRelease = { it.player = null },
            )
        }
    }
}

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private fun mix(start: Float, stop: Float, fraction: Float) = start + (stop - start) * fraction
