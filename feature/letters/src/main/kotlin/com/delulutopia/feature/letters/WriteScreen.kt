package com.delulutopia.feature.letters

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.Moods
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.network.model.StampMark
import com.delulutopia.core.ui.EmojiPickerSheet
import com.delulutopia.core.ui.LetterBody
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.StampImage
import com.delulutopia.core.ui.WaxSeal
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.doodle.DoodlePad
import com.delulutopia.core.ui.editor.EditorToolbar
import com.delulutopia.core.ui.editor.RichEditor
import com.delulutopia.core.ui.editor.RichEditorState
import com.delulutopia.core.ui.editor.rememberRichEditor
import com.delulutopia.core.ui.media.AttachmentStrip
import com.delulutopia.core.ui.media.DoodleAdd
import com.delulutopia.core.ui.media.Item
import com.delulutopia.core.ui.media.Source
import com.delulutopia.core.ui.media.VoiceState
import com.delulutopia.core.ui.media.rememberVoiceRecording
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signatureOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * S4 Write (FS-W1–W16; web write/*; canvas W-Draft, W-Edit, W-Send, W-Late, W-Seal): the letter on
 * the same ruled sheet it is read on, the stamp slot, the reference panel when replying, the
 * attachment strip and the formatting bar. No navigation bar while writing; Back leaves and the
 * draft autosaves.
 */
@Composable
fun WriteScreen(vm: WriteViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    when (val load = vm.load) {
        WriteViewModel.Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        WriteViewModel.Load.Failed -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)) {
            Text(
                if (vm.editId == null) "Couldn't load your draft. Check your connection and try again." else "This letter can't be edited.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
            DlOutlinedButton("Back", { nav.back() })
        }
        is WriteViewModel.Load.NotEditable -> LaunchedEffect(load.id) { nav.replace(LetterRoute(load.id)) }
        is WriteViewModel.Load.Ready -> {
            val editor = rememberRichEditor(load.body) { vm.onBody(it) }
            if (vm.editId == null) DraftWrite(vm, editor) else EditWrite(vm, editor)
        }
    }
}

@Composable
private fun DraftWrite(vm: WriteViewModel, editor: RichEditorState) {
    val nav = LocalAppNav.current
    val scope = rememberCoroutineScope()
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val home = homeState.home
    val friend = home?.friend?.nickname ?: "Friend"
    val saver = vm.saver!!
    val saveState by saver.state.collectAsStateWithLifecycle()
    var sendOpen by remember { mutableStateOf(false) }
    var sealing by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushOnHide() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { vm.checkNewer()?.let(editor::set) } }

    val atts = vm.attachments!!
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val canSend = (!vm.empty || atts.done.isNotEmpty()) && !atts.uploading && !voice.recording
    val savedLine = when {
        saveState.status == DraftSaver.Status.Error -> "Couldn't save your draft"
        saveState.savedAt != null && saveState.status != DraftSaver.Status.Conflict -> "Draft saved ${Time.time(saveState.savedAt)}"
        else -> ""
    }

    WriteFrame(
        vm = vm,
        editor = editor,
        title = "To $friend",
        subtitle = savedLine,
        subtitleError = saveState.status == DraftSaver.Status.Error,
        backLabel = "Back, keep the draft",
        onBack = {
            vm.flushOnHide()
            nav.back()
        },
        action = "Send",
        actionEnabled = canSend,
        onAction = { sendOpen = true },
        voice = voice,
        onRemove = { item, index ->
            atts.remove(item.key)
            item.att?.let { a -> vm.deleteDraftAttachment(a.id) { atts.restore(item, index) } }
        },
        reference = vm.pinned?.let { { ReferencePanel(vm, friend) } },
        sealing = sealing,
        sealColour = signatureOf(home?.me?.id),
    )

    if (sendOpen) {
        SendSheet(
            delay = vm.config?.delay ?: "PT60M",
            window = vm.config?.window ?: "PT5M",
            feeling = home?.me?.let { Moods.text(it.mood, it.moodEmoji, it.moodLabel) },
            onClose = { sendOpen = false },
            onSend = {
                when (val r = vm.send()) {
                    WriteViewModel.SendResult.Sent -> {
                        sendOpen = false
                        // Letter paper story: the sheet folds into its envelope and is sealed, then Letters opens.
                        sealing = true
                        delay(SEAL_MS)
                        nav.top(LettersRoute)
                        null
                    }
                    WriteViewModel.SendResult.Conflict -> {
                        sendOpen = false
                        null
                    }
                    is WriteViewModel.SendResult.Failed -> r.message
                }
            },
        )
    }
    if (saveState.status == DraftSaver.Status.Conflict) {
        DlDialog(
            title = "A newer version was saved on another device. Load it?",
            text = null,
            confirmLabel = "Load",
            onConfirm = { scope.launch { vm.loadServer()?.let(editor::set) } },
            dismissLabel = "Keep mine",
            onDismiss = { vm.keepMine() },
        )
    }
}

