package com.delulutopia.feature.letters

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.Moods
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.Motion
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.LetterFull
import com.delulutopia.core.network.model.ReplyRef
import com.delulutopia.core.network.model.Row as LetterRow
import com.delulutopia.core.ui.LetterBody
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.Lightbox
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.StampImage
import com.delulutopia.core.ui.TakeBackDialog
import com.delulutopia.core.ui.WaxSeal
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.firstLineOf
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.media.AudioPlayer
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signature
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tintOf
import com.delulutopia.core.ui.whoOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The envelope moment before a NEW letter's sheet. */
private const val OPEN_MS = 1100

private fun openable(r: LetterRow, nowMs: Long): Boolean {
    if (r.direction == "to") return true
    val s = LetterState.of(nowMs, r.windowEndsAt, r.arrivesAt, r.readAt)
    return s == LetterState.DELIVERED || s == LetterState.READ
}

/**
 * S7 (FS-R4–R10; web letters/LetterView.tsx, LetterSwipe.tsx; canvas C-Letter-*). Received: the
 * header with the frozen mood, the body, photos, voice notes, the doodle, Reply. Sent: the in-reach
 * bar while the window is open and the status line Sent → Delivered → Read. Swipe sideways for the
 * next older or newer letter; letters still on their way to me are skipped, and an unread received
 * letter is fetched (its read mark) only once it has settled on screen.
 */
