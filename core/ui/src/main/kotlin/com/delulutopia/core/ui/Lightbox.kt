package com.delulutopia.core.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Att

/**
 * S8 (FS-R8; web letters/Lightbox.tsx): the large copy full screen, "Photo n of m", swipe between
 * the item's photos, × or Back to close. [title] names a single image (a doodle) instead.
 */
@Composable
fun Lightbox(photos: List<Att>, index: Int, onIndex: (Int) -> Unit, onClose: () -> Unit, title: String? = null) {
    val c = DlTheme.colors
    val base = LocalServerBaseUrl.current
    val pager = rememberPagerState(initialPage = index) { photos.size }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { if (it != index) onIndex(it) } }
    LaunchedEffect(index) { if (pager.currentPage != index) pager.scrollToPage(index) }
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(Modifier.fillMaxSize().background(c.lightbox).statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title ?: "Photo ${pager.currentPage + 1} of ${photos.size}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.onLightbox,
                )
                IconButton(onClick = onClose) { Icon(WebIcons.Close, "Close", tint = c.onLightbox) }
            }
            HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), beyondViewportPageCount = 1) { i ->
                AsyncImage(
                    model = resolveMedia(base, photos[i].url),
                    contentDescription = title ?: "Photo ${i + 1} of ${photos.size}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
            }
            if (photos.size > 1) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
                    photos.indices.forEach { i ->
                        Box(Modifier.size(6.dp).background(if (i == pager.currentPage) c.onLightbox else c.onLightboxQuiet.copy(alpha = 0.4f), CircleShape))
                    }
                }
            }
        }
    }
}