@Composable
private fun EditWrite(vm: WriteViewModel, editor: RichEditorState) {
    val nav = LocalAppNav.current
    val scope = rememberCoroutineScope()
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val home = homeState.home
    val friend = home?.friend?.nickname ?: "Friend"
    val letter = vm.letter!!
    val nowMs = rememberNow()
    val left = (Time.ms(letter.windowEndsAt) ?: 0) - nowMs
    val late = left <= 0 || vm.lateByServer
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var sealing by remember { mutableStateOf(false) }
    val atts = vm.attachments!!
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val initialIds = remember { letter.attachments.joinToString(",") { it.id } }
    val initialBody = remember { letter.body }
    val changed = vm.body != initialBody || atts.done.joinToString(",") { it.id } != initialIds || atts.uploading || vm.stamp?.slug != letter.stamp?.slug
    val canSend = !late && !busy && (!vm.empty || atts.done.isNotEmpty()) && !atts.uploading && !voice.recording
    val back = {
        if (changed) {
            confirmDiscard = true
        } else {
            nav.back()
        }
    }
    BackHandler(enabled = !late) { back() }
    val arrives = Time.time(letter.arrivesAt)

    WriteFrame(
        vm = vm,
        editor = editor,
        title = "Editing a sent letter · ${Time.countdown(left)} left",
        subtitle = "Arrives at $arrives${if (late) "" else " whatever you change"}",
        subtitleError = false,
        backLabel = "Back, discard changes",
        onBack = back,
        backEnabled = !late,
        action = "Send again",
        actionEnabled = canSend,
        onAction = {
            busy = true
            error = null
            scope.launch {
                val failed = vm.sendAgain()
                if (failed == null) {
                    sealing = true
                    delay(SEAL_MS)
                    nav.back()
                } else {
                    busy = false
                    if (failed.isNotEmpty()) error = failed
                }
            }
        },
        voice = voice,
        onRemove = { item, _ -> atts.remove(item.key) },
        editing = true,
        late = late,
        error = error,
        sealing = sealing,
        sealColour = signatureOf(home?.me?.id),
    )

    if (late) {
        DlDialog(
            title = "Too late: the original version is on its way.",
            text = "$friend gets the letter as you first sent it, at $arrives. The text you changed is still here.",
            warning = if (home != null && !home.me.draftEmpty) "Keeping it replaces your current draft." else null,
            confirmLabel = "Keep text in draft",
            onConfirm = {
                busy = true
                scope.launch {
                    if (vm.keepInDraft()) {
                        nav.replace(WriteRoute())
                    } else {
                        busy = false
                        error = "Couldn't save your draft"
                    }
                }
            },
            dismissLabel = "Discard",
            onDismiss = {
                busy = true
                scope.launch {
                    vm.discardEdit()
                    nav.replace(LetterRoute(letter.id))
                }
            },
            busy = busy,
        )
    } else if (confirmDiscard) {
        DlDialog(
            title = "Discard your changes?",
            text = null,
            confirmLabel = "Yes",
            onConfirm = {
                confirmDiscard = false
                busy = true
                scope.launch {
                    vm.discardEdit()
                    nav.back()
                }
            },
            dismissLabel = "No",
            onDismiss = { confirmDiscard = false },
        )
    }
}

private const val SEAL_MS = 900L

/**
 * The frame both modes share: header with Back, title, saved line and the action; the reference
 * panel; the sheet with the stamp slot and the editor; the strip; the formatting bar.
 */
