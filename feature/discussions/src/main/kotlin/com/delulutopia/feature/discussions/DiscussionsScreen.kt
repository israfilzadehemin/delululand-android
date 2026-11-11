package com.delulutopia.feature.discussions

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.motion.rememberPlayed
import com.delulutopia.core.designsystem.motion.rowEnter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.NoteColor
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.DRow
import com.delulutopia.core.ui.ArchivedRoute
import com.delulutopia.core.ui.ComposeDiscussionRoute
import com.delulutopia.core.ui.DiscussionRoute
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NewChip
import com.delulutopia.core.ui.Note
import com.delulutopia.core.ui.leaves.SurfaceKind
import com.delulutopia.core.ui.leaves.leafSurface
import com.delulutopia.core.ui.noteColorOf
import com.delulutopia.core.ui.noteInk
import com.delulutopia.core.ui.noteInkQuiet
import com.delulutopia.core.ui.reactions.noticeLine
import com.delulutopia.core.ui.reactions.reactionWins
import com.delulutopia.core.ui.rememberNow

/**
 * S10 Discussions and the Archived list (web discussions/DiscussionsScreen.tsx; canvas
 * D-Discussions, K-Archive-list): big sticky notes, newest activity first, "Archived · N" at the
 * top, the round button to start one.
 */
@Composable
fun DiscussionsScreen(archived: Boolean) {
    val vm = hiltViewModel<DiscussionsViewModel, DiscussionsViewModel.Factory>(key = "discussions-$archived", creationCallback = { it.create(archived) })
    val home by vm.home.collectAsStateWithLifecycle()
    DiscussionsList(
        archived = archived,
        rows = if (vm.head == null && vm.rows.isEmpty()) null else vm.rows,
        hasMore = vm.nextCursor != null,
        archivedCount = vm.archivedCount,
        me = home.home?.me,
        friend = home.home?.friend,
        onMore = vm::loadMore,
        onFlipped = vm::refresh,
    )
}

