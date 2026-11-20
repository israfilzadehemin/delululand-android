package com.delulutopia.feature.emails

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.motionOn
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.i18n.formatNumber
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Reaction
import kotlin.math.max
import kotlin.math.roundToInt

/*
 * Bubble actions (doc "Delululand — Bubble Actions" BA1–BA10; web emails/bubbleActions.tsx): the time
 * inside the bubble (BA1), the ↩ disc beside it (BA2), long-press for the layer (BA4, BA5), the pill
 * on its edge (BA7) and the Reactions sheet behind it (BA8).
 */

/** BA4: how long a finger rests before the layer opens, and how far it may drift meanwhile. */
const val LONG_PRESS_MS = 450L
const val LONG_PRESS_SLOP_DP = 10f

/** BA7: emojis shown in the pill before the rest only count. */
const val PILL_EMOJIS = 3

/** BA7: the first [PILL_EMOJIS] emojis in chip order, and the total reactions. */
fun pillParts(chips: List<Reaction>): Pair<List<String>, Int> = chips.take(PILL_EMOJIS).map { it.emoji } to chips.sumOf { it.userIds.size }

/** BA1: the time goes on the last text line when there is room for it, else on a line of its own. */
fun timeFits(lastLineWidth: Float, gap: Float, timeWidth: Float, maxWidth: Float): Boolean = lastLineWidth + gap + timeWidth <= maxWidth

/**
 * BA4/BA5: a finger resting [LONG_PRESS_MS] on the bubble, drifting less than [LONG_PRESS_SLOP_DP],
 * calls [onLong]. A press a control inside took (links, photos, the player, the quote) or a swipe that
 * took over does not. Put it before the swipe's pointerInput so the swipe's consumption is seen.
 */
fun Modifier.longPressToReact(onLong: () -> Unit): Modifier = pointerInput(Unit) {
    val slop = LONG_PRESS_SLOP_DP.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = true)
        val ended = withTimeoutOrNull(LONG_PRESS_MS) {
            var stop = false
            while (!stop) {
                val c = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                stop = c == null || !c.pressed || c.isConsumed || (c.position - down.position).getDistance() > slop
            }
            true
        }
        if (ended == null) {
            onLong()
            // The rest of this press belongs to the layer: nothing underneath clicks.
            do {
                val event = awaitPointerEvent()
                event.changes.forEach { it.consume() }
            } while (event.changes.any { it.pressed })
        }
    }
}

/**
 * BA1: the text with the time after it, on its last line when that line has room ([timeFits]), else
 * on a line of its own, always at the end edge.
 */
@Composable
fun TextWithTime(text: @Composable ((TextLayoutResult) -> Unit) -> Unit, time: @Composable () -> Unit, modifier: Modifier = Modifier) {
    val laid = remember { arrayOfNulls<TextLayoutResult>(1) }
    Layout(content = {
        text { laid[0] = it }
        time()
    }, modifier = modifier) { measurables, constraints ->
        val textP = measurables[0].measure(constraints.copy(minWidth = 0))
        val timeP = measurables[1].measure(Constraints())
        val r = laid[0]
        val last = r?.let { it.getLineRight(it.lineCount - 1) - it.getLineLeft(it.lineCount - 1) } ?: textP.width.toFloat()
        val gap = 8.dp.toPx()
        val maxW = if (constraints.hasBoundedWidth) constraints.maxWidth.toFloat() else Float.MAX_VALUE
        val fits = timeFits(last, gap, timeP.width.toFloat(), maxW)
        val width = if (fits) {
            max(textP.width, (last + gap + timeP.width).roundToInt())
        } else {
            max(textP.width, timeP.width)
        }.coerceIn(constraints.minWidth, if (constraints.hasBoundedWidth) constraints.maxWidth else Int.MAX_VALUE)
        val height = if (fits) textP.height else textP.height + timeP.height
        layout(width, height) {
            textP.placeRelative(0, 0)
            timeP.placeRelative(width - timeP.width, height - timeP.height - if (fits) 2.dp.roundToPx() else 0)
        }
    }
}

