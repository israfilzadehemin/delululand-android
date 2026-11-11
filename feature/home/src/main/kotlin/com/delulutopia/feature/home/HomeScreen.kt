package com.delulutopia.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.motion.float
import com.delulutopia.core.designsystem.motion.rememberPlayed
import com.delulutopia.core.designsystem.motion.rowEnter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.Mood
import com.delulutopia.core.network.model.Row as LetterRow
import com.delulutopia.core.ui.Arc
import com.delulutopia.core.ui.FriendRoute
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LetterTile
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NewChip
import com.delulutopia.core.ui.OfflineBanner
import com.delulutopia.core.ui.ReachButton
import com.delulutopia.core.ui.StampImage
import com.delulutopia.core.ui.TakeBackDialog
import com.delulutopia.core.ui.TileShape
import com.delulutopia.core.ui.UniverseRoute
import com.delulutopia.core.ui.Who
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.ageWords
import com.delulutopia.core.ui.firstLineOf
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.leaves.leafSurface
import com.delulutopia.core.ui.moodText
import com.delulutopia.core.ui.notify.NotificationHint
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tileShapeOf
import com.delulutopia.core.ui.tintOf
import com.delulutopia.core.ui.whoOf
import kotlinx.coroutines.launch

/**
 * S2 Home (web home/HomeScreen.tsx; canvas A-Home): the shared board on top, both moods, the two
 * clocks, From <friend> (new and incoming) and To <friend> (in reach, in transit, delivered-unread),
 * each newest first. Rows flip locally at 0:00 and the screen refetches.
 */
@Composable
fun HomeScreen(vm: HomeViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    HomeScreen(state, vm)
}

/** Home for a given state; [actions] does the writes. */
@Composable
fun HomeScreen(state: HomeRepository.State, actions: HomeActions) {
    val vm = actions
    val home = state.home
    Column(Modifier.fillMaxSize()) {
        if (home == null) {
            if (state.offline) OfflineBanner(Modifier.padding(horizontal = 16.dp)) else HomeLoading()
        } else {
            HomeContent(home, state.offline, state.moods, state.moodLimit, state.moodsLoaded, vm)
        }
    }
}

@Composable
private fun HomeContent(home: Home, stale: Boolean, moods: List<Mood>, moodLimit: Int, moodsLoaded: Boolean, vm: HomeActions) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val nowMs = rememberNow()
    var moodOpen by remember { mutableStateOf(false) }
    var cancelId by remember { mutableStateOf<Long?>(null) }
    var cancelling by remember { mutableStateOf(false) }

    val rows = home.from + home.to
    val flipped = rows.any { stateAt(it, nowMs) != it.state }
    LaunchedEffect(flipped) { if (flipped) vm.flipped() }

    val friend = home.friend
    val empty = home.from.isEmpty() && home.to.isEmpty()
    val played = rememberPlayed()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (stale) item(key = "offline") { OfflineBanner() }
        item(key = "board") { Box(Modifier.leafSurface("board")) { BoardTile(home, nowMs, vm) } }
        item(key = "moods") {
            // The moods rise one after another (web .dl-moods: 0, 50, 100 ms); the hint fades in last.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.enter(Enter.Rise, 0, 340).leafSurface("me")) { MyMood(home.me, nowMs) { moodOpen = true } }
                if (friend != null) Box(Modifier.enter(Enter.Rise, 50, 340).leafSurface("friend")) { FriendCard(friend, nowMs) { nav.open(FriendRoute) } }
                Box(Modifier.enter(Enter.Fade, 200, 300)) { NotificationHint() }
            }
        }
        if (friend == null) {
            item(key = "waiting") {
                Box(Modifier.leafSurface("waiting")) { WaitingCard(home.universe.name, home.universe.isCreator) { nav.open(UniverseRoute) } }
            }
        }
        item(key = "clocks") { Box(Modifier.leafSurface("clocks")) { HomeClocks(home.me, friend, nowMs) } }
        if (empty) {
            item(key = "empty") { EmptyPost() }
        } else if (friend != null) {
            if (home.from.isNotEmpty()) {
                item(key = "from-h") { GroupHeading("From ${friend.nickname}", whoOf(friend.id)) }
                itemsIndexed(home.from, key = { _, it -> "from-${it.id}" }) { i, row ->
                    Box(Modifier.animateItem().rowEnter(i, played, "from-${row.id}", stateAt(row, nowMs)).leafSurface("/letters/${row.id}")) {
                        FromRow(row, nowMs, whoOf(friend.id))
                    }
                }
            }
            if (home.to.isNotEmpty()) {
                item(key = "to-h") { GroupHeading("To ${friend.nickname}", whoOf(home.me.id)) }
                itemsIndexed(home.to, key = { _, it -> "to-${it.id}" }) { i, row ->
                    Box(Modifier.animateItem().rowEnter(i, played, "to-${row.id}", stateAt(row, nowMs)).leafSurface("/letters/${row.id}")) {
                        ToRow(row, nowMs, whoOf(home.me.id)) {
                            cancelId =
                                row.id
                        }
                    }
                }
            }
        }
    }

    if (moodOpen) {
        MoodSheet(
            current = home.me.mood,
            friend = friend?.nickname,
            friendMood = friend?.mood,
            moods = moods,
            limit = moodLimit,
            canAdd = moodsLoaded,
            vm = vm,
            onClose = { moodOpen = false },
        )
    }
    cancelId?.let { id ->
        TakeBackDialog(
            friend = friend?.nickname ?: "Friend",
            draftEmpty = home.me.draftEmpty,
            busy = cancelling,
            onYes = {
                cancelling = true
                scope.launch {
                    val r = vm.cancel(id)
                    cancelling = false
                    cancelId = null
                    when (r) {
                        LetterActions.CancelResult.Done -> nav.open(WriteRoute())
                        LetterActions.CancelResult.TooLate -> notice(LetterActions.TOO_LATE)
                        LetterActions.CancelResult.Failed -> notice(LetterActions.NOT_CANCELLED)
                    }
                }
            },
            onNo = { cancelId = null },
        )
    }
}

