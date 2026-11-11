package com.delulutopia.feature.letters

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.LettersRepository
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Row as LetterRow
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LetterTile
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NewChip
import com.delulutopia.core.ui.StampImage
import com.delulutopia.core.ui.TileShape
import com.delulutopia.core.ui.WaxSeal
import com.delulutopia.core.ui.Who
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.firstLineOf
import com.delulutopia.core.ui.leaves.SurfaceKind
import com.delulutopia.core.ui.leaves.leafSurface
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signature
import com.delulutopia.core.ui.tileShapeOf
import com.delulutopia.core.ui.whoOf

/**
 * S6 Letters (FS-R11, FS-R12; web letters/LettersScreen.tsx; canvas C-Letters, C-Letters-list):
 * every letter both ways, newest first, 20 per page by keyset. Two-column thumbnails (the default)
 * or rows; the toggle at the top-right flips it and remembers it on this phone. Write opens from
 * the round button at the bottom-right.
 */
@Composable
fun LettersScreen(vm: LettersViewModel = hiltViewModel()) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val home by vm.home.collectAsStateWithLifecycle()
    val list by vm.list.collectAsStateWithLifecycle()
    LettersList(rows, home.home?.me, home.home?.friend, list, onLayout = vm::setList, onMore = vm::loadMore)
}

