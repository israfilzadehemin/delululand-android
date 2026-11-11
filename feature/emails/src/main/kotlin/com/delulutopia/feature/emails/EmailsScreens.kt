package com.delulutopia.feature.emails

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlMailPill
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.motion.float
import com.delulutopia.core.designsystem.motion.rememberPlayed
import com.delulutopia.core.designsystem.motion.rowEnter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Email
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.ThreadRow
import com.delulutopia.core.ui.Lightbox
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.NewEmailRoute
import com.delulutopia.core.ui.ThreadRoute
import com.delulutopia.core.ui.leaves.SurfaceKind
import com.delulutopia.core.ui.leaves.leafSurface
import com.delulutopia.core.ui.media.AudioPlayer
import com.delulutopia.core.ui.media.VideoPlayer
import com.delulutopia.core.ui.reactions.ReactionSummary
import com.delulutopia.core.ui.reactions.Reactions
import com.delulutopia.core.ui.reactions.noticeLine
import com.delulutopia.core.ui.reactions.reactionWins
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tintOf

/** S13 Emails (web emails/EmailsScreen.tsx; canvas E-Emails, E-Emails-empty, E-No-credits). */
@Composable
fun EmailsScreen(vm: EmailsViewModel = hiltViewModel()) {
    val home by vm.home.collectAsStateWithLifecycle()
    EmailsList(vm.head?.let { vm.rows }, vm.nextCursor != null, vm.draftEmpty, home.home, vm::loadMore)
}

