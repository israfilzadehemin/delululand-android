package com.delulutopia.feature.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Board
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NewChip
import com.delulutopia.core.ui.ageWords
import com.delulutopia.core.ui.doodle.DoodlePad
import com.delulutopia.core.ui.doodle.Surfaces
import com.delulutopia.core.ui.signatureOf
import kotlinx.coroutines.launch

private const val COMMENT_MAX = 140
private const val COUNT_FROM = 120

private fun commentLength(text: String): Int = text.trim().replace(Regex("\\s+"), " ").let { it.codePointCount(0, it.length) }

private class Pending(val png: ByteArray, val current: Board, val edit: Boolean)

/**
 * The shared board (web board/BoardTile.tsx, doc "Delululand — Board"): a whiteboard tile at the
 * top of Home holding the one doodle alive, which either user replaces (B1, B11). Tap it to see it
 * full screen (B8); Draw opens the pad on a blank 4:3 surface (B3, B5); a post over a doodle that
 * changed meanwhile asks first (B10). The friend's new doodle carries NEW until Home has shown it
 * (B12). Reactions (§R), Edit (§E) and the friend's sticky note (§C). Folds to a strip (§9).
 */
@Composable
internal fun BoardTile(home: Home, nowMs: Long, vm: HomeActions) {
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val base = LocalServerBaseUrl.current
    val mini by vm.boardMini.collectAsStateWithLifecycle()
    val board = home.board
    val friend = home.friendName
    var drawing by remember { mutableStateOf<String?>(null) }
    var viewing by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<Pending?>(null) }
    var picking by remember { mutableStateOf(false) }
    var drawn by remember { mutableStateOf<String?>(null) }

    // B12, Q5: remember which version was NEW when Home showed it; tell the server once, unfolded.
    var ribbon by remember { mutableStateOf(if (home.boardNew && board != null) board.version else null) }
    if (home.boardNew && board != null && ribbon != board.version) ribbon = board.version
    LaunchedEffect(mini, home.boardNew, board?.version) {
        if (!mini && home.boardNew && board != null) vm.boardSeen(board.version)
    }

    val nameOf = { authorId: Int -> if (authorId == home.me.id) "You" else friend }
    val didWhat = { b: Board ->
        when (b.editedFrom) {
            null -> "drew this"
            b.authorId -> "edited this"
            else -> "edited ${if (b.editedFrom == home.me.id) "your" else "$friend's"} doodle"
        }
    }
    val canReact = board != null && board.authorId != home.me.id
    val mark = reactionOf(board?.reaction)
    val whose = board?.let { if (it.authorId == home.me.id) "your doodle" else "$friend's doodle" }

    val post: (ByteArray, Int, Boolean) -> Unit = { png, version, edit ->
        busy = true
        scope.launch {
            when (val r = vm.postBoard(png, version, edit)) {
                HomeActions.PostResult.Posted -> {
                    ribbon = null
                    conflict = null
                    drawing = null
                }
                is HomeActions.PostResult.Conflict -> conflict = Pending(png, r.current, edit)
                is HomeActions.PostResult.Failed -> notice(r.message)
            }
            busy = false
        }
    }
    val react: (String?) -> Unit = { key ->
        picking = false
        if (board != null && key != board.reaction) {
            drawn = key?.let { "${board.version}:$it" }
            scope.launch { vm.react(board, key, friend)?.let(notice) }
        }
    }

    Column(Modifier.fillMaxWidth().animateContentSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        when {
            mini -> Strip(home, board, nowMs, base, nameOf) { vm.setBoardMini(false) }
            board != null -> {
                Box(Modifier.fillMaxWidth()) {
                    Frame(
                        Modifier.clickable(onClickLabel = "Open full screen") { viewing = true }
                            .semantics { contentDescription = "Board: ${if (board.authorId == home.me.id) "your" else "$friend's"} doodle" },
                    ) {
                        BoardImage(board, base)
                        if (ribbon == board.version) NewChip(Modifier.padding(8.dp), paper = false)
                    }
                    Corner(board, canReact, mark, drawn, friend, Modifier.align(Alignment.TopEnd).padding(6.dp)) { picking = true }
                }
                BoardNote(board, home, vm)
                // The status line on its own row, the actions under it at the end (web .dl-board-meta wraps).
                Row(
                    Modifier.fillMaxWidth().padding(start = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WhoDot(board.authorId)
                    val reacted = when {
                        board.reaction == null -> ""
                        canReact -> " · you reacted"
                        else -> " · $friend reacted"
                    }
                    Text(
                        buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)) {
                                append(nameOf(board.authorId))
                            }
                            append(" ${didWhat(board)} · ${ageWords(board.createdAt, nowMs)}$reacted")
                        },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().offset(x = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                ) {
                    DrawButton(WebIcons.Doodle, "Edit", "Edit $whose") { drawing = "edit" }
                    DrawButton(WebIcons.Write, "Draw", null) { drawing = "new" }
                    IconButton(onClick = { vm.setBoardMini(true) }) { Icon(WebIcons.ChevronUp, "Fold the board") }
                }
            }
            else -> Box(Modifier.fillMaxWidth()) {
                Frame(Modifier) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("The board is empty", style = DlTheme.type.letterBody.copy(fontSize = 17.sp), color = Color(0xFF48536C))
                        DlFilledButton("Draw the first doodle", { drawing = "new" }, icon = WebIcons.Write)
                    }
                }
                IconButton(onClick = {
                    vm.setBoardMini(true)
                }, Modifier.align(Alignment.BottomEnd).offset(y = 8.dp)) { Icon(WebIcons.ChevronUp, "Fold the board") }
            }
        }
    }

    if (viewing && board != null) {
        BoardViewer(
            board = board,
            home = home,
            vm = vm,
            canReact = canReact,
            onReact = { picking = true },
            onClose = { viewing = false },
            onDraw = { mode ->
                viewing = false
                drawing = mode
            },
        )
    }

    drawing?.let { mode ->
        val edit = mode == "edit" && board != null
        DoodlePad(
            onDone = { png -> post(png, board?.version ?: 0, edit) },
            onClose = {
                conflict = null
                drawing = null
            },
            surface = Surfaces.BOARD,
            title = if (edit) "Edit the board" else "Draw on the board",
            doneLabel = "Post",
            busyLabel = "Posting…",
            busy = busy,
            note = whose?.let { if (edit) "Posting replaces $it with your edit" else "Posting replaces $it" },
            board = true,
            backdrop = if (edit) board?.let { resolveMedia(base, it.url) } else null,
            onBackdropError = {
                drawing = null
                notice("Couldn't open the doodle. Try again.")
            },
        )
    }

    conflict?.let { c ->
        val mine = c.current.authorId == home.me.id
        DlBottomSheet(onDismissRequest = { conflict = null }) {
            Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("${if (mine) "You" else friend} just posted a new doodle", style = MaterialTheme.typography.titleLarge)
                Text(
                    "You were drawing over an older one. Posting now replaces it; keeping it discards your drawing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Frame(Modifier.width(200.dp)) { BoardImage(c.current, base) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DlOutlinedButton(
                        if (mine) "Keep it" else "Keep $friend's",
                        {
                            conflict = null
                            drawing = null
                        },
                        Modifier.weight(1f),
                        enabled = !busy,
                    )
                    DlFilledButton(if (busy) "Posting…" else "Replace it", { post(c.png, c.current.version, c.edit) }, Modifier.weight(1f), enabled = !busy)
                }
            }
        }
    }

    if (picking && board != null && canReact) {
        DlBottomSheet(onDismissRequest = { picking = false }) {
            Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("React to $friend's doodle", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
                REACTIONS.chunked(5).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { r ->
                            val on = board.reaction == r.key
                            Column(
                                Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button) { react(r.key) }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
                                    if (on) ChosenRing(MaterialTheme.colorScheme.onSurface, Modifier.fillMaxSize())
                                    HandReaction(r, 44.dp)
                                }
                                Text(r.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
                            }
                        }
                    }
                }
                if (board.reaction != null) {
                    DlOutlinedButton("Remove reaction", { react(null) }, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

/** The whiteboard: frame, 4:3 surface, the marker tray with two markers. */
@Composable
private fun Frame(modifier: Modifier, content: @Composable BoxScope.() -> Unit) {
    val c = DlTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(6.dp))
            .background(c.boardFrame, RoundedCornerShape(6.dp))
            .padding(start = 7.dp, end = 7.dp, top = 7.dp, bottom = 15.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(4f / 3f).clip(RoundedCornerShape(2.dp)).background(c.boardSurface),
            content = content,
        )
        Box(
            Modifier.align(
                Alignment.BottomCenter,
            ).offset(y = 11.dp).padding(horizontal = 21.dp).fillMaxWidth().height(6.dp).background(c.boardTray, RoundedCornerShape(3.dp)),
        ) {
            Box(Modifier.align(Alignment.BottomEnd).offset(x = (-16).dp, y = (-2).dp).size(34.dp, 6.dp).background(Color(0xFF202B45), RoundedCornerShape(3.dp)))
            Box(Modifier.align(Alignment.BottomEnd).offset(x = (-56).dp, y = (-2).dp).size(34.dp, 6.dp).background(Color(0xFFC96F52), RoundedCornerShape(3.dp)))
        }
    }
}