@Composable
private fun WriteFrame(
    vm: WriteViewModel,
    editor: RichEditorState,
    title: String,
    subtitle: String,
    subtitleError: Boolean,
    backLabel: String,
    onBack: () -> Unit,
    action: String,
    actionEnabled: Boolean,
    onAction: () -> Unit,
    voice: VoiceState,
    onRemove: (Item, Int) -> Unit,
    sealColour: Color,
    sealing: Boolean,
    backEnabled: Boolean = true,
    editing: Boolean = false,
    late: Boolean = false,
    error: String? = null,
    reference: (@Composable () -> Unit)? = null,
) {
    val atts = vm.attachments!!
    var stampOpen by remember { mutableStateOf(false) }
    var doodleOpen by remember { mutableStateOf(false) }
    var emojiOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = backEnabled) { Icon(DlIcons.Back, backLabel) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
                        color = if (subtitleError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            DlFilledButton(action, onAction, Modifier.padding(start = 4.dp, end = 12.dp), enabled = actionEnabled, height = 40.dp)
        }
        reference?.invoke()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Sheet(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 16.dp, top = 10.dp, bottom = 20.dp)) {
                    if (error != null) Text(error, Modifier.padding(bottom = 8.dp), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        StampSlot(vm.stamp, enabled = !late) {
                            vm.loadStamps()
                            stampOpen = true
                        }
                    }
                    RichEditor(editor, enabled = !late, minHeight = 280.dp)
                }
            }
            if (sealing) Seal(sealColour)
        }
        if (!late) {
            AttachmentStrip(
                attachments = atts,
                onRemove = onRemove,
                voice = voice,
                doodle = DoodleAdd(full = atts.has("doodle")) { doodleOpen = true },
            )
        }
        EditorToolbar(editor, Modifier.background(DlTheme.colors.letterEnvelope), enabled = !late) { emojiOpen = true }
    }
    if (doodleOpen) {
        DoodlePad(
            onDone = { png ->
                doodleOpen = false
                atts.add(Uploader.Kind.Doodle, Source.Made(png, "image/png"), "doodle.png")
            },
            onClose = { doodleOpen = false },
        )
    }
    if (stampOpen) StampPicker(vm.stamps, vm.stampsFailed, vm.stamp, vm::chooseStamp) { stampOpen = false }
    if (emojiOpen) {
        EmojiPickerSheet("Emoji", onPick = { editor.insert(it) }, onDismiss = { emojiOpen = false })
    }
}

/** The ruled letter sheet the letter is written on. */
@Composable
private fun Sheet(modifier: Modifier, content: @Composable () -> Unit) {
    val c = DlTheme.colors
    Box(
        modifier
            .background(c.letterSheet)
            .drawBehind {
                drawLine(c.letterMargin, Offset(11.dp.toPx(), 0f), Offset(11.dp.toPx(), size.height), 1.dp.toPx())
            },
    ) { content() }
}

/** The stamp slot on the paper: a dashed outline with a plus, or the chosen stamp (FS-ST8). */
@Composable
private fun StampSlot(stamp: StampMark?, enabled: Boolean, onOpen: () -> Unit) {
    val c = DlTheme.colors
    Box(
        Modifier
            .border(1.dp, c.letterEdge, RoundedCornerShape(2.dp))
            .background(if (stamp != null) c.letterFlap else Color.Transparent, RoundedCornerShape(2.dp))
            .clickable(enabled = enabled, onClick = onOpen)
            .semantics { contentDescription = if (stamp != null) "Stamp: ${stamp.name}. Change it" else "Add a stamp" }
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (stamp != null) {
            StampImage(stamp, width = 52.dp)
        } else {
            Box(
                Modifier.size(52.dp, 65.dp).drawBehind {
                    drawRect(c.letterEdge, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))))
                },
                contentAlignment = Alignment.Center,
            ) {
                Text("+", fontSize = 26.sp, color = c.letterEdge)
            }
        }
    }
}

/** Sent: the sheet folds into its envelope and is sealed (Letter paper story). */
@Composable
private fun Seal(colour: Color) {
    val c = DlTheme.colors
    val fold = remember { Animatable(0f) }
    val seal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        fold.animateTo(1f, tween(450))
        seal.animateTo(1f, tween(300))
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = fold.value)), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth(0.7f)
                .height(180.dp)
                .graphicsLayer {
                    scaleY = 0.4f + 0.6f * fold.value
                    alpha = fold.value
                }
                .shadow(2.dp, RoundedCornerShape(4.dp))
                .background(c.letterEnvelope, RoundedCornerShape(4.dp))
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val p = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(w, 0f)
                        lineTo(w * 0.57f, h * 0.5f)
                        quadraticTo(w * 0.5f, h * 0.58f, w * 0.43f, h * 0.5f)
                        close()
                    }
                    drawPath(p, c.letterFlap)
                    drawPath(p, c.letterEdge, style = Stroke(1.dp.toPx()))
                },
        ) {
            WaxSeal(colour, Modifier.align(Alignment.TopCenter).offset(y = 76.dp).graphicsLayer {
                    scaleX = seal.value
                    scaleY = seal.value
                }, size = 30.dp)
        }
    }
}

/**
 * S5 Send confirm (FS-S1–S3; canvas W-Send): arrival time and "in N min", until when it can be
 * edited or cancelled, the frozen mood. [onSend] returns an error line, or null when done.
 */