@Composable
fun LetterScreen(id: Long, vm: LettersViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val rowsState by vm.rows.collectAsStateWithLifecycle()
    val homeState by vm.home.collectAsStateWithLifecycle()
    val letters by vm.letters.collectAsStateWithLifecycle()
    val home = homeState.home
    // The order is S6's (newest first); the opened letter is always part of it.
    val ids = remember(rowsState, id) {
        val now = Time.now()
        val list = rowsState.rows.filter { it.id == id || openable(it, now) }.map { it.id }
        if (id in list) list else listOf(id)
    }
    val start = ids.indexOf(id).coerceAtLeast(0)
    val pager = rememberPagerState(initialPage = start) { ids.size }
    var current by rememberSaveable { mutableStateOf(id) }
    LaunchedEffect(pager.settledPage, ids) {
        ids.getOrNull(pager.settledPage)?.let {
            current = it
            vm.open(it)
        }
        if (pager.settledPage >= ids.size - 1 && rowsState.hasMore) vm.loadMore()
    }
    val shownLetter = (letters[current] as? LetterUi.Shown)?.letter
    val friend = home?.friend?.nickname ?: "Friend"
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(
            title = when {
                shownLetter != null -> "${if (shownLetter.direction == "to") "To" else "From"} $friend"
                letters[current] is LetterUi.Loading || letters[current] == null -> ""
                else -> "From $friend"
            },
            onBack = { nav.back() },
            backDescription = "Back",
        )
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth(), key = { ids[it] }) { page ->
            val letterId = ids[page]
            when (val ui = letters[letterId]) {
                is LetterUi.Shown -> LetterContent(ui.letter, home, swiped = letterId != id, vm = vm)
                is LetterUi.NotArrived -> Gone("Arrives at ${Time.clock(ui.arrivesAt)}") { nav.back() }
                LetterUi.Gone -> Gone("This letter isn't here anymore.") { nav.back() }
                LetterUi.Unreachable -> Gone("Can't reach ${LocalBrand.current}. Try again.") { nav.back() }
                else -> Box(Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun Gone(text: String, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DlOutlinedButton("Back", onBack)
    }
}

@Composable
private fun LetterContent(letter: LetterFull, home: Home?, swiped: Boolean, vm: LettersViewModel) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val base = LocalServerBaseUrl.current
    val nowMs = rememberNow()
    val sent = letter.direction == "to"
    val friend = home?.friend
    val me = home?.me
    val sender: Citizen? = if (sent) me else friend
    val state = LetterState.of(nowMs, letter.windowEndsAt, letter.arrivesAt, letter.readAt)
    val inReach = sent && state == LetterState.IN_REACH
    val arrived = state == LetterState.DELIVERED || state == LetterState.READ
    val feeling = if (!letter.moodEmoji.isNullOrEmpty() &&
        !letter.moodLabel.isNullOrEmpty()
    ) {
        "${letter.moodEmoji} ${letter.moodLabel}"
    } else {
        Moods.text(letter.moodAtSend)
    }
    val photos = letter.attachments.filter { it.kind == "photo" }
    val audios = letter.attachments.filter { it.kind == "audio" }
    val doodle = letter.attachments.firstOrNull { it.kind == "doodle" }
    var photo by rememberSaveable(letter.id) { mutableStateOf(-1) }
    var doodleOpen by rememberSaveable(letter.id) { mutableStateOf(false) }
    var cancelling by remember { mutableStateOf(false) }
    var askCancel by remember { mutableStateOf(false) }

    // A received letter opened for the first time writes itself onto the paper (a tap shows it all),
    // after its envelope opens, unless a swipe brought it here.
    val fresh = !sent && (letter.readAt == null || Time.now() - (Time.ms(letter.readAt) ?: 0) < 30_000)
    var reveal by rememberSaveable(letter.id) { mutableStateOf(fresh) }
    var opening by rememberSaveable(letter.id) { mutableStateOf(fresh && !swiped) }
    // Thumbnails and players pop in after the sheet (web: 260 ms, or 1000 ms out of an envelope).
    val mediaDelay = if (fresh && !swiped) 1000 else 260
    LaunchedEffect(opening) {
        if (opening) {
            delay(OPEN_MS.toLong())
            opening = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // The sheet fills the page (web .dl-paper flex: 1): its rules run to the bottom; Reply stays under it.
        BoxWithConstraints(Modifier.weight(1f)) {
            val pageHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                if (inReach) {
                    Row(
                        Modifier
                            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${Time.countdown((Time.ms(letter.windowEndsAt) ?: 0) - nowMs)} left to edit or cancel",
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        )
                        DlOutlinedButton("Edit", { nav.open(WriteRoute(editId = letter.id)) })
                        DlOutlinedButton("Cancel", { askCancel = true })
                    }
                }
                Box(Modifier.padding(horizontal = 16.dp).padding(bottom = if (sent) 24.dp else 12.dp)) {
                    // The sheet settles in (dl-paper-in); out of a NEW letter's envelope it waits for the envelope.
                    Paper(
                        Modifier
                            .heightIn(min = pageHeight - if (sent) 24.dp else 12.dp)
                            .enter(Enter.Paper, if (fresh && !swiped) 620 else 40, if (fresh && !swiped) 520 else 480, key = letter.id)
                            .clickable(enabled = reveal) { reveal = false },
                    ) {
                        // Head, quote and body on top; the foot at the bottom. The body's rules run on behind the gap.
                        Column {
                            // Head: who, feeling, times; a double rule under it (web .dl-paper--sheet .dl-paper-head).
                            val rule = DlTheme.colors.letterRule
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .drawBehind {
                                        drawLine(rule, Offset(0f, size.height - 3.dp.toPx()), Offset(size.width, size.height - 3.dp.toPx()), 1.dp.toPx())
                                        drawLine(rule, Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), 1.dp.toPx())
                                    }
                                    .padding(start = 16.dp, top = 16.dp, end = if (letter.stamp != null) 84.dp else 16.dp, bottom = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                if (sender != null) {
                                    DlRemoteAvatar(
                                        sender.avatarUrl,
                                        sender.nickname,
                                        tintOf(sender.id),
                                        size = 40.dp,
                                        initials = sender.initials(),
                                        ring = signatureOf(sender.id),
                                    )
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        buildAnnotatedString {
                                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(sender?.nickname.orEmpty()) }
                                            if (feeling != null) {
                                                withStyle(SpanStyle(fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                                    append(" · Feeling $feeling")
                                                }
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                        color = DlTheme.colors.letterInk,
                                    )
                                    Text(
                                        "Sent ${Time.clock(
                                            letter.sentAt,
                                            nowMs,
                                        )} · ${if (arrived) "Arrived" else "Arrives"} ${Time.clock(letter.arrivesAt, nowMs)}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, fontFeatureSettings = "tnum"),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                            letter.replyTo?.let {
                                Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                                    ReplyQuote(it, friend?.nickname ?: "Friend", nowMs) { nav.open(LetterRoute(it.id)) }
                                }
                            }
                            // Body: 17/28 Literata on 28 dp rules, down to the foot.
                            Box(
                                Modifier.fillMaxWidth().padding(
                                    start = 20.dp,
                                    end = 20.dp,
                                    top = if (letter.replyTo !=
                                        null
                                    ) {
                                        12.dp
                                    } else {
                                        18.dp
                                    },
                                    bottom = 18.dp,
                                ),
                            ) {
                                Ruled(runOn = true) { LetterBody(letter.body, reveal = reveal && !opening) }
                            }
                        }
                        val sheet = DlTheme.colors.letterSheet
                        val margin = DlTheme.colors.letterMargin
                        Column(
                            Modifier.fillMaxWidth().drawBehind {
                                drawRect(sheet)
                                drawLine(margin, Offset(11.dp.toPx(), 0f), Offset(11.dp.toPx(), size.height), 1.dp.toPx())
                            },
                        ) {
                            val hasMedia = doodle != null || photos.isNotEmpty() || audios.isNotEmpty()
                            if (hasMedia || sent) {
                                // Foot: a perforated edge, then the doodle, prints, players and the status line.
                                val line = MaterialTheme.colorScheme.outlineVariant
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            drawLine(
                                                line,
                                                Offset.Zero,
                                                Offset(size.width, 0f),
                                                1.5.dp.toPx(),
                                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.dp.toPx())),
                                            )
                                        }
                                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    doodle?.let { d ->
                                        AsyncImage(
                                            model = resolveMedia(base, d.url),
                                            contentDescription = "Doodle, open full size",
                                            modifier = Modifier.fillMaxWidth().aspectRatio(1f).rotate(0.3f).border(1.dp, line).clickable { doodleOpen = true },
                                            contentScale = ContentScale.Fit,
                                        )
                                    }
                                    if (photos.isNotEmpty()) {
                                        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            photos.forEachIndexed { i, p ->
                                                Print(
                                                    resolveMedia(base, p.thumbUrl ?: p.url),
                                                    "Photo ${i + 1} of ${photos.size}, open full size",
                                                    if (i % 2 == 0) -1.5f else 1.2f,
                                                    Modifier.enter(Enter.PopIn, mediaDelay, 300),
                                                ) { photo = i }
                                            }
                                        }
                                    }
                                    audios.forEachIndexed { i, a ->
                                        AudioPlayer(
                                            resolveMedia(base, a.url),
                                            a.durationMs,
                                            Modifier.enter(Enter.PopIn, mediaDelay, 300),
                                            label = if (audios.size > 1) "voice note ${i + 1}" else "voice note",
                                        )
                                    }
                                    if (sent) StatusLine(letter, state, nowMs)
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                    // The stamp takes the corner, as on an envelope (FS-ST6; web 72 px).
                    letter.stamp?.let {
                        StampImage(
                            it,
                            Modifier.align(Alignment.TopEnd).padding(top = 10.dp, end = 10.dp)
                                .enter(Enter.Stamp, if (fresh && !swiped) 1040 else 380, key = letter.id).rotate(2f),
                            width = 64.dp,
                        )
                    }
                    if (opening) EnvelopeOpening(whoOf(sender?.id).signature()) { opening = false }
                }
            }
        }
        if (!sent) {
            // Reply under the sheet, full width (web .dl-letter-actions).
            DlFilledButton(
                "Reply",
                { nav.open(WriteRoute(replyTo = letter.id)) },
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 24.dp).enter(Enter.Rise, 300, 340),
                icon = WebIcons.Reply,
                height = 48.dp,
            )
        }
    }

    if (photo >= 0 && photo < photos.size) Lightbox(photos, photo, { photo = it }, { photo = -1 })
    if (doodleOpen && doodle != null) Lightbox(listOf(doodle), 0, {}, { doodleOpen = false }, title = "Doodle")
    if (askCancel) {
        TakeBackDialog(
            friend = friend?.nickname ?: "Friend",
            draftEmpty = me?.draftEmpty != false,
            busy = cancelling,
            onYes = {
                cancelling = true
                scope.launch {
                    val r = vm.cancel(letter.id)
                    cancelling = false
                    askCancel = false
                    when (r) {
                        // The letter page has nothing left to show: Write takes its place.
                        LetterActions.CancelResult.Done -> nav.replace(WriteRoute())
                        LetterActions.CancelResult.TooLate -> notice(LetterActions.TOO_LATE)
                        LetterActions.CancelResult.Failed -> notice(LetterActions.NOT_CANCELLED)
                    }
                }
            },
            onNo = { askCancel = false },
        )
    }
}