@Composable
private fun BoardImage(board: Board, base: String) {
    AsyncImage(
        model = resolveMedia(base, board.url),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
    )
}

/** §9: the folded strip, with NEW while unseen. */
@Composable
private fun Strip(home: Home, board: Board?, nowMs: Long, base: String, nameOf: (Int) -> String, onUnfold: () -> Unit) {
    val c = DlTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(8.dp))
            .background(c.boardFrame, RoundedCornerShape(8.dp))
            .clickable(onClickLabel = "Unfold the board", onClick = onUnfold)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(72.dp, 54.dp).clip(RoundedCornerShape(2.dp)).background(c.boardSurface)) {
            if (board != null) BoardImage(board, base)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Board", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold), color = Color(0xFF202B45))
                if (home.boardNew && board != null) NewChip()
            }
            Text(
                if (board !=
                    null
                ) {
                    "${nameOf(board.authorId)} ${if (board.editedFrom == null) "drew" else "edited"} · ${ageWords(board.createdAt, nowMs)}"
                } else {
                    "The board is empty"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF202B45).copy(alpha = 0.85f),
            )
        }
        Icon(WebIcons.ChevronDown, null, tint = Color(0xFF202B45))
    }
}

/** The reaction corner: the friend's slot or mark (tap to change), the author's read-only mark. */
@Composable
private fun Corner(board: Board, canReact: Boolean, mark: BoardReaction?, drawn: String?, friend: String, modifier: Modifier, onPick: () -> Unit) {
    if (canReact) {
        Box(
            modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onPick)
                .semantics { contentDescription = if (mark != null) "Your reaction: ${mark.name}. Change it" else "React to $friend's doodle" },
            contentAlignment = Alignment.Center,
        ) {
            if (mark != null) {
                HandReaction(mark, 64.dp, tilt = 8f, draw = drawn == "${board.version}:${mark.key}")
            } else {
                ReactSlot(Color(0xFF48536C).copy(alpha = 0.55f), Color(0xFF202B45))
            }
        }
    } else if (mark != null) {
        Box(modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp), contentAlignment = Alignment.Center) {
            HandReaction(mark, 64.dp, tilt = 8f, label = "$friend reacted: ${mark.name}")
        }
    }
}

