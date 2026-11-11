package com.delulutopia.feature.discussions

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.NoteColor
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.ui.DiscussionRoute
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EmojiPickerSheet
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.PICKABLE_NOTES
import com.delulutopia.core.ui.editor.EditorToolbar
import com.delulutopia.core.ui.editor.RichEditor
import com.delulutopia.core.ui.editor.RichEditorState
import com.delulutopia.core.ui.editor.rememberRichEditor
import com.delulutopia.core.ui.media.AttachmentStrip
import com.delulutopia.core.ui.media.Item
import com.delulutopia.core.ui.media.Source
import com.delulutopia.core.ui.media.VoiceState
import com.delulutopia.core.ui.media.rememberVoiceRecording
import com.delulutopia.core.ui.noteInk
import com.delulutopia.core.ui.noteInkQuiet
import com.delulutopia.core.ui.rememberNow
import kotlinx.coroutines.launch

/**
 * S12 Compose and editing a discussion (web discussions/ComposeScreen.tsx, EditDiscussionScreen.tsx;
 * canvas D-Compose, K-Edit): the title above the editor on a sticky note in the colour picked, the
 * strip and the formatting bar. Post is on when the title and the body both have something.
 */
@Composable
fun ComposeScreen(editId: Long?) {
    val vm = hiltViewModel<ComposeViewModel, ComposeViewModel.Factory>(key = "compose-$editId", creationCallback = { it.create(editId) })
    val nav = LocalAppNav.current
    when (val load = vm.load) {
        ComposeViewModel.Load.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        ComposeViewModel.Load.Failed -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Text("Couldn't load your draft. Check your connection and try again.", color = MaterialTheme.colorScheme.error)
            DlOutlinedButton("Back", { nav.back() })
        }
        is ComposeViewModel.Load.NotEditable -> LaunchedEffect(load.id) { nav.replace(DiscussionRoute(load.id)) }
        is ComposeViewModel.Load.Ready -> {
            val editor = rememberRichEditor(load.body) { vm.onBody(it) }
            if (editId == null) Compose(vm, editor) else Edit(vm, editor)
        }
    }
}

@Composable
private fun Compose(vm: ComposeViewModel, editor: RichEditorState) {
    val nav = LocalAppNav.current
    val scope = rememberCoroutineScope()
    val home by vm.homeState.collectAsStateWithLifecycle()
    val friend = home.home?.friend?.nickname ?: "Friend"
    val saver = vm.saver!!
    val saveState by saver.state.collectAsStateWithLifecycle()
    var postOpen by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.flushOnHide() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { vm.checkNewer()?.let(editor::set) } }
    val atts = vm.attachments!!
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val canPost = vm.title.isNotBlank() && !vm.empty && !atts.uploading && !voice.recording
    val savedLine = when {
        saveState.status == DraftSaver.Status.Error -> "Couldn't save your draft"
        saveState.savedAt != null && saveState.status != DraftSaver.Status.Conflict -> "Draft saved ${Time.time(saveState.savedAt)}"
        else -> ""
    }
    Frame(
        vm,
        editor,
        title = "New discussion",
        subtitle = savedLine,
        subtitleError = saveState.status == DraftSaver.Status.Error,
        backLabel = "Back to Discussions",
        onBack = {
            vm.flushOnHide()
            nav.back()
        },
        action = "Post",
        actionEnabled = canPost,
        onAction = { postOpen = true },
        voice = voice,
        onRemove = { item, index ->
            atts.remove(item.key)
            item.att?.let { a -> vm.deleteDraftAttachment(a.id) { atts.restore(item, index) } }
        },
    )
    if (postOpen) {
        PostSheet(
            friend = friend,
            delay = vm.config?.discussionDelay ?: "PT60M",
            window = vm.config?.window ?: "PT5M",
            onClose = { postOpen = false },
        ) {
            when (val r = vm.post()) {
                is ComposeViewModel.PostResult.Posted -> {
                    postOpen = false
                    nav.top(DiscussionsRoute)
                    nav.open(DiscussionRoute(r.id))
                    null
                }
                ComposeViewModel.PostResult.Conflict -> {
                    postOpen = false
                    null
                }
                is ComposeViewModel.PostResult.Failed -> r.message
            }
        }
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
private fun Edit(vm: ComposeViewModel, editor: RichEditorState) {
    val nav = LocalAppNav.current
    val scope = rememberCoroutineScope()
    val d = vm.original!!
    val atts = vm.attachments!!
    val voice = rememberVoiceRecording { bytes, ms -> atts.add(Uploader.Kind.Audio, Source.Made(bytes, "audio/mp4"), "voice.m4a", ms) }
    val initialIds = remember { d.attachments.joinToString(",") { it.id } }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirm by remember { mutableStateOf(false) }
    val changed = vm.title.trim() != d.title || vm.colour.name.lowercase() != (d.color ?: "lemon") || vm.body != d.body ||
        atts.done.joinToString(",") { it.id } != initialIds || atts.uploading
    val canSave = !busy && vm.title.isNotBlank() && !vm.empty && !atts.uploading && !voice.recording
    val back = { if (changed) confirm = true else nav.back() }
    BackHandler { back() }
    val arrived = d.state == "ARRIVED"
    Frame(
        vm,
        editor,
        title = "Editing a discussion",
        subtitle = "${if (arrived) "Arrived" else "Arrives"} at ${Time.clock(d.arrivesAt)}${if (arrived) "" else " whatever you change"}",
        subtitleError = false,
        backLabel = "Back, discard changes",
        onBack = back,
        action = "Save",
        actionEnabled = canSave,
        onAction = {
            if (!changed) {
                nav.back()
            } else {
                busy = true
                error = null
                scope.launch {
                    when (val failed = vm.save()) {
                        null -> nav.back()
                        ComposeViewModel.GONE -> nav.top(DiscussionsRoute)
                        else -> {
                            busy = false
                            error = failed
                        }
                    }
                }
            }
        },
        voice = voice,
        onRemove = { item, _ -> atts.remove(item.key) },
        error = error,
    )
    if (confirm) {
        DlDialog(
            title = "Discard your changes?",
            text = null,
            confirmLabel = "Yes",
            onConfirm = {
                confirm = false
                scope.launch {
                    vm.discardEdit()
                    nav.back()
                }
            },
            dismissLabel = "No",
            onDismiss = { confirm = false },
        )
    }
}

@Composable
private fun Frame(
    vm: ComposeViewModel,
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
    error: String? = null,
) {
    var emojiOpen by remember { mutableStateOf(false) }
    val pair = DlTheme.colors.notes.getValue(vm.colour)
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(DlIcons.Back, backLabel) }
            Column(Modifier.weight(1f).padding(start = 4.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = if (subtitleError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            DlFilledButton(action, onAction, Modifier.padding(start = 4.dp, end = 12.dp), enabled = actionEnabled, height = 40.dp)
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(0.62f to pair.face, 1f to pair.deep))
                .verticalScroll(rememberScrollState())
                .padding(start = 18.dp, end = 16.dp, top = 12.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            BasicTextField(
                value = vm.title,
                onValueChange = vm::changeTitle,
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Title" },
                singleLine = true,
                textStyle = DlTheme.type.noteTitle.copy(color = noteInk),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (vm.title.isEmpty()) Text("Title", style = DlTheme.type.noteTitle, color = noteInkQuiet)
                        inner()
                    }
                },
            )
            ColourPicker(vm.colour, vm::changeColour)
            RichEditor(editor, colour = noteInk, label = "Discussion text", minHeight = 220.dp)
        }
        AttachmentStrip(attachments = vm.attachments!!, onRemove = onRemove, voice = voice)
        EditorToolbar(editor, Modifier.background(DlTheme.colors.letterEnvelope)) { emojiOpen = true }
    }
    if (emojiOpen) EmojiPickerSheet("Emoji", onPick = { editor.insert(it) }, onDismiss = { emojiOpen = false })
}