/** The letter sheet: margin line, a folded corner, a soft shadow. */
@Composable
private fun Paper(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = DlTheme.colors
    val shape = RoundedCornerShape(2.dp)
    Box {
        Column(
            modifier
                .fillMaxWidth()
                .shadow(2.dp, shape)
                .clip(shape)
                .background(c.letterSheet, shape)
                .drawBehind { drawLine(c.letterMargin, Offset(11.dp.toPx(), 0f), Offset(11.dp.toPx(), size.height), 1.dp.toPx()) },
            verticalArrangement = Arrangement.SpaceBetween,
            content = content,
        )
        val ground = MaterialTheme.colorScheme.surface
        Box(
            Modifier.align(Alignment.BottomEnd).size(26.dp).drawBehind {
                val s = size.width
                drawPath(
                    Path().apply {
                        moveTo(s, 0f)
                        lineTo(s, s)
                        lineTo(0f, s)
                        close()
                    },
                    ground,
                )
                drawPath(
                    Path().apply {
                        moveTo(0f, 0f)
                        lineTo(s, 0f)
                        lineTo(0f, s)
                        close()
                    },
                    c.letterFlap,
                )
            },
        )
    }
}

/** Ruled paper under the body: one rule per 28 sp line. */
@Composable
private fun Ruled(runOn: Boolean = false, content: @Composable () -> Unit) {
    val rule = DlTheme.colors.letterRule
    Box(
        Modifier.fillMaxWidth().heightIn(min = 196.dp).drawBehind {
            val step = 28.sp.toPx()
            var y = step - 1.dp.toPx()
            // On a letter the rules carry on down the sheet; the sheet clips them and the foot covers them.
            val end = if (runOn) size.height + 4000f else size.height
            while (y < end) {
                drawLine(rule, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }
        },
    ) { content() }
}

private val QuoteShape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)

