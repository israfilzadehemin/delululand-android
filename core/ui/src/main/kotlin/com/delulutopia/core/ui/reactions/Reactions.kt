package com.delulutopia.core.ui.reactions

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import com.delulutopia.core.data.Time
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Reaction
import com.delulutopia.core.network.model.ReactionNotice
import com.delulutopia.core.ui.EmojiPickerSheet
import java.text.BreakIterator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Where reactions go: `discussions`, `comments` or `emails`. Adds or removes mine; answers the new chips. */
fun interface ReactionsApi {
    suspend fun toggle(target: String, id: Long, emoji: String, add: Boolean): List<Reaction>
}

val LocalReactionsApi = staticCompositionLocalOf<ReactionsApi> { ReactionsApi { _, _, _, _ -> emptyList() } }

/** The chip list with my emoji added or taken away; a chip with nobody left disappears. */
fun withMine(list: List<Reaction>, emoji: String, me: Int, add: Boolean): List<Reaction> {
    val i = list.indexOfFirst { it.emoji == emoji }
    if (add) {
        if (i < 0) return list + Reaction(emoji, listOf(me))
        if (me in list[i].userIds) return list
        return list.mapIndexed { j, c -> if (j == i) c.copy(userIds = c.userIds + me) else c }
    }
    if (i < 0) return list
    val users = list[i].userIds - me
    return if (users.isEmpty()) list.filterIndexed { j, _ -> j != i } else list.mapIndexed { j, c -> if (j == i) c.copy(userIds = users) else c }
}

/** The quick row's six emojis: most recent first, kept on this phone (R2). */
private val DEFAULT_RECENTS = listOf("❤️", "😂", "🥹", "😮", "😢", "🔥")
private const val RECENTS = "reactionRecents"

private fun readRecents(context: Context): List<String> {
    val saved = context.getSharedPreferences("device", Context.MODE_PRIVATE).getString(RECENTS, null)?.split('\u0001').orEmpty()
    return (saved + DEFAULT_RECENTS).filter { it.isNotEmpty() }.distinct().take(6)
}

private fun pushRecent(context: Context, emoji: String) {
    val next = (listOf(emoji) + readRecents(context).filter { it != emoji }).take(6)
    context.getSharedPreferences("device", Context.MODE_PRIVATE).edit().putString(RECENTS, next.joinToString("\u0001")).apply()
}