/** The note colour (Materials story): six swatches as one radio group. */
@Composable
private fun ColourPicker(value: NoteColor, onChange: (NoteColor) -> Unit) {
    Row(Modifier.semantics { contentDescription = "Note colour" }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        PICKABLE_NOTES.forEach { c ->
            val pair = DlTheme.colors.notes.getValue(c)
            val on = c == value
            Box(
                Modifier
                    .size(32.dp)
                    .background(pair.face, CircleShape)
                    .border(if (on) 3.dp else 1.dp, if (on) MaterialTheme.colorScheme.primary else pair.deep, CircleShape)
                    .clickable(role = Role.RadioButton) { onChange(c) }
                    .semantics {
                        contentDescription = c.name
                        selected = on
                    },
            )
        }
    }
}

/** FS-D21: "Post this discussion?" with the arrival and the end of the take-back window. */
@Composable
private fun PostSheet(friend: String, delay: String, window: String, onClose: () -> Unit, onPost: suspend () -> String?) {
    val scope = rememberCoroutineScope()
    val nowMs = rememberNow()
    var posting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val delayMs = Time.isoDurationMs(delay) ?: 3_600_000L
    val windowMs = Time.isoDurationMs(window) ?: 300_000L
    DlBottomSheet(onDismissRequest = { if (!posting) onClose() }) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                Icon(WebIcons.Discussions, null)
            }
            Text("Post this discussion?", style = MaterialTheme.typography.headlineSmall)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Arrives for $friend at", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        Time.timeOf(nowMs + delayMs),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    )
                    Text(
                        " · in ${Math.round(delayMs / 60_000.0)} min",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                buildAnnotatedString {
                    append("Until ")
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    append(Time.timeOf(nowMs + windowMs))
                    pop()
                    append(" you can still take it back.")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DlOutlinedButton("Keep writing", onClose, Modifier.weight(1f), enabled = !posting)
                DlFilledButton(
                    if (posting) "Posting…" else "Post",
                    {
                        posting = true
                        error = null
                        scope.launch {
                            val failed = onPost()
                            if (failed != null) {
                                error = failed
                                posting = false
                            }
                        }
                    },
                    Modifier.weight(1f),
                    enabled = !posting,
                )
            }
        }
    }
}