/** A photo print taped onto the sheet (web .dl-paper--sheet .dl-paper-thumb): 3 dp white border, tilt, tape. */
@Composable
private fun Print(url: String, description: String, tilt: Float, modifier: Modifier, onOpen: () -> Unit) {
    val c = DlTheme.colors
    Box(modifier.rotate(tilt)) {
        AsyncImage(
            model = url,
            contentDescription = description,
            modifier = Modifier
                .size(64.dp)
                .shadow(2.dp, RoundedCornerShape(2.dp))
                .background(c.letterPrint, RoundedCornerShape(2.dp))
                .padding(3.dp)
                .clip(RoundedCornerShape(1.dp))
                .clickable(onClick = onOpen),
            contentScale = ContentScale.Crop,
        )
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-5).dp).size(22.dp, 9.dp).rotate(-4f).background(c.letterTape))
    }
}

/** Replies B1: the letter this one answers, clipped above the text. Opens that letter. */
@Composable
private fun ReplyQuote(reply: ReplyRef, friendName: String, nowMs: Long, onOpen: () -> Unit) {
    val c = DlTheme.colors
    val whose = if (reply.direction == "to") "your letter" else "$friendName's letter"
    val line = firstLineOf(reply.firstLine, reply.media)
    Box(Modifier.padding(top = 12.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .rotate(-0.4f)
                .shadow(1.dp, QuoteShape)
                .background(c.letterSlip, QuoteShape)
                .drawBehind { drawRect(c.userA, size = Size(3.dp.toPx(), size.height)) }
                .clickable(onClick = onOpen)
                .semantics { contentDescription = "In reply to $whose of ${Time.clock(reply.sentAt, nowMs)}. Open it" }
                .padding(start = 15.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(Modifier.padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(WebIcons.Reply, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "In reply to $whose · ${Time.clock(reply.sentAt, nowMs)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (line.isNotEmpty()) {
                Text(
                    line,
                    style = DlTheme.type.letterBody.copy(fontSize = 14.sp, lineHeight = 20.sp),
                    color = c.letterInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // The paper clip holding the slip.
        val ink = MaterialTheme.colorScheme.onSurfaceVariant
        Box(
            Modifier.offset(x = 10.dp, y = (-12).dp).size(14.dp, 30.dp).drawBehind {
                val sx = size.width / 14f
                val sy = size.height / 30f
                val p = Path().apply {
                    moveTo(4 * sx, 28 * sy)
                    lineTo(4 * sx, 6 * sy)
                    cubicTo(4 * sx, 2 * sy, 10 * sx, 2 * sy, 10 * sx, 6 * sy)
                    lineTo(10 * sx, 24 * sy)
                    cubicTo(10 * sx, 26 * sy, 7 * sx, 26 * sy, 7 * sx, 24 * sy)
                    lineTo(7 * sx, 8 * sy)
                }
                drawPath(p, ink, style = Stroke(1.6.dp.toPx()))
            },
        )
    }
}

/** FS-R7: Sent → Delivered → Read, each with its time once reached; "Arrives in m:ss" before delivery. */
@Composable
private fun StatusLine(letter: LetterFull, state: String, nowMs: Long) {
    val delivered = state == LetterState.DELIVERED || state == LetterState.READ
    val read = state == LetterState.READ
    val label = when {
        read -> "Status: read"
        delivered -> "Status: delivered, not read yet"
        else -> "Status: sent, not yet delivered"
    }
    Column(Modifier.padding(top = 12.dp).semantics { contentDescription = label }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Step("SENT", Time.clock(letter.sentAt, nowMs), done = true, double = false)
            Box(Modifier.weight(1f).padding(top = 7.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Step("DELIVERED", if (delivered) Time.clock(letter.arrivesAt, nowMs) else "", done = delivered, double = false)
            Box(Modifier.weight(1f).padding(top = 7.dp).height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
            Step("READ", if (read) Time.clock(letter.readAt, nowMs) else "", done = read, double = true)
        }
        if (!delivered) {
            Text(
                "Arrives in ${Time.countdown((Time.ms(letter.arrivesAt) ?: 0) - nowMs)}",
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = DlTheme.colors.letterInk,
            )
        }
    }
}

@Composable
private fun Step(label: String, time: String, done: Boolean, double: Boolean) {
    val colour = if (done) DlTheme.colors.letterInk else MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (done) Icon(if (double) WebIcons.DoubleCheck else WebIcons.Check, null, Modifier.enter(Enter.Pop, durationMs = 380).size(12.dp), tint = colour)
            Text(
                label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.07.em),
                color = colour,
            )
        }
        if (time.isNotEmpty()) Text(time, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = colour)
    }
}

/**
 * The envelope a NEW letter arrives in: its flap lifts, the seal gives, and it drops away while the
 * sheet rises out of it. A tap skips to the sheet.
 */
@Composable
private fun EnvelopeOpening(seal: Color, onSkip: () -> Unit) {
    val c = DlTheme.colors
    val flap = remember { Animatable(0f) }
    val drop = remember { Animatable(0f) }
    // Web timeline (OPEN_MS 1100): flap lifts 360–640 ms, the seal gives 380–620 ms, the envelope drops 600–1080 ms.
    LaunchedEffect(Unit) {
        launch {
            delay(360)
            flap.animateTo(1f, tween(280, easing = Motion.EaseOut))
        }
        delay(600)
        drop.animateTo(1f, tween(480, easing = Motion.EaseIn))
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(260.dp)
            .graphicsLayer {
                translationY = drop.value * 70.dp.toPx()
                scaleX = 1f - 0.04f * drop.value
                scaleY = scaleX
                alpha = 1f - drop.value
            }
            .clickable(onClick = onSkip)
            .background(c.letterEnvelope, RoundedCornerShape(4.dp)),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .graphicsLayer {
                    rotationX = flap.value * 88f
                    alpha = 1f - flap.value
                    transformOrigin = TransformOrigin(0.5f, 0f)
                    cameraDistance = 24f * density
                }
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val p = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w * 0.57f, h * 0.86f)
                        quadraticTo(w * 0.5f, h * 0.99f, w * 0.43f, h * 0.86f)
                        close()
                    }
                    drawPath(p, c.letterFlap)
                    drawPath(p, c.letterEdge, style = Stroke(1.dp.toPx(), pathEffect = PathEffect.cornerPathEffect(2f)))
                },
        )
        WaxSeal(
            seal,
            Modifier.align(Alignment.TopCenter).offset(y = 118.dp).graphicsLayer {
                alpha = 1f - flap.value
                scaleX = 1f - 0.4f * flap.value
                scaleY = scaleX
            },
            size = 30.dp,
        )
    }
}
