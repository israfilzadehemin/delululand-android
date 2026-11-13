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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlGhostButton
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
import com.delulutopia.core.i18n.formatNumber
import com.delulutopia.core.i18n.t
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
import com.delulutopia.core.ui.reactions.ReactionSurface
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
    val newLabel = if (draftEmpty) t("emails.newEmail") else t("emails.continueNewEmail")
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
                Text(t("emails.noEmailsYet"), style = MaterialTheme.typography.titleMedium)
                Text(
                    t("emails.startTheFirstOne"),
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            // web .dl-list: 8 dp between cards.
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (line != null) {
                item {
                    Text(
                        line,
                        Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = if (credits?.left == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            itemsIndexed(rows, key = { _, it -> it.id }) { i, it ->
                Box(Modifier.animateItem().rowEnter(i, played, it.id).leafSurface("/emails/${it.id}")) { ThreadCard(it, nowMs, home) }
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
                            t("emails.loadingOlderThreads"),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            t("emails.thatIsEverything"),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
    val meta = t(
        "emails.meta",
        "sender" to sender.orEmpty(),
        "recipient" to recipient.orEmpty(),
        "time" to Time.clock(row.lastEmailAt, nowMs),
        "count" to emailCountWords(row.emailCount),
    )
    val reacted = row.unseenReactions
    val snippet = if (reactionWins(row.lastReaction, reacted, row.lastEmailAt)) {
        noticeLine(row.lastReaction!!, reacted, home?.friend?.nickname ?: t("common.friend"))
    } else {
        row.lastText ?: row.lastMedia?.let { mediaWords(it.photos, it.audios, it.videos) } ?: ""
    }
    val fresh = row.unread + reacted
    val unread = fresh > 0
    // web .dl-row.dl-mail: a 10 dp mail card, 10/12 padding; unread is tinted with a 3 dp blue edge.
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(if (unread) c.mailUnread else c.mailBackground)
            .drawBehind { if (unread) drawRect(c.mailBlue, size = Size(3.dp.toPx(), size.height)) }
            .border(1.dp, c.mailLine, shape)
            .clickable { nav.open(ThreadRoute(row.id)) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(c.mailBlue.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(WebIcons.Emails, null, Modifier.size(18.dp), tint = c.mailBlue)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (unread) Box(Modifier.size(8.dp).background(c.mailBlue, CircleShape))
                Text(
                    row.subject,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        lineHeight = 18.sp,
                        fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = c.mailInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                meta,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (snippet.isNotEmpty()) {
                Text(
                    snippet,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (unread) DlMailPill(t("emails.new", "fresh" to fresh))
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
                DlTopAppBar(title = "", onBack = { nav.back() }, backDescription = t("emails.backToEmails"))
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            ThreadViewModel.Ui.Gone, ThreadViewModel.Ui.Unreachable -> {
                DlTopAppBar(title = t("emails.email"), onBack = { nav.back() }, backDescription = t("emails.backToEmails"))
                Column(
                    Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Text(if (ui == ThreadViewModel.Ui.Gone) t("emails.threadGone") else t("common.cantReachTryAgain", "brand" to LocalBrand.current))
                    DlOutlinedButton(t("emails.back"), { nav.back() })
                }
            }
            is ThreadViewModel.Ui.Shown -> {
                // web .dl-ethread-subject: Literata 18/700.
                DlTopAppBar(
                    title = ui.t.thread.subject,
                    onBack = { nav.back() },
                    backDescription = t("emails.backToEmails"),
                    titleStyle = DlTheme.type.noteTitle.copy(fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold),
                )
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
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // web .dl-ethread-reply: 4/16/12 around the reply composer.
        Box(Modifier.padding(top = 4.dp, bottom = 4.dp)) {
            ComposerView(vm.composer, home?.emailCredits) { sent ->
                toggled = emptySet()
                vm.sent(sent.email)
            }
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
            }, onPhoto = { list, i -> photo = list to i }, onReacted = vm::reacted, seen = e.readAt?.takeIf { e.id == myNewest?.id })
        }
        // web .dl-emails-end: centred, 12 dp around.
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (vm.cursor != null) {
                DlGhostButton(if (vm.loadingOlder) t("emails.loadingOlder") else t("emails.seeOlder"), vm::seeOlder, enabled = !vm.loadingOlder)
            } else if (items.size > 20) {
                Text(
                    t("emails.thatIsEverything"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (vm.olderFailed) {
                Text(
                    t("emails.couldntLoadOlderEmailsTry"),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        Box(Modifier.height(96.dp))
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
    seen: String? = null,
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
    // web .dl-email.dl-mail: a 10 dp mail card; mine in the grey-blue.
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, shape)
            .clip(shape)
            .background(if (mine) c.mailMine else c.mailBackground)
            .border(1.dp, c.mailLine, shape)
            // Fold & open (Motion doc): the card's height eases instead of jumping.
            .animateContentSize(tween(260)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { contentDescription = if (open) t("emails.fold") else t("emails.unfold") },
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
                    ringWidth = 1.5.dp,
                    initials = person.nickname.take(1).uppercase(),
                    ring = signatureOf(person.id),
                )
            }
            Text(
                person?.nickname.orEmpty(),
                if (open) Modifier.weight(1f) else Modifier,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold),
                color = c.mailInk,
            )
            if (open) {
                Text(
                    t("emails.to", "name" to if (mine) friend?.nickname.orEmpty() else me?.nickname.orEmpty()),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val t = e.text.replace(Regex("\\s+"), " ").trim()
                val snippet = t.ifEmpty { mediaWords(photos.size, audios.size, if (video != null) 1 else 0).orEmpty() }
                Text(
                    snippet,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val icons = listOfNotNull(
                    if (photos.isNotEmpty()) "📷${if (photos.size > 1) " ${formatNumber(photos.size)}" else ""}" else null,
                    if (audios.isNotEmpty()) "🎙${if (audios.size > 1) " ${formatNumber(audios.size)}" else ""}" else null,
                    if (video != null) "🎬" else null,
                )
                if (icons.isNotEmpty()) Text(icons.joinToString(" "), fontSize = 12.sp)
                ReactionSummary(e.reactions)
            }
            Text(
                Time.clock(e.sentAt, nowMs),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (open) {
            // web .dl-email-body: indented to the name (44 dp), 15/1.55 text.
            Column(Modifier.padding(start = 44.dp, end = 12.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (e.text.isNotEmpty()) {
                    Text(
                        e.text,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 23.25.sp),
                        color = c.mailInk,
                    )
                }
                video?.let { v -> VideoPlayer(resolveMedia(base, v.url), v.thumbUrl?.let { resolveMedia(base, it) }, v.durationMs, v.width, v.height) }
                if (photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.forEachIndexed { i, p ->
                            AsyncImage(
                                model = resolveMedia(base, p.thumbUrl ?: p.url),
                                contentDescription = t("emails.photoOfOpenFullSize", "n" to i + 1, "count" to photos.size),
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)).clickable { onPhoto(photos, i) },
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
                            t("letters.voiceNoteN", "n" to i + 1)
                        } else {
                            t("media.voiceNoteLower")
                        },
                    )
                }
                Reactions("emails", e.id, e.reactions, me, friend, chipColour = c.mailBackground, surface = ReactionSurface.Mail, onChanged = onReacted)
            }
        }
        seen?.let {
            Text(
                t("emails.seen", "time" to Time.clock(it, nowMs)),
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 8.dp).offset(y = (-4).dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
    }
}

/** S15 New email (web emails/NewEmailScreen.tsx; canvas E-New). */
@Composable
fun NewEmailScreen(vm: NewEmailViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val home by vm.home.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().imePadding()) {
        DlTopAppBar(title = t("emails.newEmail"), onBack = { nav.back() }, backDescription = t("emails.backToEmails"))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp)) {
            ComposerView(vm.composer, home.home?.emailCredits) { sent ->
                nav.replace(ThreadRoute(sent.thread.id))
            }
        }
    }
}