@Composable
fun DiscussionsList(
    archived: Boolean,
    rows: List<DRow>?,
    hasMore: Boolean,
    archivedCount: Int,
    me: Citizen?,
    friend: Citizen?,
    onMore: () -> Unit,
    onFlipped: () -> Unit,
) {
    val nav = LocalAppNav.current
    val nowMs = rememberNow()
    val draftEmpty = me?.discussionDraftEmpty ?: true
    val startLabel = if (draftEmpty) "Start a discussion" else "Continue your discussion draft"
    val bar: @Composable () -> Unit = {
        // web .dl-list-bar--discussions: a quiet 36 dp link, pulled 6 dp into the gaps above and below.
        Row(
            Modifier.fillMaxWidth().layout { m, k ->
                val p = m.measure(k)
                val pull = 6.dp.roundToPx()
                layout(p.width, p.height - 2 * pull) { p.place(0, -pull) }
            },
            horizontalArrangement = if (archived) Arrangement.Start else Arrangement.End,
        ) {
            if (archived) {
                ArchivedLink(WebIcons.Back, "Discussions", "Back to Discussions", Modifier.offset(x = (-10).dp)) { nav.back() }
            } else {
                ArchivedLink(WebIcons.Archive, "Archived · $archivedCount", "Archived discussions: $archivedCount", Modifier.offset(x = 10.dp)) {
                    nav.open(ArchivedRoute)
                }
            }
        }
    }
    if (rows == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(4) { Box(Modifier.fillMaxWidth().height(80.dp).background(DlTheme.colors.letterEnvelope, RoundedCornerShape(2.dp))) }
        }
        return
    }
    LaunchedEffect(rows.any { dStateAt(it, nowMs) != it.state }) { if (rows.any { dStateAt(it, nowMs) != it.state }) onFlipped() }
    if (rows.isEmpty() && !hasMore) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            if (archived || archivedCount > 0) bar()
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            ) {
                EmptyNotes()
                if (archived) {
                    Text("Nothing archived yet.", style = MaterialTheme.typography.titleMedium)
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("No discussions yet.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Start the first one.",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DlFilledButton(startLabel, { nav.open(ComposeDiscussionRoute) })
                }
            }
        }
        return
    }
    val list = rememberLazyListState()
    val played = rememberPlayed()
    LaunchedEffect(list, hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (hasMore && last >= list.layoutInfo.totalItemsCount - 3) onMore()
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            // web .dl-list gap 8 + the note's 6 px top margin.
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { bar() }
            items(rows, key = { it.id }) { r ->
                val index = rows.indexOf(r)
                Box(Modifier.animateItem().rowEnter(index, played, r.id, r.state).leafSurface("/discussions/${r.id}")) {
                    NoteRow(
                        r,
                        nowMs,
                        authorName = if (r.mine) me?.nickname ?: "Me" else friend?.nickname ?: "Friend",
                        friendName = friend?.nickname ?: "Friend",
                        tilt = if (index % 2 == 0) -0.6f else 0.5f,
                        archived = archived,
                    )
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 40.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (hasMore) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(
                            "Loading older discussions…",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            "That is everything",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
        if (!archived) {
            DlFab(DlIcons.NotePlus, startLabel, {
                nav.open(ComposeDiscussionRoute)
            }, Modifier.align(Alignment.BottomEnd).padding(16.dp).leafSurface(kind = SurfaceKind.Round), hasDraft = !draftEmpty)
        }
    }
}

/**
 * FS-D3–D5: title, "[Author] · Posted HH:MM · N comments", one chip, the unseen ring. Unseen
 * reactions count in "N new" and replace the second line while they are the newest activity.
 */
@Composable
private fun NoteRow(r: DRow, nowMs: Long, authorName: String, friendName: String, tilt: Float, archived: Boolean) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val state = dStateAt(r, nowMs)
    val arrived = state == LetterState.ARRIVED
    val hidden = hiddenRow(r, nowMs)
    val reacted = r.unseenReactions
    val fresh = r.unseenComments + reacted
    val unseen = fresh > 0
    val comments = commentWords(r.commentCount)
    val lastActivity = r.lastCommentAt ?: if (r.mine) r.postedAt else r.arrivesAt
    val active = r.activityAt?.takeIf { (Time.ms(it) ?: 0) > (Time.ms(r.postedAt) ?: 0) }
    val whenText = if (active != null) "Active ${Time.clock(active, nowMs)}" else "Posted ${Time.clock(r.postedAt, nowMs)}"
    val meta = if (reactionWins(r.lastReaction, reacted, lastActivity)) {
        noticeLine(r.lastReaction!!, reacted, friendName)
    } else {
        "$authorName · $whenText${if (r.editedAt != null) " · edited" else ""}${if (comments.isNotEmpty()) " · $comments" else ""}"
    }
    val chip: @Composable () -> Unit = {
        when {
            r.mine && state == LetterState.IN_REACH -> Box(
                Modifier.height(28.dp).background(DlTheme.colors.countdownChip, RoundedCornerShape(14.dp)).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "${Time.countdown((Time.ms(r.windowEndsAt) ?: 0) - nowMs)} to take back",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = DlTheme.colors.letterInk,
                )
            }
            !arrived -> Text(Time.countdown((Time.ms(r.arrivesAt) ?: 0) - nowMs), style = DlTheme.type.countdown.copy(fontSize = 16.sp), color = noteInk)
            r.isNew -> NewChip()
            unseen -> NewChip(text = "$fresh new")
        }
    }
    if (hidden) {
        val arrives = Time.clock(r.arrivesAt, nowMs)
        Note(
            NoteColor.Blank,
            Modifier.semantics { contentDescription = "A discussion is on its way, arrives $arrives" },
            tilt = tilt,
            onClick = { notice("Arrives at $arrives") },
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("A discussion is on its way", style = NoteLine, color = noteInkQuiet, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("$authorName · arrives $arrives", style = NoteMeta, color = noteInkQuiet)
                }
                chip()
            }
        }
        return
    }
    Note(noteColorOf(r.color), unseen = unseen, tilt = tilt, onClick = { nav.open(DiscussionRoute(r.id)) }) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                // web .dl-list--discussions .dl-note .dl-row-line: Literata 17/18, 600, one line.
                Text(
                    r.title.orEmpty(),
                    style = NoteLine,
                    color = noteInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    meta,
                    style = NoteMeta,
                    color = noteInkQuiet,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            chip()
        }
    }
}

@Composable
private fun EmptyNotes() {
    val line = MaterialTheme.colorScheme.outlineVariant
    val face = DlTheme.colors.notes.getValue(NoteColor.Lemon).face
    Canvas(Modifier.size(120.dp, 96.dp)) {
        scale(size.width / 120f, size.height / 96f, pivot = Offset.Zero) {
            val note = Path().apply {
                moveTo(26f, 18f)
                lineTo(94f, 14f)
                lineTo(98f, 82f)
                lineTo(30f, 86f)
                close()
            }
            drawPath(note, face)
            drawPath(note, line, style = Stroke(1.5f))
            for (y in listOf(40f, 52f, 64f)) drawLine(line, Offset(40f, y), Offset(84f, y - 2f), 1.5f)
        }
    }
}

private val NoteLine @Composable get() = DlTheme.type.noteTitle.copy(fontSize = 17.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold)
private val NoteMeta @Composable get() = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp)

/** web .dl-archived-link: icon and label in quiet ink, 600 13 px, a 36 dp pill on press. */
@Composable
private fun ArchivedLink(icon: ImageVector, text: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier
            .heightIn(min = 36.dp)
            .pressScale(source)
            .clip(RoundedCornerShape(18.dp))
            .clickable(interactionSource = source, indication = ripple(), onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = description }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