@Composable
private fun SendSheet(delay: String, window: String, feeling: String?, onClose: () -> Unit, onSend: suspend () -> String?) {
    val scope = rememberCoroutineScope()
    val nowMs = rememberNow()
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val delayMs = Time.isoDurationMs(delay) ?: 3_600_000L
    val windowMs = Time.isoDurationMs(window) ?: 300_000L
    DlBottomSheet(onDismissRequest = { if (!sending) onClose() }) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(WebIcons.Envelope, null)
            }
            Text("Send this letter?", style = MaterialTheme.typography.headlineSmall)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Arrives at", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(Time.timeOf(nowMs + delayMs), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"))
                    Text(" · in ${Math.round(delayMs / 60_000.0)} min", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                buildAnnotatedString {
                    append("Until ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(Time.timeOf(nowMs + windowMs))
                    pop()
                    append(" you can still edit or cancel it from Letters or Home.")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (feeling != null) Text("Feeling $feeling", style = MaterialTheme.typography.bodyMedium)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlOutlinedButton("Keep writing", onClose, Modifier.weight(1f), enabled = !sending)
                DlFilledButton(
                    if (sending) "Sending…" else "Send",
                    {
                        sending = true
                        error = null
                        scope.launch {
                            val failed = onSend()
                            if (failed != null) {
                                error = failed
                                sending = false
                            }
                        }
                    },
                    Modifier.weight(1f),
                    enabled = !sending,
                )
            }
        }
    }
}

/**
 * The letter being answered (FS-W5, Replies A1–A4; canvas W-Draft): a note slip clipped to the
 * sheet. ‹ newer and › older step through received letters and move the reply; × makes it a plain
 * letter; Hide folds it to a link (remembered on this phone).
 */
@Composable
private fun ReferencePanel(vm: WriteViewModel, friend: String) {
    val c = DlTheme.colors
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("device", Context.MODE_PRIVATE) }
    var hidden by remember { mutableStateOf(prefs.getBoolean("referencePanelHidden", false)) }
    val setHidden = { h: Boolean ->
        hidden = h
        prefs.edit().putBoolean("referencePanelHidden", h).apply()
    }
    val id = vm.pinned ?: return
    LaunchedEffect(Unit) { vm.loadReceived() }
    val rows = vm.received
    val index = rows.indexOfFirst { it.id == id }
    LaunchedEffect(id, index, vm.receivedMore) { if (index == -1 && vm.receivedMore) vm.loadReceived() }
    LaunchedEffect(id, hidden) { if (!hidden) vm.loadReferenced(id) }
    if (hidden) {
        TextButton(onClick = { setHidden(false) }, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Show $friend's letter") }
        return
    }
    val olderId = rows.getOrNull(index + 1)?.id
    val newerId = if (index > 0) rows[index - 1].id else null
    val l = vm.referenced?.takeIf { it.id == id }
    val feeling = l?.let { if (!it.moodEmoji.isNullOrEmpty() && !it.moodLabel.isNullOrEmpty()) "${it.moodEmoji} ${it.moodLabel}" else Moods.text(it.moodAtSend) }
    val meta = listOfNotNull(friend, feeling?.let { "Feeling $it" }, l?.let { Time.clock(it.arrivesAt) }).joinToString(" · ")
    Box(Modifier.padding(start = 14.dp, end = 12.dp, top = 10.dp)) {
        Column(
            Modifier
                .fillMaxWidth()
                .shadow(2.dp, RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                .background(c.letterSlip)
                .border(1.dp, c.letterEdge)
                .padding(start = 16.dp, end = 10.dp, top = 10.dp, bottom = 12.dp)
                .semantics { contentDescription = "The letter you are answering" },
        ) {
            Text(
                "↰ REPLYING TO",
                Modifier.padding(start = 14.dp),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.07.em),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                l?.stamp?.let { StampImage(it, width = 18.dp) }
                Text(
                    meta,
                    Modifier.weight(1f).padding(start = 6.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = { newerId?.let(vm::pin) }, enabled = newerId != null) { Icon(WebIcons.Back, "Newer received letter", Modifier.size(18.dp)) }
                IconButton(onClick = { olderId?.let(vm::pin) ?: vm.loadReceived() }, enabled = olderId != null || vm.receivedMore) {
                    Icon(WebIcons.Forward, "Older received letter", Modifier.size(18.dp))
                }
                IconButton(onClick = { vm.pin(null) }) { Icon(WebIcons.Close, "Not a reply: send as a new letter", Modifier.size(12.dp)) }
                TextButton(onClick = { setHidden(true) }) { Text("Hide", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
            }
            if (l != null) {
                Box(Modifier.height(80.dp)) {
                    LetterBody(l.body, style = DlTheme.type.letterBody.copy(fontSize = 14.sp, lineHeight = 20.sp))
                }
            } else {
                Box(Modifier.fillMaxWidth().height(40.dp))
            }
        }
    }
}
