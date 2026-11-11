package com.delulutopia.feature.letters

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.network.model.StampCatalogue
import com.delulutopia.core.network.model.StampMark
import com.delulutopia.core.network.model.StampView
import java.text.Normalizer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Case- and diacritic-insensitive, so `krakow` finds Kraków (FS-ST11). */
internal fun fold(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").lowercase()

internal fun matches(stamp: StampView, folded: String): Boolean =
    folded.isEmpty() || fold(stamp.name).contains(folded) || stamp.keywords.any { fold(it).contains(folded) }

/**
 * The stamp picker (FS-ST9–ST13; web stamps/StampPicker.tsx; canvas W-Stamps): search, the Recent
 * row, "No stamp", then a section per theme with a strip of theme buttons — the shape of a phone's
 * emoji keyboard. One tap selects and closes.
 */
@Composable
internal fun StampPicker(catalogue: StampCatalogue?, failed: Boolean, current: StampMark?, onPick: (StampMark?) -> Unit, onClose: () -> Unit) {
    val base = LocalServerBaseUrl.current
    val scope = rememberCoroutineScope()
    var raw by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(raw) {
        delay(150)
        query = raw.trim()
    }
    val all = catalogue?.stamps.orEmpty()
    val themes = catalogue?.themes.orEmpty()
    val folded = fold(query)
    val results = if (folded.isEmpty()) emptyList() else all.filter { matches(it, folded) }
    val bySlug = all.associateBy { it.slug }
    val recent = catalogue?.recent.orEmpty().mapNotNull { bySlug[it] }
    val grid = rememberLazyGridState()
    val pick = { s: StampView? ->
        onPick(s?.let { StampMark(it.slug, it.name, it.imageUrl) })
        onClose()
    }
    // Where each theme's heading sits in the grid, for the theme strip.
    val headings = HashMap<String, Int>()

    DlBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().height(560.dp)) {
            OutlinedTextField(
                value = raw,
                onValueChange = { raw = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).semantics { contentDescription = "Search stamps by name or keyword" },
                placeholder = { Text("Search stamps") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                state = grid,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var index = 0
                fun heading(text: String, slug: String? = null) {
                    if (slug != null) headings[slug] = index
                    index++
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text,
                            Modifier.padding(top = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                fun cells(section: String, stamps: List<StampView>) {
                    index += stamps.size
                    stampCells(section, stamps, current, base, pick)
                }
                when {
                    catalogue == null && failed -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            "Couldn't load the stamps. Check your connection and try again.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                        )
                    }
                    catalogue == null -> item(span = { GridItemSpan(maxLineSpan) }) {
                        Text("Loading stamps…", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                    query.isNotEmpty() -> if (results.isEmpty()) {
                        item(span = {
                            GridItemSpan(maxLineSpan)
                        }) { Text("Nothing found", color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center) }
                    } else {
                        cells("q", results)
                    }
                    else -> {
                        if (recent.isNotEmpty()) {
                            heading("Recent")
                            cells("recent", recent)
                        }
                        if (current != null) {
                            index++
                            item(span = { GridItemSpan(maxLineSpan) }) { DlOutlinedButton("No stamp", { pick(null) }) }
                        }
                        themes.forEach { t ->
                            val of = all.filter { it.theme == t.slug }
                            if (of.isNotEmpty()) {
                                heading(t.name, t.slug)
                                cells(t.slug, of)
                            }
                        }
                    }
                }
            }
            if (themes.isNotEmpty() && query.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    themes.forEach { t ->
                        FilterChip(
                            selected = false,
                            onClick = {
                                raw = ""
                                query = ""
                                headings[t.slug]?.let { scope.launch { grid.animateScrollToItem(it) } }
                            },
                            label = { Text(t.name) },
                        )
                    }
                }
            }
        }
    }
}

private fun LazyGridScope.stampCells(section: String, stamps: List<StampView>, current: StampMark?, base: String, pick: (StampView) -> Unit) {
    items(stamps, key = { "$section/${it.slug}" }) { s ->
        val on = s.slug == current?.slug
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(300f / 380f)
                .clip(RoundedCornerShape(6.dp))
                .then(if (on) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)) else Modifier)
                .background(if (on) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                .clickable { pick(s) }
                .semantics {
                    contentDescription = s.name
                    selected = on
                }
                .padding(6.dp),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(model = resolveMedia(base, s.imageUrl), contentDescription = null, contentScale = ContentScale.Fit)
        }
    }
}