/** BA1: the time, small and quiet; on media a white chip. */
@Composable
fun BubbleTime(label: String, modifier: Modifier = Modifier, onMedia: Boolean = false) {
    if (onMedia) {
        Text(
            label,
            modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x94141620))
                .padding(horizontal = 6.dp, vertical = 1.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = "tnum"),
            color = Color.White,
        )
    } else {
        Text(
            label,
            modifier,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 14.sp, fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** BA2: the ↩ disc on the bubble's free side: a 28 dp face in a 44 dp target. */
@Composable
fun ReplyDisc(onReply: () -> Unit, modifier: Modifier = Modifier) {
    val label = t("emails.reply")
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onReply)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DlTheme.colors.letterEnvelope.copy(alpha = 0.55f))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(WebIcons.Reply, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** BA7: one button over the bubble's bottom edge; highlighted when mine is among them; pops in when it first appears. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReactionPill(chips: List<Reaction>, mine: Boolean, onOpen: () -> Unit, onLong: () -> Unit, modifier: Modifier = Modifier) {
    if (chips.isEmpty()) return
    val (shown, total) = pillParts(chips)
    val on = motionOn()
    val pop = remember { Animatable(if (on) 0.6f else 1f) }
    LaunchedEffect(Unit) { if (on) pop.animateTo(1f, tween(260)) }
    val shape = RoundedCornerShape(13.dp)
    val label = t("emails.reactionsPill", "list" to chips.joinToString(" ") { it.emoji }, "n" to formatNumber(total))
    Box(
        modifier
            .heightIn(min = 44.dp)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onLongClick = onLong,
                onClick = onOpen,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .graphicsLayer {
                    scaleX = pop.value
                    scaleY = pop.value
                    alpha = ((pop.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                }
                .height(26.dp)
                .shadow(1.dp, shape)
                .clip(shape)
                .background(if (mine) MaterialTheme.colorScheme.primaryContainer else DlTheme.colors.letterEnvelope)
                .border(1.dp, if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(shown.joinToString(""), fontSize = 14.sp, letterSpacing = 1.sp)
            if (total > 1) {
                Text(
                    formatNumber(total),
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** The layer covers the whole window from its top-left corner. */
private object FullWindow : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset =
        IntOffset.Zero
}

/**
 * BA4: scrim, a lifted copy of the bubble ([bubble]) moved only as far as needed to fit, the quick
 * row (six recents + ⋯) above it and Reply / Copy text below. A pick toggles mine and closes; the
 * scrim or Back closes it. [bounds] is the bubble in window pixels.
 */
@Composable
fun BubbleLayer(
    bounds: Rect,
    mine: Boolean,
    recents: List<String>,
    isMine: (String) -> Boolean,
    canCopy: Boolean,
    onPick: (String) -> Unit,
    onMore: () -> Unit,
    onReply: () -> Unit,
    onCopy: () -> Unit,
    onDismiss: () -> Unit,
    bubble: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    // Line up with the bubble's outer edge: the friend's start edge, my end edge.
    val left = mine == rtl
    val on = motionOn()
    val shown = remember { Animatable(if (on) 0f else 1f) }
    LaunchedEffect(Unit) { if (on) shown.animateTo(1f, tween(200)) }
    val title = t("emails.emailActions")
    Popup(popupPositionProvider = FullWindow, onDismissRequest = onDismiss, properties = PopupProperties(focusable = true, clippingEnabled = false)) {
        BoxWithConstraints(Modifier.fillMaxSize().semantics { paneTitle = title }) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shown.value }
                    .background(Color(0x5C16181F))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
            )
            val winH = with(density) { maxHeight.toPx() }
            val winW = with(density) { maxWidth.toPx() }
            val above = with(density) { (52 + 10).dp.toPx() }
            val below = with(density) { (if (canCopy) 2 * 48 + 14 else 48 + 14).dp.toPx() }
            val edge = with(density) { 12.dp.toPx() }
            val maxH = max(with(density) { 80.dp.toPx() }, winH - above - below - 2 * edge)
            val h = minOf(bounds.height, maxH)
            val top = minOf(max(bounds.top, edge + above), winH - edge - below - h) - above
            val inset = max(with(density) { 8.dp.toPx() }, if (left) bounds.left else winW - bounds.right)
            Column(
                Modifier
                    .align(if (left) AbsoluteAlignment.TopLeft else AbsoluteAlignment.TopRight)
                    .offset { IntOffset(if (left) inset.roundToInt() else -inset.roundToInt(), top.roundToInt()) }
                    .graphicsLayer {
                        alpha = shown.value
                        val s = 0.96f + 0.04f * shown.value
                        scaleX = s
                        scaleY = s
                    },
                horizontalAlignment = if (left) AbsoluteAlignment.Left else AbsoluteAlignment.Right,
            ) {
                QuickRow(recents, isMine, onPick, onMore)
                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .width(with(density) { bounds.width.toDp() })
                        .heightIn(max = with(density) { maxH.toDp() })
                        .graphicsLayer {
                            val s = 1f + 0.02f * shown.value
                            scaleX = s
                            scaleY = s
                        }
                        .shadow(14.dp, RoundedCornerShape(16.dp), clip = false),
                ) { bubble() }
                Spacer(Modifier.height(14.dp))
                Menu(canCopy, onReply, onCopy)
            }
        }
    }
}

@Composable
private fun QuickRow(recents: List<String>, isMine: (String) -> Boolean, onPick: (String) -> Unit, onMore: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    val label = t("emails.react")
    Row(
        Modifier
            .widthIn(max = 360.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(DlTheme.colors.letterEnvelope)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
            .horizontalScroll(rememberScrollState())
            .padding(4.dp)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        recents.forEach { e ->
            val on = isMine(e)
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (on) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .then(if (on) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape) else Modifier)
                    .clickable(role = Role.Button) { onPick(e) }
                    .semantics {
                        contentDescription = e
                        selected = on
                    },
                contentAlignment = Alignment.Center,
            ) { Text(e, fontSize = 26.sp) }
        }
        val all = t("reactions.allEmojis")
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .clickable(role = Role.Button, onClick = onMore)
                .semantics { contentDescription = all },
            contentAlignment = Alignment.Center,
        ) { Text("⋯", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun Menu(canCopy: Boolean, onReply: () -> Unit, onCopy: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .width(210.dp)
            .shadow(8.dp, shape)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape),
    ) {
        MenuItem(t("emails.reply"), { Icon(WebIcons.Reply, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }, onReply)
        if (canCopy) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            MenuItem(t("emails.copyText"), { Icon(DlIcons.Copy, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant) }, onCopy)
        }
    }
}

@Composable
private fun MenuItem(label: String, icon: @Composable () -> Unit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon()
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * BA8: who reacted with each emoji. Mine can be removed, the friend's-only ones added; "Another
 * reaction" opens the full picker. Closes itself when the last one goes.
 */
@Composable
fun ReactionsSheet(
    chips: List<Reaction>,
    myId: String?,
    nameOf: (String) -> String,
    onToggle: (String) -> Unit,
    onAnother: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(chips.isEmpty()) { if (chips.isEmpty()) onDismiss() }
    val total = chips.sumOf { it.userIds.size }
    DlBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
            Row(Modifier.fillMaxWidth().padding(bottom = 4.dp), verticalAlignment = Alignment.Bottom) {
                Text(t("reactions.reactions"), Modifier.weight(1f), style = DlTheme.type.noteTitle.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold))
                Text(formatNumber(total), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            chips.forEach { c ->
                val mine = myId != null && myId in c.userIds
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) { Text(c.emoji, fontSize = 22.sp) }
                    Text(
                        c.userIds.map(nameOf).filter { it.isNotEmpty() }.joinToString(", "),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                    )
                    val label = if (mine) t("emails.removeReactionLabel", "emoji" to c.emoji) else t("emails.addReactionLabel", "emoji" to c.emoji)
                    val pill = RoundedCornerShape(18.dp)
                    Box(
                        Modifier
                            .heightIn(min = 44.dp)
                            .clickable(role = Role.Button) { onToggle(c.emoji) }
                            .semantics { contentDescription = label },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (mine) t("emails.removeReaction") else "+ " + t("emails.addReaction"),
                            Modifier
                                .clip(pill)
                                .background(if (mine) Color.Transparent else MaterialTheme.colorScheme.primary)
                                .border(1.dp, if (mine) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, pill)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                            color = if (mine) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                Modifier.heightIn(min = 48.dp).clickable(role = Role.Button, onClick = onAnother).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(WebIcons.Emoji, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(
                    t("emails.anotherReaction"),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/** BA5: Copy text, with "Copied" where the system does not say so itself (before Android 13). */
fun copyEmailText(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("email", text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) Toast.makeText(context, t("emails.copied"), Toast.LENGTH_SHORT).show()
}
