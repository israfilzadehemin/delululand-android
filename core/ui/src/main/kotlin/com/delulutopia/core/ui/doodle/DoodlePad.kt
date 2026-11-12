package com.delulutopia.core.ui.doodle

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The doodle pad (web doodle/DoodlePad.tsx; canvas W-Doodle, B-Pad): the surface scaled to the
 * screen, full screen. One finger or a stylus draws; a second finger is ignored (a resting palm).
 * Done exports the PNG; closing a pad with something on it asks first. The board reuses it on a 4:3
 * whiteboard with its own title, button label and a note under the canvas, and to edit the doodle on
 * it (§E): [backdrop] is drawn under the strokes, the eraser paints surface over it, Clear wipes it,
 * and the pad can post once anything changed.
 */
@Composable
fun DoodlePad(
    onDone: (ByteArray) -> Unit,
    onClose: () -> Unit,
    surface: Surface = Surfaces.LETTER,
    title: String = t("doodle.doodle"),
    doneLabel: String = t("doodle.done"),
    busyLabel: String? = null,
    busy: Boolean = false,
    note: String? = null,
    board: Boolean = false,
    backdrop: String? = null,
    onBackdropError: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf(History()) }
    var colour by remember { mutableIntStateOf(COLOURS[0].second) }
    var brush by remember { mutableIntStateOf(1) }
    var eraser by remember { mutableStateOf(false) }
    var askDiscard by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf(false) }
    var backImg by remember { mutableStateOf<Bitmap?>(null) }
    val waiting = backdrop != null && backImg == null
    val blank = if (backdrop != null) history.isUnchanged else history.isBlank

    LaunchedEffect(backdrop) {
        if (backdrop == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context).data(backdrop).allowHardware(false).build()
        val result = SingletonImageLoader.get(context).execute(request)
        if (result is SuccessResult) backImg = result.image.toBitmap() else onBackdropError()
    }

    // The rendered history, re-drawn when it changes; each frame adds only the live stroke on top.
    val base = remember(surface) { Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888) }
    var baseVersion by remember { mutableIntStateOf(0) }
    LaunchedEffect(history, backImg) {
        render(Canvas(base), history, surface, backImg)
        baseVersion++
    }
    var live by remember { mutableStateOf<Stroke?>(null) }
    var liveTick by remember { mutableIntStateOf(0) }

    val requestClose = { if (blank) onClose() else askDiscard = true }
    val done = done@{
        if (blank || exporting || busy || waiting) return@done
        exporting = true
        scope.launch {
            val png = withContext(Dispatchers.Default) { exportPng(history, surface, backImg) }
            exporting = false
            onDone(png)
        }
    }

    Dialog(onDismissRequest = requestClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = requestClose) { Icon(WebIcons.Close, t("doodle.close"), Modifier.size(20.dp)) }
                    Text(
                        title,
                        Modifier.weight(1f).padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    DlFilledButton(
                        text = if (busy && busyLabel != null) busyLabel else doneLabel,
                        onClick = done,
                        enabled = !(blank || exporting || busy || waiting),
                        height = 40.dp,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                    val ratio = surface.width.toFloat() / surface.height
                    val frame = if (board) 10.dp else 0.dp
                    val fitWidth = minOf(maxWidth, (maxHeight - frame * 2) * ratio + frame * 2)
                    Box(
                        Modifier
                            .width(fitWidth)
                            .then(if (board) Modifier.background(DlTheme.colors.boardFrame, RoundedCornerShape(8.dp)).padding(frame) else Modifier),
                    ) {
                        val label = when {
                            backdrop != null -> t("doodle.editing")
                            blank -> if (board) t("doodle.emptyBoard") else t("doodle.emptyPaper")
                            else -> t("doodle.yours")
                        }
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio)
                                .clip(RoundedCornerShape(2.dp))
                                .semantics { contentDescription = label }
                                .pointerInput(surface) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        if (exporting || busy || waiting) return@awaitEachGesture
                                        val width = SIZES[brush].second * if (eraser) ERASER_FACTOR else 1f
                                        val stroke = Stroke(colour, width, eraser)
                                        val w = this.size.width.toFloat()
                                        val h = this.size.height.toFloat()
                                        stroke.points += toCanvas(down.position.x, down.position.y, w, h, surface)
                                        live = stroke
                                        liveTick++
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                            var changed = false
                                            for (hp in change.historical) {
                                                val p = toCanvas(hp.position.x, hp.position.y, w, h, surface)
                                                changed = stroke.extend(p[0], p[1]) || changed
                                            }
                                            val p = toCanvas(change.position.x, change.position.y, w, h, surface)
                                            changed = stroke.extend(p[0], p[1]) || changed
                                            change.consume()
                                            if (changed) liveTick++
                                            if (!change.pressed) break
                                        }
                                        live = null
                                        history = history.add(stroke)
                                    }
                                }
                                .drawBehind {
                                    // Read both so a new history or a moved finger redraws.
                                    if (baseVersion < 0 || liveTick < 0) return@drawBehind
                                    drawIntoCanvas { c ->
                                        val n = c.nativeCanvas
                                        n.save()
                                        n.scale(size.width / surface.width, size.height / surface.height)
                                        n.drawBitmap(base, 0f, 0f, null)
                                        live?.let { drawStroke(n, it, surface.paper) }
                                        n.restore()
                                    }
                                },
                        )
                    }
                }
                Column(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    if (note != null) {
                        Text(
                            note,
                            Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        COLOURS.forEach { (name, value) ->
                            val on = !eraser && colour == value
                            Box(
                                Modifier
                                    .size(46.dp, 48.dp)
                                    .selectable(on, role = Role.RadioButton) {
                                        colour = value
                                        eraser = false
                                    }
                                    .semantics { contentDescription = name },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(32.dp)
                                        .then(if (on) Modifier.border(2.dp, Color(value), CircleShape).padding(5.dp) else Modifier)
                                        .background(Color(value), CircleShape),
                                )
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        SIZES.forEachIndexed { i, (name, _) ->
                            val on = brush == i
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                    .selectable(on, role = Role.RadioButton) { brush = i }
                                    .semantics { contentDescription = name },
                                contentAlignment = Alignment.Center,
                            ) {
                                val dot = (4 + i * 6).dp
                                Box(Modifier.size(dot).background(if (eraser) MaterialTheme.colorScheme.onSurfaceVariant else Color(colour), CircleShape))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Tool(WebIcons.Eraser, t("doodle.eraser"), on = eraser) { eraser = !eraser }
                        Tool(WebIcons.Undo, t("doodle.undo"), enabled = history.canUndo) { history = history.undo() }
                        Tool(WebIcons.Redo, t("doodle.redo"), enabled = history.canRedo) { history = history.redo() }
                        Tool(WebIcons.Trash, t("doodle.clear"), enabled = history.canClear(backdrop != null)) { history = history.clear(backdrop != null) }
                    }
                }
            }
        }
        if (askDiscard) {
            DlDialog(
                title = t("doodle.discardDoodle"),
                text = null,
                confirmLabel = t("doodle.discard"),
                onConfirm = onClose,
                dismissLabel = t("doodle.keepDrawing"),
                onDismiss = { askDiscard = false },
            )
        }
    }
}

@Composable
private fun Tool(icon: ImageVector, label: String, enabled: Boolean = true, on: Boolean = false, onClick: () -> Unit) {
    val tint = if (enabled) MaterialTheme.colorScheme.onSurface else DlTheme.colors.inkDisabled
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, Modifier.size(22.dp), tint = tint)
    }
}
