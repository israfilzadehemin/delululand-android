package com.delulutopia.feature.discussions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.NoteColor
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.DComment
import com.delulutopia.core.network.model.DFull
import com.delulutopia.core.ui.EditDiscussionRoute
import com.delulutopia.core.ui.LetterBody
import com.delulutopia.core.ui.Lightbox
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.Note
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.media.AudioPlayer
import com.delulutopia.core.ui.noteColorOf
import com.delulutopia.core.ui.noteInk
import com.delulutopia.core.ui.noteInkQuiet
import com.delulutopia.core.ui.reactions.Reactions
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tintOf
import kotlinx.coroutines.launch

/**
 * S11 (FS-D9–D17; web discussions/DiscussionView.tsx; canvas D-Thread, K-Menu, D-Comment-*): the
 * note with title, author, times, body, attachments and reactions; the take-back bar for the author;
 * the archived bar; the comments on small notes in the discussion's colour, and the composer.
 */
@Composable
fun DiscussionScreen(id: Long) {
    val vm = hiltViewModel<DiscussionViewModel, DiscussionViewModel.Factory>(key = "discussion-$id", creationCallback = { it.create(id) })
    val nav = LocalAppNav.current
    val home by vm.home.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().imePadding()) {
        DlTopAppBar(title = "Discussion", onBack = { nav.back() }, backDescription = "Back to Discussions")
        when (val ui = vm.ui) {
            DiscussionViewModel.Ui.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is DiscussionViewModel.Ui.NotArrived -> Gone("Arrives at ${Time.clock(ui.arrivesAt)}")
            DiscussionViewModel.Ui.Gone -> Gone("This discussion isn't here anymore.")
            DiscussionViewModel.Ui.Unreachable -> Gone("Can't reach ${LocalBrand.current}. Try again.")
            is DiscussionViewModel.Ui.Shown -> Thread(ui.d, home.home?.me, home.home?.friend, vm)
        }
    }
}

@Composable
private fun Gone(text: String) {
    val nav = LocalAppNav.current
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DlOutlinedButton("Back", { nav.back() })
    }
}

