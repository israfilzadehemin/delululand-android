package com.delulutopia.feature.emails.stickers

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Sticker
import kotlinx.coroutines.launch

/**
 * The sticker picker (doc "Delululand — Stickers" FS-SK1–SK5; web stickers/StickerPicker.tsx), in place
 * of the keyboard: search, Recent, Favourites, Ours (+ and N of cap), the packs, and a strip of
 * sections at the bottom. One tap sends (FS-SK6) and it stays open; at 0 credits the grid is dimmed
 * (FS-SK5). Long-press on one of Ours offers Delete (FS-SK25).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun StickerPicker(
    store: StickerStore,
    disabled: Boolean,
    disabledLine: String?,
    onPick: (Sticker) -> Unit,
    onAdd: () -> Unit,
    onDelete: (Sticker) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cat = store.catalogue
    var query by remember { mutableStateOf("") }
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val base = LocalServerBaseUrl.current
    val all = remember(cat) { cat?.let { sections(it) } ?: emptyList() }
    val results = remember(cat, query) { if (query.isBlank() || cat == null) emptyList() else cat.stickers.filter { stickerMatches(it, query) } }
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val line = MaterialTheme.colorScheme.outlineVariant
    var active by remember { mutableStateOf("recent") }

    // Where each section starts in the grid (headers count as one item each).
    val starts = remember(all) {
        var i = 0
        all.associate { sec ->
            val at = i
            i += 1 + (if (sec.add) 1 else 0) + sec.items.size
            sec.key to at
        }
    }

    Column(modifier.fillMaxWidth().height(340.dp).background(MaterialTheme.colorScheme.surface)) {
        // Search
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 2.dp)
                .fillMaxWidth()
                .height(40.dp)
                .clip(shape)
                .background(DlTheme.colors.letterEnvelope)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(WebIcons.Search, null, Modifier.size(18.dp), tint = quiet)
            val style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.weight(1f).semantics { contentDescription = t("stickers.search") },
                singleLine = true,
                textStyle = style,
                cursorBrush = SolidColor(DlTheme.colors.mailBlue),
                decorationBox = { inner ->
                    Box {
                        if (query.isEmpty()) Text(t("stickers.search"), style = style, color = DlTheme.colors.inkDisabled)
                        inner()
                    }
                },
            )
        }
        if (disabled && disabledLine != null) {
            Text(
                disabledLine,
                Modifier.fillMaxWidth().padding(top = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center,
            )
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                cat == null && store.failed -> Text(t("stickers.couldntLoad"), Modifier.align(Alignment.Center), color = quiet)
                cat == null -> Unit
                query.isNotBlank() && results.isEmpty() -> Text(t("stickers.nothingFound"), Modifier.align(Alignment.Center), color = quiet)
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    state = grid,
                    modifier = Modifier.fillMaxSize().alpha(if (disabled) 0.45f else 1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (query.isNotBlank()) {
                        items(results, key = { "q-" + it.id }) { s ->
                            Tile(s, base, disabled, own = s.custom != null, onPick, onDelete, haptics)
                        }
                    } else {
                        all.forEach { sec ->
                            item(key = "h-" + sec.key, span = { GridItemSpan(maxLineSpan) }) {
                                Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        sec.title,
                                        Modifier.weight(1f).semantics { heading() },
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold),
                                        color = quiet,
                                    )
                                    if (sec.add) {
                                        Text(
                                            t("stickers.usedOfCap", "used" to cat.custom.used, "cap" to cat.custom.cap),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                                            color = quiet,
                                        )
                                    }
                                }
                            }
                            if (sec.add) {
                                item(key = "add") {
                                    val addLabel = t("stickers.addSticker")
                                    Box(
                                        Modifier
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, line, RoundedCornerShape(12.dp))
                                            .clickable(role = Role.Button, onClick = onAdd)
                                            .semantics { contentDescription = addLabel },
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(DlIcons.Plus, null, Modifier.size(24.dp), tint = quiet) }
                                }
                            }
                            items(sec.items, key = { sec.key + "-" + it.id }) { s ->
                                Tile(s, base, disabled, own = sec.key == "ours", onPick, onDelete, haptics)
                            }
                        }
                    }
                }
            }
        }
        if (query.isBlank() && all.isNotEmpty()) {
            LazyRow(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(all, key = { "s-" + it.key }) { sec ->
                    val on = active == sec.key
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) DlTheme.colors.letterEnvelope else Color.Transparent)
                            .clickable(role = Role.Button) {
                                active = sec.key
                                starts[sec.key]?.let { scope.launch { grid.scrollToItem(it) } }
                            }
                            .semantics {
                                contentDescription = sec.title
                                selected = on
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        when (sec.key) {
                            "recent" -> Icon(WebIcons.Recent, null, Modifier.size(20.dp), tint = quiet)
                            "favourites" -> Icon(WebIcons.Star, null, Modifier.size(20.dp), tint = quiet)
                            "ours" -> Icon(WebIcons.HeartOutline, null, Modifier.size(20.dp), tint = quiet)
                            else -> sec.items.firstOrNull()?.let { AsyncImage(resolveMedia(base, it.url256), null, Modifier.size(28.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Tile(
    s: Sticker,
    base: String,
    disabled: Boolean,
    own: Boolean,
    onPick: (Sticker) -> Unit,
    onDelete: (Sticker) -> Unit,
    haptics: HapticFeedback,
) {
    val label = stickerLabel(s)
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                enabled = !disabled,
                role = Role.Button,
                onLongClick = if (own) {
                    {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDelete(s)
                    }
                } else {
                    null
                },
                onClick = { onPick(s) },
            )
            .padding(4.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(resolveMedia(base, s.url256), null, Modifier.fillMaxSize())
    }
}