@Composable
fun LettersList(
    state: LettersRepository.State,
    me: Citizen?,
    friend: Citizen?,
    list: Boolean,
    onLayout: (Boolean) -> Unit,
    onMore: () -> Unit,
) {
    val nav = LocalAppNav.current
    val nowMs = rememberNow()
    val draft = me?.draftEmpty == false
    val rows = state.rows
    if (!state.loaded) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            LayoutBar(list, onLayout)
            repeat(4) {
                Box(
                    Modifier.fillMaxWidth().height(
                        if (list) 72.dp else 176.dp,
                    ).padding(vertical = 6.dp).background(DlTheme.colors.letterEnvelope, RoundedCornerShape(4.dp)),
                )
            }
        }
        return
    }
    if (rows.isEmpty() && !state.hasMore) {
        EmptyLetters(draft) { nav.open(WriteRoute()) }
        return
    }
    val grid = rememberLazyGridState()
    LaunchedEffect(grid, state.hasMore) {
        snapshotFlow { grid.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (state.hasMore && last >= grid.layoutInfo.totalItemsCount - 3) onMore()
        }
    }
    val friendName = friend?.nickname ?: "Friend"
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (list) 1 else 2),
            state = grid,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(if (list) 10.dp else 14.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { LayoutBar(list, onLayout) }
            itemsIndexed(rows, key = { _, r -> r.id }) { i, row ->
                val sender = if (row.direction == "to") me?.id ?: 1 else friend?.id ?: 2
                Box(Modifier.leafSurface("/letters/${row.id}")) {
                    if (list) ListRow(row, nowMs, friendName, whoOf(sender)) else Thumb(row, nowMs, friendName, whoOf(sender), i)
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (state.hasMore) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Loading older letters…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        Text("That is everything", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        DlFab(
            DlIcons.Pen,
            if (draft) "Continue your draft" else "Write a letter",
            { nav.open(WriteRoute()) },
            Modifier.align(Alignment.BottomEnd).padding(16.dp).leafSurface(kind = SurfaceKind.Round),
            hasDraft = draft,
        )
    }
}

/** The layout toggle at the list's top-right; its icon shows the layout it switches to. */
@Composable
private fun LayoutBar(list: Boolean, onLayout: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        IconButton(onClick = { onLayout(!list) }) {
            Icon(if (list) WebIcons.Grid else WebIcons.List, if (list) "Show as thumbnails" else "Show as list")
        }
    }
}

@Composable
private fun EmptyLetters(draft: Boolean, onWrite: () -> Unit) {
    val line = MaterialTheme.colorScheme.outlineVariant
    val letter = DlTheme.colors.letterSheet
    val seal = MaterialTheme.colorScheme.primary
    val tint = MaterialTheme.colorScheme.secondaryContainer
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Canvas(Modifier.size(120.dp, 96.dp)) {
            scale(size.width / 120f, size.height / 96f, pivot = Offset.Zero) {
                drawRoundRect(letter, Offset(14f, 30f), Size(92f, 58f), CornerRadius(6f))
                drawRoundRect(line, Offset(14f, 30f), Size(92f, 58f), CornerRadius(6f), style = Stroke(1.5f))
                drawPath(
                    Path().apply {
                        moveTo(14f, 36f)
                        lineTo(60f, 66f)
                        lineTo(106f, 36f)
                    },
                    line,
                    style = Stroke(1.5f),
                )
                val plane = Path().apply {
                    moveTo(60f, 8f)
                    lineTo(94f, 28f)
                    lineTo(74f, 36f)
                    lineTo(68f, 50f)
                    close()
                }
                drawPath(plane, tint)
                drawPath(plane, seal, style = Stroke(1.5f))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("No letters yet.", style = MaterialTheme.typography.titleMedium)
            Text("Write the first one.", style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DlFilledButton(if (draft) "Continue your draft" else "Write a letter", onWrite)
    }
}

private fun arrivedOf(state: String) = state == LetterState.DELIVERED || state == LetterState.READ

@Composable
private fun Direction(text: String, who: Who) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        color = if (who == Who.A) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** The state chip shared by rows and tiles (FS-R11): in reach, countdown, NEW, Opened, Read, Delivered. */
@Composable
internal fun StatusChip(row: LetterRow, state: String, nowMs: Long) {
    val to = row.direction == "to"
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val chip = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    when {
        to && state == LetterState.IN_REACH -> Box(
            Modifier.height(28.dp).background(DlTheme.colors.countdownChip, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${Time.countdown((Time.ms(row.windowEndsAt) ?: 0) - nowMs)} to edit",
                style = chip.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                color = DlTheme.colors.letterInk,
            )
        }
        !arrivedOf(
            state,
        ) -> Text(
            Time.countdown((Time.ms(row.arrivesAt) ?: 0) - nowMs),
            style = DlTheme.type.countdown.copy(fontSize = 16.sp),
            color = DlTheme.colors.letterInk,
        )
        !to && state == LetterState.DELIVERED -> NewChip()
        !to -> Text("Opened ${Time.clock(row.readAt, nowMs)}", style = chip, color = quiet)
        state == LetterState.READ -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(WebIcons.DoubleCheck, null, Modifier.size(16.dp), tint = quiet)
            Text("Read ${Time.clock(row.readAt, nowMs)}", style = chip, color = quiet)
        }
        else -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(WebIcons.Check, null, Modifier.size(16.dp), tint = quiet)
            Text("Delivered", style = chip, color = quiet)
        }
    }
}

/** Replies B2: the row's letter answers another one. */
@Composable
private fun ReplyMark() {
    Icon(WebIcons.Reply, "Reply to a letter", Modifier.size(12.dp).offset(y = 1.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ListRow(row: LetterRow, nowMs: Long, friendName: String, who: Who) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val state = LetterState.of(nowMs, row.windowEndsAt, row.arrivesAt, row.readAt)
    val arrived = arrivedOf(state)
    val to = row.direction == "to"
    val incoming = !to && !arrived
    val isNew = !to && state == LetterState.DELIVERED
    val arrives = Time.clock(row.arrivesAt, nowMs)
    LetterTile(
        tileShapeOf(row, state),
        who,
        onClick = { if (incoming) notice("Arrives at $arrives") else nav.open(LetterRoute(row.id)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Direction(if (to) "→ To $friendName" else "← From $friendName", who)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!incoming && row.replyToId != null) ReplyMark()
                    Text(
                        if (incoming) "On its way" else firstLineOf(row),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            fontWeight = if (isNew) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (incoming) MaterialTheme.colorScheme.onSurfaceVariant else DlTheme.colors.letterInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    "Sent ${Time.clock(row.sentAt, nowMs)} · ${if (arrived) "Arrived" else "Arrives"} $arrives",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            row.stamp?.let { StampImage(it) }
            StatusChip(row, state, nowMs)
        }
    }
}

private val TILT = floatArrayOf(-0.7f, 0.6f, 0.5f, -0.5f)

/**
 * One letter as a thumbnail, drawn as an envelope (flap, wax seal, a postmark across the stamp) or a
 * letter sheet (margin, four ruled lines, a folded corner). Nothing of an incoming letter shows
 * before it arrives; tapping it only repeats the ETA.
 */
@Composable
private fun Thumb(row: LetterRow, nowMs: Long, friendName: String, who: Who, index: Int) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val c = DlTheme.colors
    val state = LetterState.of(nowMs, row.windowEndsAt, row.arrivesAt, row.readAt)
    val arrived = arrivedOf(state)
    val to = row.direction == "to"
    val incoming = !to && !arrived
    val isNew = !to && state == LetterState.DELIVERED
    val shape = tileShapeOf(row, state)
    val media = if (incoming) null else row.media
    val shapeR = RoundedCornerShape(4.dp)
    val seal = who.signature()
    val hue = c.postmarkHue
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 176.dp)
            .rotate(TILT[index % 4])
            .shadow(2.dp, shapeR)
            .clip(shapeR)
            .background(if (shape == TileShape.Envelope) c.letterEnvelope else c.letterSheet)
            .then(
                if (shape == TileShape.Sheet) {
                    Modifier.drawBehind {
                        drawLine(c.letterMargin, Offset(7.dp.toPx(), 0f), Offset(7.dp.toPx(), size.height), 1.dp.toPx())
                    }
                } else {
                    Modifier
                },
            )
            .clickable { if (incoming) notice("Arrives at ${Time.clock(row.arrivesAt, nowMs)}") else nav.open(LetterRoute(row.id)) }
            .padding(start = 12.dp, end = 10.dp, top = if (shape == TileShape.Envelope) 0.dp else 10.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (shape == TileShape.Envelope) {
            Box(Modifier.fillMaxWidth().height(78.dp)) {
                Canvas(Modifier.padding(start = 0.dp).fillMaxWidth().height(62.dp).offset(x = (-12).dp)) {
                    val w = size.width + 22.dp.toPx()
                    val h = size.height
                    val flap = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w * 0.57f, h * 0.86f)
                        quadraticTo(w * 0.5f, h * 0.99f, w * 0.43f, h * 0.86f)
                        close()
                    }
                    drawPath(flap, c.letterFlap)
                    drawPath(flap, c.letterEdge, style = Stroke(1.dp.toPx()))
                }
                WaxSeal(seal, Modifier.align(Alignment.TopCenter).offset(y = 44.dp), size = 26.dp)
                row.stamp?.let {
                    StampImage(it, Modifier.align(Alignment.TopEnd).offset(y = 8.dp).rotate(2f))
                    Canvas(Modifier.align(Alignment.TopEnd).offset(x = (-22).dp, y = 18.dp).size(48.dp, 30.dp)) {
                        scale(size.width / 48f, size.height / 30f, pivot = Offset.Zero) {
                            val st = Stroke(1.4f)
                            drawCircle(hue.copy(alpha = 0.7f), 12f, Offset(15f, 15f), style = st)
                            drawCircle(hue.copy(alpha = 0.7f), 8.5f, Offset(15f, 15f), style = st)
                            for (y in listOf(9f, 15f, 21f)) {
                                drawPath(
                                    Path().apply {
                                        moveTo(27f, y)
                                        quadraticTo(31.25f, y - 3f, 35.5f, y)
                                        quadraticTo(39.75f, y + 3f, 44f, y)
                                    },
                                    hue.copy(alpha = 0.7f),
                                    style = st,
                                )
                            }
                        }
                    }
                }
            }
            Direction(if (to) "→ To $friendName" else "← From $friendName", who)
        } else {
            Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Box(Modifier.weight(1f).padding(top = 2.dp)) { Direction(if (to) "→ To $friendName" else "← From $friendName", who) }
                row.stamp?.let { StampImage(it) }
            }
        }
        val rule = c.letterRule
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (shape == TileShape.Sheet) {
                        Modifier.heightIn(min = 76.dp).drawBehind {
                            val step = 19.sp.toPx()
                            var y = 18.sp.toPx()
                            while (y < size.height) {
                                drawLine(rule, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                                y += step
                            }
                        }
                    } else {
                        Modifier
                    },
                ),
        ) {
            if (!incoming && row.replyToId != null) ReplyMark()
            Text(
                if (incoming) "On its way" else firstLineOf(row),
                style = if (incoming) {
                    MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp)
                } else {
                    DlTheme.type.letterBody.copy(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = if (isNew) FontWeight.SemiBold else FontWeight.Normal)
                },
                color = if (incoming) MaterialTheme.colorScheme.onSurfaceVariant else c.letterInk,
                maxLines = if (shape == TileShape.Envelope) 2 else 4,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (media != null && (media.photos > 0 || media.audios > 0 || media.doodles > 0)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                val quiet = MaterialTheme.colorScheme.onSurfaceVariant
                val small = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                if (media.photos > 0) {
                    Row(
                        Modifier.semantics {
                            contentDescription = if (media.photos ==
                                1
                            ) {
                                "1 photo"
                            } else {
                                "${media.photos} photos"
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        Icon(WebIcons.PhotoSmall, null, Modifier.size(14.dp), tint = quiet)
                        Text("${media.photos}", style = small, color = quiet)
                    }
                }
                if (media.audios > 0) Icon(WebIcons.MicSmall, "Voice note", Modifier.size(14.dp), tint = quiet)
                if (media.doodles > 0) Icon(WebIcons.DoodleSmall, "Doodle", Modifier.size(14.dp), tint = quiet)
            }
        }
        Spacer(Modifier.weight(1f, fill = false))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Sent ${Time.clock(row.sentAt, nowMs)}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StatusChip(row, state, nowMs)
        }
    }
}