@Composable
private fun Thread(d: DFull, me: Citizen?, friend: Citizen?, vm: DiscussionViewModel) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val base = LocalServerBaseUrl.current
    val nowMs = rememberNow()
    val author = if (d.mine) me else friend
    val state = LetterState.discussion(nowMs, d.windowEndsAt, d.arrivesAt)
    val arrived = state == LetterState.ARRIVED
    val inReach = d.mine && state == LetterState.IN_REACH
    val photos = d.attachments.filter { it.kind == "photo" }
    val audios = d.attachments.filter { it.kind == "audio" }
    val archived = d.archivedAt != null
    val colour = noteColorOf(d.color)
    var photo by rememberSaveable { mutableStateOf(-1) }
    var showEditedAt by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    var askTakeBack by remember { mutableStateOf(false) }
    var askDelete by remember { mutableStateOf(false) }
    var deletingComment by remember { mutableStateOf<DComment?>(null) }
    var busy by remember { mutableStateOf(false) }
    val archivedBy = when (d.archivedBy) {
        me?.id -> "you"
        friend?.id -> friend?.nickname ?: "your friend"
        else -> ""
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        if (inReach) {
            Bar("${Time.countdown((Time.ms(d.windowEndsAt) ?: 0) - nowMs)} left to take it back") { DlOutlinedButton("Cancel", { askTakeBack = true }) }
        }
        if (archived) {
            Bar("Archived${if (archivedBy.isNotEmpty()) " by $archivedBy" else ""} · ${Time.day(d.archivedAt)}") {
                if (d.canUnarchive) DlOutlinedButton("Unarchive", { scope.launch { notice(vm.unarchive()) } })
            }
        }
        Note(colour, Modifier.padding(horizontal = 12.dp, vertical = 12.dp), pin = 24.dp) {
            Column(
                Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 28.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(d.title, Modifier.weight(1f), style = DlTheme.type.noteTitle, color = noteInk)
                    val items = buildList {
                        if (d.canEdit) add("Edit")
                        if (d.canArchive) add("Archive")
                        if (d.canUnarchive) add("Unarchive")
                        if (d.canDelete) add("Delete")
                    }
                    if (items.isNotEmpty()) {
                        Box {
                            Text(
                                "⋯",
                                Modifier.clip(RoundedCornerShape(20.dp)).clickable { menu = true }.padding(horizontal = 12.dp, vertical = 4.dp).semantics {
                                    contentDescription =
                                        "Discussion options"
                                },
                                fontSize = 22.sp,
                                color = noteInk,
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                items.forEach { label ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                label,
                                                color = if (label ==
                                                    "Delete"
                                                ) {
                                                    MaterialTheme.colorScheme.error
                                                } else {
                                                    MaterialTheme.colorScheme.onSurface
                                                },
                                            )
                                        },
                                        onClick = {
                                            menu = false
                                            when (label) {
                                                "Edit" -> nav.open(EditDiscussionRoute(d.id))
                                                "Archive" -> scope.launch { notice(vm.archive()) }
                                                "Unarchive" -> scope.launch { notice(vm.unarchive()) }
                                                "Delete" -> askDelete = true
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (author !=
                        null
                    ) {
                        DlRemoteAvatar(
                            author.avatarUrl,
                            author.nickname,
                            tintOf(author.id),
                            size = 44.dp,
                            initials = author.initials(),
                            ring = signatureOf(author.id),
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            author?.nickname.orEmpty(),
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                            color = noteInk,
                        )
                        Row {
                            Text(
                                "Posted ${Time.clock(d.postedAt, nowMs)} · ${if (arrived) "Arrived" else "Arrives"} ${Time.clock(d.arrivesAt, nowMs)}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = noteInkQuiet,
                            )
                            if (d.editedAt != null) {
                                val label = "Edited ${Time.clock(d.editedAt, nowMs)}"
                                Text(
                                    " · ${if (showEditedAt) label else "Edited"}",
                                    Modifier.clickable { showEditedAt = !showEditedAt }.semantics { contentDescription = label },
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = noteInkQuiet,
                                )
                            }
                        }
                    }
                }
                LetterBody(d.body, colour = noteInk)
                if (photos.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        photos.forEachIndexed { i, p ->
                            AsyncImage(
                                model = resolveMedia(base, p.thumbUrl ?: p.url),
                                contentDescription = "Photo ${i + 1} of ${photos.size}, open full size",
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(4.dp)).clickable { photo = i },
                                contentScale = ContentScale.Crop,
                            )
                        }
                    }
                }
                audios.forEachIndexed { i, a ->
                    AudioPlayer(resolveMedia(base, a.url), a.durationMs, label = if (audios.size > 1) "voice note ${i + 1}" else "voice note")
                }
                if (!arrived) {
                    Text(
                        "Arrives for ${friend?.nickname ?: "your friend"} in ${Time.countdown((Time.ms(d.arrivesAt) ?: 0) - nowMs)}",
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = noteInk,
                    )
                }
                Reactions("discussions", d.id, d.reactions, me, friend, readOnly = archived, onChanged = vm::reacted)
            }
        }
        Comments(d, me, friend, archived, nowMs, vm) { deletingComment = it }
    }

    if (photo >= 0 && photo < photos.size) Lightbox(photos, photo, { photo = it }, { photo = -1 })
    if (askTakeBack) {
        DlDialog(
            title = "Take it back?",
            text = "This deletes the discussion and every comment on it. It can't be undone.",
            confirmLabel = "Yes, take it back",
            onConfirm = {
                busy = true
                scope.launch {
                    val failed = vm.remove(takeBack = true)
                    busy = false
                    askTakeBack = false
                    if (failed == null) nav.back() else notice(failed)
                }
            },
            dismissLabel = "No",
            onDismiss = { askTakeBack = false },
            busy = busy,
        )
    }
    if (askDelete) {
        DlDialog(
            title = "Delete this discussion?",
            text = "It disappears for both of you, with every comment and reaction. It can't be undone.",
            confirmLabel = "Yes, delete",
            onConfirm = {
                busy = true
                scope.launch {
                    val failed = vm.remove(takeBack = false)
                    busy = false
                    askDelete = false
                    if (failed == null) nav.back() else notice(failed)
                }
            },
            dismissLabel = "No",
            onDismiss = { askDelete = false },
            busy = busy,
        )
    }
    deletingComment?.let { c ->
        DlDialog(
            title = "Delete this comment?",
            text = "It disappears for both of you, with its reactions. It can't be undone.",
            confirmLabel = "Yes, delete",
            onConfirm = {
                busy = true
                scope.launch {
                    vm.delete(c)?.let(notice)
                    busy = false
                    deletingComment = null
                }
            },
            dismissLabel = "No",
            onDismiss = { deletingComment = null },
            busy = busy,
        )
    }
}

@Composable
private fun Bar(text: String, action: @Composable () -> Unit) {
    Row(
        Modifier
            .padding(start = 12.dp, end = 12.dp, top = 4.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text,
            Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
        )
        action()
    }
}

