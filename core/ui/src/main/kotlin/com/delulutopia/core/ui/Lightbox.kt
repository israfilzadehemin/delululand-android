package com.delulutopia.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.motion.motionOn
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Att
import kotlinx.coroutines.launch

/** Where each photo's thumbnail is on screen (by attachment id), so the lightbox grows out of it and back into it. */
private val sources = HashMap<String, Rect>()

/** Marks [att]'s thumbnail as the place its full-size copy grows from in [Lightbox]. */
fun Modifier.lightboxSource(att: Att): Modifier = composed {
    DisposableEffect(att.id) { onDispose { sources.remove(att.id) } }
    onGloballyPositioned { sources[att.id] = Rect(it.positionOnScreen(), it.size.toSize()) }
}

private const val OPEN_MS = 340
private const val CLOSE_MS = 260

/**
 * S8 (FS-R8; web letters/Lightbox.tsx): the large copy full screen, "Photo n of m", swipe between
 * the item's photos, × or Back to close. [title] names a single image (a doodle) instead.
 *
 * Motion: the photo grows out of its thumbnail ([lightboxSource]) to fill the screen while the dark
 * backdrop fades in, and on close the photo then showing shrinks back into its own thumbnail. A photo
 * with no thumbnail on screen fades and zooms instead.
 */
@Composable
fun Lightbox(photos: List<Att>, index: Int, onIndex: (Int) -> Unit, onClose: () -> Unit, title: String? = null) {
    val c = DlTheme.colors
    val base = LocalServerBaseUrl.current
    val pager = rememberPagerState(initialPage = index) { photos.size }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { if (it != index) onIndex(it) } }
    LaunchedEffect(index) { if (pager.currentPage != index) pager.scrollToPage(index) }
    val motion = motionOn()
    val progress = remember { Animatable(if (motion) 0f else 1f) }
    val scope = rememberCoroutineScope()
    var closing by remember { mutableStateOf(false) }
    val close: () -> Unit = {
        if (!closing) {
            closing = true
            scope.launch {
                if (motion) progress.animateTo(0f, tween(CLOSE_MS, easing = Motion.EmphasizedAccelerate))
                onClose()
            }
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(OPEN_MS, easing = Motion.EmphasizedDecelerate)) }
    Dialog(onDismissRequest = close, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        var screen by remember { mutableStateOf(Rect.Zero) }
        Column(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { screen = Rect(it.positionOnScreen(), it.size.toSize()) }
                .drawBehind { drawRect(c.lightbox.copy(alpha = c.lightbox.alpha * progress.value)) }
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().graphicsLayer { alpha = progress.value }.padding(start = 16.dp, end = 4.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    title ?: t("letters.photoOf", "n" to pager.currentPage + 1, "count" to photos.size),
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.onLightbox,
                )
                IconButton(onClick = close) { Icon(WebIcons.Close, t("common.close"), tint = c.onLightbox) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { i ->
                var frame by remember { mutableStateOf(Rect.Zero) }
                AsyncImage(
                    model = resolveMedia(base, photos[i].url),
                    contentDescription = title ?: t("letters.photoOf", "n" to i + 1, "count" to photos.size),
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { frame = Rect(it.positionOnScreen(), it.size.toSize()) }
                        .graphicsLayer {
                            val p = progress.value
                            if (p >= 1f || i != pager.currentPage) return@graphicsLayer
                            val from = sources[photos[i].id]?.takeIf { frame.width > 0f && it.width > 0f && it.overlaps(screen) }
                            if (from == null) {
                                alpha = p
                                scaleX = 0.94f + 0.06f * p
                                scaleY = scaleX
                            } else {
                                // The picture as drawn (fitted, centred) starts at the thumbnail's size and place.
                                val ratio = photos[i].let { a ->
                                    if ((a.width ?: 0) > 0 &&
                                        (a.height ?: 0) > 0
                                    ) {
                                        a.width!!.toFloat() / a.height!!
                                    } else {
                                        from.width / from.height
                                    }
                                }
                                val fitW = minOf(frame.width, frame.height * ratio)
                                val fitH = fitW / ratio
                                val start = minOf(from.width / fitW, from.height / fitH)
                                scaleX = start + (1f - start) * p
                                scaleY = scaleX
                                translationX = (from.center.x - frame.center.x) * (1f - p)
                                translationY = (from.center.y - frame.center.y) * (1f - p)
                            }
                        },
                    contentScale = ContentScale.Fit,
                )
            }
            if (photos.size > 1) {
                Row(
                    Modifier.fillMaxWidth().graphicsLayer { alpha = progress.value }.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                ) {
                    photos.indices.forEach { i ->
                        Box(Modifier.size(6.dp).background(if (i == pager.currentPage) c.onLightbox else c.onLightboxQuiet.copy(alpha = 0.4f), CircleShape))
                    }
                }
            }
        }
    }
}