@Composable
fun EmailsList(rows: List<ThreadRow>?, hasMore: Boolean, draftEmpty: Boolean, home: Home?, onMore: () -> Unit) {
    val nav = LocalAppNav.current
    val nowMs = rememberNow(30_000)
    val newLabel = if (draftEmpty) "New email" else "Continue your new email"
    val credits = home?.emailCredits
    val line = creditsLine(credits)
    if (rows == null) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(4) { Box(Modifier.fillMaxWidth().height(72.dp).background(DlTheme.colors.mailBackground, RoundedCornerShape(12.dp))) }
        }
        return
    }
    if (rows.isEmpty() && !hasMore) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(WebIcons.Emails, null, Modifier.enter(Enter.Rise, durationMs = 400).float(400).size(64.dp), tint = MaterialTheme.colorScheme.outlineVariant)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("No emails yet.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Start the first one.",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            line?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            DlFilledButton(newLabel, { nav.open(NewEmailRoute) })
        }
        return
    }
    val list = rememberLazyListState()
    val played = rememberPlayed()
    LaunchedEffect(list, hasMore) {
        snapshotFlow { list.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect {
            if (hasMore &&
                it >= list.layoutInfo.totalItemsCount - 3
            ) {
                onMore()
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = list,
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (line != null) {
                item {
                    Text(
                        line,
                        Modifier.padding(vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = if (credits?.left == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(rows, key = { _, it -> it.id }) { i, it ->
                Box(Modifier.animateItem().rowEnter(i, played, it.id).leafSurface("/emails/${it.id}")) { ThreadCard(it, nowMs, home) }
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                    if (hasMore) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Loading older threads…", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text("That is everything", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        DlFab(DlIcons.EnvelopePlus, newLabel, {
            nav.open(NewEmailRoute)
        }, Modifier.align(Alignment.BottomEnd).padding(16.dp).leafSurface(kind = SurfaceKind.Round), hasDraft = !draftEmpty)
    }
}

/** FS-E3: subject (bold when unread), "[sender] → [recipient] · HH:MM · N emails", the snippet, "N new". */
@Composable
private fun ThreadCard(row: ThreadRow, nowMs: Long, home: Home?) {
    val nav = LocalAppNav.current
    val c = DlTheme.colors
    val mineLast = row.lastSenderId == home?.me?.id
    val sender = if (mineLast) home?.me?.nickname else home?.friend?.nickname
    val recipient = if (mineLast) home?.friend?.nickname else home?.me?.nickname
    val meta = "${sender.orEmpty()} → ${recipient.orEmpty()} · ${Time.clock(row.lastEmailAt, nowMs)} · ${emailCountWords(row.emailCount)}"
    val reacted = row.unseenReactions
    val snippet = if (reactionWins(row.lastReaction, reacted, row.lastEmailAt)) {
        noticeLine(row.lastReaction!!, reacted, home?.friend?.nickname ?: "Friend")
    } else {
        row.lastText ?: row.lastMedia?.let { mediaWords(it.photos, it.audios, it.videos) } ?: ""
    }
    val fresh = row.unread + reacted
    val unread = fresh > 0
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (unread) c.mailUnread else c.mailBackground)
            .border(1.dp, c.mailLine, RoundedCornerShape(12.dp))
            .clickable { nav.open(ThreadRoute(row.id)) }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(c.mailBlue.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(WebIcons.Emails, null, Modifier.size(20.dp), tint = c.mailBlue)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (unread) Box(Modifier.size(8.dp).background(c.mailBlue, CircleShape))
                Text(
                    row.subject,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = if (unread) FontWeight.Bold else FontWeight.Medium),
                    color = c.mailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(meta, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            if (snippet.isNotEmpty()) {
                Text(
                    snippet,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (unread) DlMailPill("$fresh new")
    }
}

/** S14 (web emails/ThreadView.tsx; canvas E-Thread). */
@Composable
fun ThreadScreen(id: Long) {
    val vm = hiltViewModel<ThreadViewModel, ThreadViewModel.Factory>(key = "thread-$id", creationCallback = { it.create(id) })
    val nav = LocalAppNav.current
    val homeState by vm.home.collectAsStateWithLifecycle()
    val home = homeState.home
    Column(Modifier.fillMaxSize().imePadding()) {
        when (val ui = vm.ui) {
            ThreadViewModel.Ui.Loading -> {
                DlTopAppBar(title = "", onBack = { nav.back() }, backDescription = "Back to Emails")
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            ThreadViewModel.Ui.Gone, ThreadViewModel.Ui.Unreachable -> {
                DlTopAppBar(title = "Email", onBack = { nav.back() }, backDescription = "Back to Emails")
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text(if (ui == ThreadViewModel.Ui.Gone) "This thread isn't here." else "Can't reach ${LocalBrand.current}. Try again.")
                    DlOutlinedButton("Back", { nav.back() })
                }
            }
            is ThreadViewModel.Ui.Shown -> {
                DlTopAppBar(title = ui.t.thread.subject, onBack = { nav.back() }, backDescription = "Back to Emails")
                ThreadBody(vm, home?.me, home?.friend, home)
            }
        }
    }
}

@Composable
private fun ThreadBody(vm: ThreadViewModel, me: Citizen?, friend: Citizen?, home: Home?) {
    val nowMs = rememberNow(30_000)
    val items = vm.items
    val newestId = items.firstOrNull()?.id
    var toggled by remember { mutableStateOf(setOf<Long>()) }
    var photo by remember { mutableStateOf<Pair<List<Att>, Int>?>(null) }
    val myNewest = items.firstOrNull { it.senderId == me?.id }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComposerView(vm.composer, home?.emailCredits) { sent ->
            toggled = emptySet()
            vm.sent(sent.email)
        }
        items.forEach { e ->
            val open = (e.id == newestId) != (e.id in toggled)
            EmailCard(e, open, me, friend, nowMs, onToggle = {
                toggled = if (e.id in
                    toggled
                ) {
                    toggled - e.id
                } else {
                    toggled + e.id
                }
            }, onPhoto = { list, i -> photo = list to i }, onReacted = vm::reacted)
            if (e.id == myNewest?.id && e.readAt != null) {
                Text(
                    "Seen ${Time.clock(e.readAt, nowMs)}",
                    Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (vm.cursor != null) {
            DlOutlinedButton(if (vm.loadingOlder) "Loading older emails…" else "See older", vm::seeOlder, Modifier.fillMaxWidth(), enabled = !vm.loadingOlder)
        } else if (items.size > 20) {
            Text("That is everything", style = MaterialTheme.typography.bodySmall)
        }
        if (vm.olderFailed) Text("Couldn't load older emails. Try again.", color = MaterialTheme.colorScheme.error)
        Box(Modifier.height(24.dp))
    }
    photo?.let { (list, i) -> Lightbox(list, i, { photo = list to it }, { photo = null }) }
}

@Composable
private fun EmailCard(
    e: Email,
    open: Boolean,
    me: Citizen?,
    friend: Citizen?,
    nowMs: Long,
    onToggle: () -> Unit,
    onPhoto: (List<Att>, Int) -> Unit,
    onReacted: () -> Unit,
) {
    val c = DlTheme.colors
    val base = LocalServerBaseUrl.current
    val person = if (e.senderId == me?.id) {
        me
    } else if (e.senderId == friend?.id) {
        friend
    } else {
        null
    }
    val mine = e.senderId == me?.id
    val photos = e.attachments.filter { it.kind == "photo" }
    val audios = e.attachments.filter { it.kind == "audio" }
    val video = e.attachments.firstOrNull { it.kind == "video" }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (mine) c.mailMine else c.mailBackground)
            .border(1.dp, c.mailLine, RoundedCornerShape(12.dp))
            // Fold & open (Motion doc): the card's height eases instead of jumping.
            .animateContentSize(tween(260)),
    ) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(12.dp).semantics { contentDescription = if (open) "Fold" else "Unfold" },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (person !=
                null
            ) {
                DlRemoteAvatar(
                    person.avatarUrl,
                    person.nickname,
                    tintOf(person.id),
                    size = 24.dp,
                    initials = person.nickname.take(1).uppercase(),
                    ring = signatureOf(person.id),
                )
            }
            Text(person?.nickname.orEmpty(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = c.mailInk)
            if (open) {
                Text(
                    "to ${if (mine) friend?.nickname.orEmpty() else me?.nickname.orEmpty()}",
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val t = e.text.replace(Regex("\\s+"), " ").trim()
                val snippet = t.ifEmpty { mediaWords(photos.size, audios.size, if (video != null) 1 else 0).orEmpty() }
                Text(
                    snippet,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val icons = listOfNotNull(
                    if (photos.isNotEmpty()) "📷${if (photos.size > 1) " ${photos.size}" else ""}" else null,
                    if (audios.isNotEmpty()) "🎙${if (audios.size > 1) " ${audios.size}" else ""}" else null,
                    if (video != null) "🎬" else null,
                )
                if (icons.isNotEmpty()) Text(icons.joinToString(" "), fontSize = 12.sp)
                ReactionSummary(e.reactions)
            }
            Text(Time.clock(e.sentAt, nowMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (open) {
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (e.text.isNotEmpty()) Text(e.text, style = MaterialTheme.typography.bodyLarge, color = c.mailInk)
                video?.let { v -> VideoPlayer(resolveMedia(base, v.url), v.thumbUrl?.let { resolveMedia(base, it) }, v.durationMs, v.width, v.height) }
                if (photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.forEachIndexed { i, p ->
                            AsyncImage(
                                model = resolveMedia(base, p.thumbUrl ?: p.url),
                                contentDescription = "Photo ${i + 1} of ${photos.size}, open full size",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(4.dp)).clickable { onPhoto(photos, i) },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                audios.forEachIndexed { i, a ->
                    AudioPlayer(
                        resolveMedia(base, a.url),
                        a.durationMs,
                        label = if (audios.size >
                            1
                        ) {
                            "voice note ${i + 1}"
                        } else {
                            "voice note"
                        },
                    )
                }
                Reactions("emails", e.id, e.reactions, me, friend, chipColour = c.mailBackground, onChanged = onReacted)
            }
        }
    }
}

/** S15 New email (web emails/NewEmailScreen.tsx; canvas E-New). */
@Composable
fun NewEmailScreen(vm: NewEmailViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val home by vm.home.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().imePadding()) {
        DlTopAppBar(title = "New email", onBack = { nav.back() }, backDescription = "Back to Emails")
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
            ComposerView(vm.composer, home.home?.emailCredits) { sent ->
                nav.replace(ThreadRoute(sent.thread.id))
            }
        }
    }
}