/** The comments (FS-D12–D15): count, "See older comments", small notes oldest first, the composer. */
@Composable
private fun Comments(d: DFull, me: Citizen?, friend: Citizen?, readOnly: Boolean, nowMs: Long, vm: DiscussionViewModel, onDelete: (DComment) -> Unit) {
    val scope = rememberCoroutineScope()
    val items = vm.comments
    val cursor = vm.cursor
    val total = d.commentCount ?: d.comments.items.size
    val count = if (cursor != null) maxOf(total, items.size) else items.size
    val colour = noteColorOf(d.color)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            when (count) {
                0 -> "No comments yet"
                1 -> "1 comment"
                else -> "$count comments"
            },
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        )
        if (cursor != null) {
            DlOutlinedButton(if (vm.loadingOlder) "Loading older comments…" else "See older comments", vm::seeOlder, enabled = !vm.loadingOlder)
        } else if (items.size > 5) {
            Text("That is everything", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (vm.olderFailed) {
            Text(
                "Couldn't load older comments. Try again.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        items.forEach { c ->
            val person = if (c.authorId == me?.id) {
                me
            } else if (c.authorId == friend?.id) {
                friend
            } else {
                null
            }
            CommentItem(c, person, mine = c.authorId == me?.id, readOnly, colour, nowMs, me, friend, vm, onDelete)
        }
        if (readOnly) {
            Text("Archived. Unarchive it to comment.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            var text by rememberSaveable { mutableStateOf("") }
            var busy by remember { mutableStateOf(false) }
            var error by remember { mutableStateOf<String?>(null) }
            Field(text, { text = it }, "Write a comment", enabled = !busy)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                error?.let { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                DlFilledButton(
                    if (busy) "Posting…" else "Post",
                    {
                        busy = true
                        error = null
                        scope.launch {
                            val failed = vm.post(text)
                            if (failed == null) text = "" else error = failed
                            busy = false
                        }
                    },
                    enabled = text.isNotBlank() && !busy,
                    height = 40.dp,
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    c: DComment,
    person: Citizen?,
    mine: Boolean,
    readOnly: Boolean,
    colour: NoteColor,
    nowMs: Long,
    me: Citizen?,
    friend: Citizen?,
    vm: DiscussionViewModel,
    onDelete: (DComment) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var showEditedAt by remember { mutableStateOf(false) }
    var menu by remember { mutableStateOf(false) }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        if (person != null) {
            DlRemoteAvatar(
                person.avatarUrl,
                person.nickname,
                tintOf(person.id),
                size = 24.dp,
                initials = person.nickname.take(1).uppercase(),
                ring = signatureOf(person.id),
            )
        }
        Note(colour, Modifier.weight(1f), pin = 14.dp) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 14.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(person?.nickname.orEmpty(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = noteInk)
                    Text(Time.clock(c.createdAt, nowMs), style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = noteInkQuiet)
                    if (c.editedAt != null) {
                        val label = "Edited ${Time.clock(c.editedAt, nowMs)}"
                        Text(
                            if (showEditedAt) "(edited ${Time.clock(c.editedAt, nowMs)})" else "(edited)",
                            Modifier.clickable { showEditedAt = !showEditedAt }.semantics { contentDescription = label },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = noteInkQuiet,
                        )
                    }
                    Box(Modifier.weight(1f))
                    if (mine && !editing && !readOnly) {
                        Box {
                            Text(
                                "⋯",
                                Modifier.clip(RoundedCornerShape(16.dp)).clickable { menu = true }.padding(horizontal = 10.dp).semantics {
                                    contentDescription =
                                        "Comment options"
                                },
                                fontSize = 18.sp,
                                color = noteInk,
                            )
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                DropdownMenuItem(text = { Text("Edit") }, onClick = {
                                    menu = false
                                    editing = true
                                })
                                DropdownMenuItem(text = { Text("Delete", color = MaterialTheme.colorScheme.error) }, onClick = {
                                    menu = false
                                    onDelete(c)
                                })
                            }
                        }
                    }
                }
                if (editing) {
                    var text by remember { mutableStateOf(c.text) }
                    var busy by remember { mutableStateOf(false) }
                    var error by remember { mutableStateOf<String?>(null) }
                    Field(text, { text = it }, "Edit comment", enabled = !busy)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        error?.let { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        DlOutlinedButton("Cancel", { editing = false }, enabled = !busy)
                        DlFilledButton(
                            if (busy) "Saving…" else "Save",
                            {
                                busy = true
                                error = null
                                scope.launch {
                                    val failed = vm.edit(c, text)
                                    busy = false
                                    if (failed == null) editing = false else error = failed
                                }
                            },
                            enabled = text.isNotBlank() && !busy,
                            height = 40.dp,
                        )
                    }
                } else {
                    Text(c.text, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 21.sp), color = noteInk)
                }
                Reactions("comments", c.id, c.reactions, me, friend, readOnly = readOnly, onChanged = vm::reacted)
            }
        }
    }
}

/** A plain multi-line field on the note (Enter is a line break). */
@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean) {
    val c = DlTheme.colors
    BasicTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .background(c.letterSlip, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .semantics { contentDescription = label },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.letterInk),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                inner()
            }
        },
    )
}