internal fun stateAt(row: LetterRow, nowMs: Long): String = LetterState.of(nowMs, row.windowEndsAt, row.arrivesAt, row.readAt)

private fun progressAt(row: LetterRow, nowMs: Long): Float {
    val start = Time.ms(row.sentAt) ?: return 1f
    val end = Time.ms(row.arrivesAt) ?: return 1f
    if (end <= start) return 1f
    return ((nowMs - start).toFloat() / (end - start)).coerceIn(0f, 1f)
}

@Composable
private fun MyMood(me: Citizen, nowMs: Long, onOpen: () -> Unit) {
    val mood = me.moodText()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DlRemoteAvatar(me.avatarUrl, me.nickname, tintOf(me.id), size = 40.dp, initials = me.initials(), ring = signatureOf(me.id))
        val shape = RoundedCornerShape(20.dp)
        Row(
            Modifier
                .height(40.dp)
                .clip(shape)
                .then(
                    if (mood != null) {
                        Modifier.background(DlTheme.colors.letterEnvelope).border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    } else {
                        Modifier.dashed(MaterialTheme.colorScheme.primary, 20.dp)
                    },
                )
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (mood != null) {
                Text(
                    buildAnnotatedString {
                        append(mood)
                        withStyle(SpanStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(" · set ${ageWords(me.moodSetAt, nowMs)}")
                        }
                    },
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
            } else {
                Text(
                    "Set your mood",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun FriendCard(friend: Citizen, nowMs: Long, onOpen: () -> Unit) {
    val mood = friend.moodText()
    Row(
        Modifier.fillMaxWidth().background(tintOf(friend.id), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.clip(CircleShape).clickable(onClickLabel = "${friend.nickname}'s profile", onClick = onOpen)) {
            DlRemoteAvatar(friend.avatarUrl, friend.nickname, tintOf(friend.id), size = 48.dp, initials = friend.initials(), ring = signatureOf(friend.id))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(friend.nickname, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            if (mood != null) {
                Text(
                    buildAnnotatedString {
                        append(mood)
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" · set ${ageWords(friend.moodSetAt, nowMs)}") }
                    },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                )
            } else {
                Text("No mood yet", style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** Universe U8: the friend card's place while the creator is alone. */
@Composable
private fun WaitingCard(name: String, isCreator: Boolean, onUniverse: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().dashed(DlTheme.colors.inkDisabled, 12.dp, width = 2.dp).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Waiting for someone to join", style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        Text(
            "Send $name's key to the one person you want here.${if (isCreator) " Lost it? Make a new one." else ""}",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 21.sp),
        )
        if (isCreator) DlFilledButton("You › Universe", onUniverse, height = 40.dp)
    }
}

/** Home clocks (doc "Delululand — Clocks"): one analog face per citizen in that citizen's zone, mine first. */
@Composable
private fun HomeClocks(me: Citizen, friend: Citizen?, nowMs: Long) {
    if (me.timezone == null || (friend != null && friend.timezone == null)) return
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ZoneClock(me, nowMs, Modifier.weight(1f))
        if (friend != null) ZoneClock(friend, nowMs, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun ZoneClock(person: Citizen, nowMs: Long, modifier: Modifier) {
    val zone = person.timezone ?: "UTC"
    val (hour, minute) = Time.zonedTime(nowMs, zone)
    val night = Time.isNight(hour)
    val hhmm = "%02d:%02d".format(hour, minute)
    val city = Time.cityOf(zone)
    val c = DlTheme.colors
    val seal = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val quiet = MaterialTheme.colorScheme.onSurfaceVariant
    val ring = signatureOf(person.id)
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Canvas(Modifier.size(56.dp).semantics { contentDescription = "${person.nickname}, $city: $hhmm${if (night) ", night" else ""}" }) {
            scale(size.width / 100f, size.height / 100f, pivot = Offset.Zero) {
                val centre = Offset(50f, 50f)
                drawCircle(if (night) seal else paper, 46f, centre)
                drawCircle(ring, 46f, centre, style = Stroke(3f))
                for (i in 0 until 12) {
                    rotate(i * 30f, centre) {
                        val long = i % 3 == 0
                        drawLine(
                            if (night) c.inkOnScrim else quiet,
                            Offset(50f, if (long) 10f else 11.5f),
                            Offset(50f, if (long) 17f else 15f),
                            2f,
                            StrokeCap.Round,
                        )
                    }
                }
                val hands = if (night) paper else ink
                rotate((hour % 12) * 30f + minute * 0.5f, centre) { drawLine(hands, Offset(50f, 54f), Offset(50f, 28f), 4.5f, StrokeCap.Round) }
                rotate(minute * 6f, centre) { drawLine(hands, Offset(50f, 56f), Offset(50f, 18f), 3f, StrokeCap.Round) }
                drawCircle(if (night) paper else seal, 3f, centre)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("${person.nickname} · $city", style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
            Text(
                buildAnnotatedString {
                    append(hhmm)
                    if (night) withStyle(SpanStyle(fontWeight = FontWeight.Normal, color = quiet)) { append(" · night") }
                },
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                color = seal,
            )
        }
    }
}

@Composable
private fun GroupHeading(text: String, who: Who) {
    Text(
        text.uppercase(),
        Modifier.enter(Enter.Fade, 60, 300).padding(top = 4.dp, start = 2.dp).semantics { heading() },
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        color = if (who == Who.A) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** From the friend: NEW once arrived (opens the letter), else an incoming row that opens nothing (FS-R3). */
@Composable
private fun FromRow(row: LetterRow, nowMs: Long, who: Who) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val state = stateAt(row, nowMs)
    if (state == LetterState.DELIVERED || state == LetterState.READ) {
        LetterTile(tileShapeOf(row, state), who, onClick = { nav.open(LetterRoute(row.id)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    RowLine(firstLineOf(row), strong = true)
                    RowMeta("Arrived ${Time.clock(row.arrivesAt, nowMs)}")
                }
                row.stamp?.let { StampImage(it) }
                if (state == LetterState.DELIVERED) NewChip()
            }
        }
        return
    }
    val arrives = Time.clock(row.arrivesAt, nowMs)
    LetterTile(
        TileShape.Envelope,
        who,
        Modifier.semantics {
            contentDescription = "On its way, arrives $arrives"
        },
        onClick = { notice("Arrives at $arrives") },
    ) {
        InTransit(row, nowMs, who, arrives)
    }
}

@Composable
private fun InTransit(row: LetterRow, nowMs: Long, who: Who, arrives: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RowMeta("On its way · arrives $arrives")
            Arc(progressAt(row, nowMs), who)
        }
        Text(Time.countdown((Time.ms(row.arrivesAt) ?: 0) - nowMs), style = DlTheme.type.countdown, color = DlTheme.colors.letterInk)
    }
}

/** To the friend: in reach (Edit, Cancel), in transit (arc and countdown), delivered and not read yet. */
@Composable
private fun ToRow(row: LetterRow, nowMs: Long, who: Who, onCancel: () -> Unit) {
    val nav = LocalAppNav.current
    val state = stateAt(row, nowMs)
    val arrives = Time.clock(row.arrivesAt, nowMs)
    when (state) {
        LetterState.IN_REACH -> LetterTile(TileShape.Envelope, who, reach = true) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Time.countdown((Time.ms(row.windowEndsAt) ?: 0) - nowMs)} left to edit or cancel",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                        color = DlTheme.colors.letterInk,
                    )
                    RowMeta("Arrives $arrives")
                }
                Row(
                    Modifier.fillMaxWidth().clickable { nav.open(LetterRoute(row.id)) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(Modifier.weight(1f)) { RowLine(firstLineOf(row), strong = false) }
                    row.stamp?.let { StampImage(it) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReachButton("Edit", Modifier.weight(1f)) { nav.open(WriteRoute(editId = row.id)) }
                    ReachButton("Cancel", Modifier.weight(1f), onClick = onCancel)
                }
            }
        }
        LetterState.IN_TRANSIT -> LetterTile(
            TileShape.Envelope,
            who,
            Modifier.semantics {
                contentDescription = "On its way, arrives $arrives"
            },
            onClick = { nav.open(LetterRoute(row.id)) },
        ) {
            InTransit(row, nowMs, who, arrives)
        }
        else -> LetterTile(TileShape.Sheet, who, onClick = { nav.open(LetterRoute(row.id)) }) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    RowLine(firstLineOf(row), strong = false)
                    RowMeta(
                        if (state ==
                            LetterState.READ
                        ) {
                            "Read ${Time.clock(row.readAt, nowMs)}"
                        } else {
                            "Delivered ${Time.clock(row.arrivesAt, nowMs)} · not read yet"
                        },
                    )
                }
                row.stamp?.let { StampImage(it) }
                Icon(WebIcons.Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RowLine(text: String, strong: Boolean) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = if (strong) FontWeight.Bold else FontWeight.Medium),
        color = DlTheme.colors.letterInk,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RowMeta(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 16.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** Nothing in the post yet: the empty envelope with its plus seal. */
@Composable
private fun EmptyPost() {
    val line = MaterialTheme.colorScheme.outlineVariant
    val letter = DlTheme.colors.letterSheet
    val paper = MaterialTheme.colorScheme.surface
    val hue = DlTheme.colors.postmarkHue
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Canvas(Modifier.enter(Enter.Rise, durationMs = 400).float(400).size(96.dp, 72.dp)) {
            scale(size.width / 96f, size.height / 72f, pivot = Offset.Zero) {
                drawRoundRect(letter, Offset(8f, 14f), Size(80f, 50f), CornerRadius(6f))
                drawRoundRect(line, Offset(8f, 14f), Size(80f, 50f), CornerRadius(6f), style = Stroke(1.5f))
                drawLine(line, Offset(8f, 20f), Offset(48f, 46f), 1.5f)
                drawLine(line, Offset(48f, 46f), Offset(88f, 20f), 1.5f)
                drawCircle(paper, 9f, Offset(78f, 18f))
                drawCircle(hue, 9f, Offset(78f, 18f), style = Stroke(1.5f))
                drawLine(hue, Offset(78f, 13f), Offset(78f, 23f), 1.5f)
                drawLine(hue, Offset(73f, 18f), Offset(83f, 18f), 1.5f)
            }
        }
        Text(
            "Nothing in the post yet",
            Modifier.enter(Enter.Rise, 80, 400),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** S2-Home-loading: placeholders in the shapes that will arrive. */
@Composable
private fun HomeLoading() {
    val ph = DlTheme.colors.letterEnvelope
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(220.dp).background(ph, RoundedCornerShape(6.dp)))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(40.dp).background(ph, CircleShape))
            Box(Modifier.size(140.dp, 40.dp).background(ph, RoundedCornerShape(20.dp)))
        }
        Box(Modifier.fillMaxWidth().height(68.dp).background(ph, RoundedCornerShape(12.dp)))
        repeat(3) { Box(Modifier.fillMaxWidth().height(64.dp).background(ph, RoundedCornerShape(4.dp))) }
    }
}

/** A dashed rounded outline (the unset mood chip, the waiting card). */
private fun Modifier.dashed(colour: Color, radius: Dp, width: Dp = 1.dp): Modifier = drawBehind {
    val w = width.toPx()
    drawRoundRect(
        colour,
        topLeft = Offset(w / 2, w / 2),
        size = Size(size.width - w, size.height - w),
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
    )
}