/**
 * The chip row under a discussion, a comment or an email (doc "Delululand — Reactions" R1–R3; web
 * reactions/Reactions.tsx): one chip per emoji, highlighted when I am among its users, a count when
 * both used it. Tapping my chip removes mine; tapping the friend's adds the same. ＋ opens six recent
 * emojis and ⋯ the full picker. A long press shows who reacted. Changes show at once.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun Reactions(
    target: String,
    id: Long,
    reactions: List<Reaction>,
    me: Citizen?,
    friend: Citizen?,
    modifier: Modifier = Modifier,
    readOnly: Boolean = false,
    chipColour: Color = DlTheme.colors.letterSlip,
    onChanged: () -> Unit = {},
) {
    val api = LocalReactionsApi.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chips by remember { mutableStateOf(reactions) }
    var pending by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf(false) }
    var quick by remember { mutableStateOf(false) }
    var all by remember { mutableStateOf(false) }
    var tip by remember { mutableStateOf<String?>(null) }
    // Follow the server's chips (polls) unless a change of mine is in flight: its answer is newer.
    LaunchedEffect(reactions) { if (pending == 0) chips = reactions }
    LaunchedEffect(tip) {
        if (tip != null) {
            delay(2_000)
            tip = null
        }
    }
    val myId = me?.id
    val mineOn = { emoji: String -> myId != null && chips.any { it.emoji == emoji && myId in it.userIds } }
    val names = { users: List<Int> ->
        users.mapNotNull { u ->
            if (u ==
                me?.id
            ) {
                me.nickname
            } else if (u == friend?.id) {
                friend.nickname
            } else {
                null
            }
        }.joinToString(", ")
    }
    val toggle = { emoji: String ->
        if (myId != null && !readOnly) {
            val removing = mineOn(emoji)
            error = false
            chips = withMine(chips, emoji, myId, !removing)
            if (!removing) pushRecent(context, emoji)
            pending++
            scope.launch {
                try {
                    chips = api.toggle(target, id, emoji, !removing)
                    onChanged()
                } catch (_: Exception) {
                    chips = withMine(chips, emoji, myId, removing)
                    error = true
                } finally {
                    pending--
                }
            }
        }
    }
    if (readOnly && chips.isEmpty()) return
    Column(modifier) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { c ->
                val on = myId != null && myId in c.userIds
                val who = names(c.userIds)
                Row(
                    Modifier
                        .heightIn(min = 32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (on) MaterialTheme.colorScheme.secondaryContainer else chipColour)
                        .border(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                        .then(
                            if (readOnly) {
                                Modifier
                            } else {
                                Modifier.combinedClickable(onLongClick = { tip = "${c.emoji} $who" }) { toggle(c.emoji) }
                            },
                        )
                        .semantics {
                            contentDescription = "${c.emoji} $who${if (readOnly) {
                                ""
                            } else if (on) {
                                ", tap to remove yours"
                            } else {
                                ", tap to add yours"
                            }}"
                        }
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(c.emoji, fontSize = 16.sp)
                    if (c.userIds.size > 1) Text("${c.userIds.size}", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
            if (!readOnly) {
                Box {
                    Box(
                        Modifier
                            .size(36.dp, 32.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .combinedClickable { quick = !quick }
                            .semantics { contentDescription = "Add a reaction" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(WebIcons.Emoji, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (quick) {
                        Popup(alignment = Alignment.BottomStart, offset = IntOffset(0, -120), onDismissRequest = { quick = false }) {
                            Row(
                                Modifier
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                readRecents(context).forEach { e ->
                                    Box(
                                        Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(22.dp))
                                            .background(if (mineOn(e)) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                            .combinedClickable {
                                                quick = false
                                                toggle(e)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) { Text(e, fontSize = 22.sp) }
                                }
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .combinedClickable {
                                            quick = false
                                            all = true
                                        }
                                        .semantics { contentDescription = "All emojis" },
                                    contentAlignment = Alignment.Center,
                                ) { Text("⋯", fontSize = 20.sp) }
                            }
                        }
                    }
                }
            }
        }
        tip?.let { Text(it, Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (error) {
            Text(
                "Not saved. Try again.",
                Modifier.padding(top = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
    if (all) {
        EmojiPickerSheet(
            "All emojis",
            onPick = {
                all = false
                if (!mineOn(it)) toggle(it)
            },
            onDismiss = { all = false },
        )
    }
}

/** The folded email line: the chips' emojis only. */
@Composable
fun ReactionSummary(reactions: List<Reaction>, modifier: Modifier = Modifier) {
    if (reactions.isEmpty()) return
    val shown = reactions.take(4).joinToString("") { it.emoji } + if (reactions.size > 4) "…" else ""
    Text(shown, modifier.semantics { contentDescription = "Reactions: ${reactions.joinToString(" ") { it.emoji }}" }, fontSize = 14.sp)
}

/** C3: how much of the reacted item's text a row quotes, in visible characters. */
const val NOTICE_SNIPPET = 40

/**
 * The row line for unseen reactions (doc "Delululand — Replies & Reaction Notices" C3–C5):
 * "Friend reacted to 'first 40 characters…' with ❤️", "+N more" when there are others.
 */
fun noticeLine(n: ReactionNotice, count: Int, reactorName: String): String {
    val what = n.text?.let { "'${clip(it, NOTICE_SNIPPET)}'" } ?: "${if (n.onMine) "your" else "their"} ${itemWord(n)}"
    val more = if (count > 1) " · +${count - 1} more" else ""
    return "$reactorName reacted to $what with ${n.emoji}$more"
}

private fun itemWord(n: ReactionNotice): String = when {
    n.target != "email" -> n.target
    n.media == "video" -> "video"
    n.media == "photo" -> "photo"
    n.media == "audio" -> "voice note"
    else -> "email"
}

private fun clip(text: String, max: Int): String {
    val t = text.replace(Regex("\\s+"), " ").trim()
    val chars = BreakIterator.getCharacterInstance()
    chars.setText(t)
    var count = 0
    var end = 0
    while (true) {
        val next = chars.next()
        if (next == BreakIterator.DONE) return t
        count++
        end = next
        if (count == max) break
    }
    return if (end >= t.length) t else t.substring(0, end).trimEnd() + "…"
}

/** C4: the reaction line replaces the row's usual line only while it is the newest activity. */
fun reactionWins(n: ReactionNotice?, count: Int, otherActivityAt: String?): Boolean {
    if (n == null || count == 0) return false
    val other = otherActivityAt?.let { Time.ms(it) } ?: return true
    return (Time.ms(n.at) ?: 0) > other
}