@Composable
private fun WhoDot(authorId: Int) {
    Box(Modifier.size(14.dp).border(3.dp, signatureOf(authorId), CircleShape))
}

@Composable
private fun DrawButton(icon: ImageVector, text: String, description: String?, onClick: () -> Unit) {
    val seal = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, seal, RoundedCornerShape(22.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .then(if (description != null) Modifier.semantics { contentDescription = description } else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = seal)
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = seal)
    }
}

/**
 * The board comment (§C): one handwritten sticky note taped under the tray. Only the friend of the
 * doodle's author writes it, once; it can be deleted (not edited) and written again.
 */
@Composable
internal fun BoardNote(board: Board, home: Home, vm: HomeActions, viewer: Boolean = false) {
    val notice = LocalNotice.current
    val scope = rememberCoroutineScope()
    val friend = home.friendName
    val mine = board.authorId != home.me.id
    var text by remember(board.version) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val dark = DlTheme.isDark
    val face = if (dark) Color(0xFF4F4A2C) else Color(0xFFF6EAB0)
    val deep = if (dark) Color(0xFF45402A) else Color(0xFFECDC93)
    val ink = if (dark) Color(0xFFECE4D6) else Color(0xFF33323B)
    val tape = if (dark) Color(0xB3787062) else Color(0xC7ECE4D2)
    val comment = board.comment
    if (comment == null && !mine) return

    val noteModifier = Modifier
        .padding(start = if (viewer) 0.dp else 22.dp, top = 0.dp)
        .offset(y = (-12).dp)
        .fillMaxWidth(0.78f)
        .rotate(-1.5f)
    Box(noteModifier) {
        if (comment != null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp)
                    .shadow(3.dp, RoundedCornerShape(2.dp))
                    .background(Brush.verticalGradient(0.7f to face, 1f to deep), RoundedCornerShape(2.dp))
                    .padding(start = 16.dp, end = 40.dp, top = 16.dp, bottom = 10.dp),
            ) {
                Text(comment.text, style = DlTheme.type.boardNote.copy(fontSize = 23.sp), color = ink)
                Text(
                    "— ${if (mine) "You" else friend}",
                    Modifier.fillMaxWidth().padding(top = 4.dp),
                    style = DlTheme.type.boardNote.copy(fontSize = 18.sp),
                    color = ink.copy(alpha = 0.72f),
                    textAlign = TextAlign.End,
                )
            }
            if (mine) {
                IconButton(
                    onClick = {
                        if (!busy) {
                            busy = true
                            scope.launch {
                                vm.uncomment(board, friend)?.let(notice)
                                busy = false
                            }
                        }
                    },
                    modifier = Modifier.align(Alignment.TopEnd),
                    enabled = !busy,
                ) {
                    Box(Modifier.size(28.dp).background(ink.copy(alpha = 0.08f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(WebIcons.Close, "Delete your comment", Modifier.size(12.dp), tint = ink)
                    }
                }
            }
        } else {
            val length = commentLength(text)
            val tooLong = length > COMMENT_MAX
            val submit = {
                if (!busy && length > 0 && !tooLong) {
                    busy = true
                    scope.launch {
                        val failed = vm.comment(board, text, friend)
                        if (failed == null) text = "" else notice(failed)
                        busy = false
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(face.copy(alpha = 0.55f), RoundedCornerShape(2.dp))
                    .border(1.5.dp, deep, RoundedCornerShape(2.dp))
                    .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                BasicTextField(
                    value = text,
                    onValueChange = { text = it.replace(Regex("[\\r\\n]+"), " ") },
                    modifier = Modifier.weight(1f).semantics { contentDescription = "Comment on $friend's doodle" },
                    enabled = !busy,
                    singleLine = true,
                    textStyle = DlTheme.type.boardNote.copy(color = ink),
                    cursorBrush = SolidColor(ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { submit() }),
                    decorationBox = { field ->
                        Box {
                            if (text.isEmpty()) Text("Leave $friend a note…", style = DlTheme.type.boardNote, color = ink.copy(alpha = 0.55f))
                            field()
                        }
                    },
                )
                if (length >= COUNT_FROM) {
                    Text(
                        "${COMMENT_MAX - length}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (tooLong) (if (dark) Color(0xFFF0A39A) else Color(0xFFB23A3A)) else ink.copy(alpha = 0.7f),
                    )
                }
                val enabled = !busy && length > 0 && !tooLong
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = if (enabled) 1f else 0.35f))
                        .clickable(enabled = enabled, role = Role.Button) { submit() }
                        .semantics { contentDescription = "Post comment" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(WebIcons.Check, null, Modifier.size(16.dp), tint = Color.White)
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter).offset(y = (-9).dp).rotate(2f).size(64.dp, 18.dp).background(tape))
    }
}

/** B8: the doodle full screen, who drew it and when, React, Edit and Draw new. */
@Composable
private fun BoardViewer(board: Board, home: Home, vm: HomeActions, canReact: Boolean, onReact: () -> Unit, onClose: () -> Unit, onDraw: (String) -> Unit) {
    val c = DlTheme.colors
    val base = LocalServerBaseUrl.current
    val friend = home.friendName
    val mark = reactionOf(board.reaction)
    val name = if (board.authorId == home.me.id) "You" else friend
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = c.lightbox, contentColor = c.onLightbox) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(4.dp)) {
                    IconButton(onClick = onClose) { Icon(WebIcons.Close, "Close", tint = c.onLightbox) }
                }
                Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.Center) {
                    Box {
                        Frame(Modifier) {
                            AsyncImage(
                                model = resolveMedia(base, board.url),
                                contentDescription = "${if (name == "You") "Your" else "$name's"} doodle",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit,
                            )
                        }
                        if (mark != null) {
                            val by = if (canReact) "Your reaction" else "$friend reacted"
                            HandReaction(mark, 84.dp, Modifier.align(Alignment.TopEnd).padding(14.dp), tilt = 8f, label = "$by: ${mark.name}")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    BoardNote(board, home, vm, viewer = true)
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WhoDot(board.authorId)
                    Column(Modifier.weight(1f)) {
                        Text(name, style = MaterialTheme.typography.titleSmall, color = c.onLightbox)
                        Text(Time.clock(board.createdAt), style = MaterialTheme.typography.bodySmall, color = c.onLightboxQuiet)
                    }
                    if (canReact) {
                        Box(
                            Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onReact)
                                .semantics { contentDescription = if (mark != null) "Your reaction: ${mark.name}. Change it" else "React" },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (mark != null) HandReaction(mark, 28.dp) else ReactSlot(c.onLightbox, c.onLightbox, size = 32.dp)
                        }
                    }
                    ViewerButton(WebIcons.Doodle, "Edit", outlined = true) { onDraw("edit") }
                    ViewerButton(WebIcons.Write, "Draw new", outlined = false) { onDraw("new") }
                }
            }
        }
    }
}

@Composable
private fun ViewerButton(icon: ImageVector, text: String, outlined: Boolean, onClick: () -> Unit) {
    val c = DlTheme.colors
    Row(
        Modifier
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .then(if (outlined) Modifier.border(1.dp, c.onLightbox, RoundedCornerShape(22.dp)) else Modifier.background(c.onLightbox))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val ink = if (outlined) c.onLightbox else c.lightbox
        Icon(icon, null, Modifier.size(18.dp), tint = ink)
        Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = ink)
    }
}
