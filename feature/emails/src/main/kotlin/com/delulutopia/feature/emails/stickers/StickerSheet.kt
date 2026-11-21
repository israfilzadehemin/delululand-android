package com.delulutopia.feature.emails.stickers

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Sticker
import com.delulutopia.core.network.model.StickerRef

/**
 * Tapping a sticker in a thread (doc "Delululand — Stickers" SK10; web stickers/StickerSheet.tsx): the
 * sticker at 240 dp, ★ Favourite, and "More from this pack" (8, a tap sends) — or who added one of Ours.
 */
@Composable
internal fun StickerSheet(
    store: StickerStore,
    sticker: StickerRef,
    caption: String,
    canSend: Boolean,
    addedBy: (Sticker) -> String,
    onSend: (Sticker) -> Unit,
    onDismiss: () -> Unit,
) {
    val base = LocalServerBaseUrl.current
    val cat = store.catalogue
    val full = cat?.stickers?.firstOrNull { it.id == sticker.id }
    val fav = store.isFavourite(sticker.id)
    val pack = full?.pack?.let { p -> cat?.packs?.firstOrNull { it.slug == p } }
    val more = full?.pack?.let { p -> cat?.stickers?.filter { it.pack == p && it.id != full.id }?.take(8) }.orEmpty()
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    DlBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                resolveMedia(base, sticker.url512),
                full?.let { stickerLabel(it) } ?: t("stickers.label"),
                Modifier.size(240.dp),
            )
            Text(
                if (full?.custom != null) addedBy(full) else caption,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                color = quiet,
                textAlign = TextAlign.Center,
            )
            if (full != null) {
                DlOutlinedButton(
                    if (fav) t("stickers.removeFavourite") else t("stickers.addFavourite"),
                    { store.toggleFavourite(sticker.id) },
                    icon = WebIcons.Star,
                )
            }
            if (pack != null && more.isNotEmpty()) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        t("stickers.morePack", "pack" to pack.name),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                    )
                    if (canSend) Text(t("stickers.tapToSend"), style = MaterialTheme.typography.bodySmall, color = quiet)
                }
                more.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        row.forEach { s ->
                            val label = stickerLabel(s)
                            Box(
                                Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(enabled = canSend, role = Role.Button) {
                                        onSend(s)
                                        onDismiss()
                                    }
                                    .padding(6.dp)
                                    .semantics { contentDescription = label },
                                contentAlignment = Alignment.Center,
                            ) { AsyncImage(resolveMedia(base, s.url256), null, Modifier.fillMaxSize()) }
                        }
                        repeat(4 - row.size) { Box(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}
