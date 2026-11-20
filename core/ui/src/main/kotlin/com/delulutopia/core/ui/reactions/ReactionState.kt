package com.delulutopia.core.ui.reactions

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Reaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One item's reactions (doc "Delululand — Reactions" R1), for places that draw them their own way
 * (the email bubble's pill and long-press layer, doc "Delululand — Bubble Actions"): the chips, which
 * are mine, and a toggle that shows at once, is confirmed by the server's chips and rolls back with
 * an error when refused. Same rules as [Reactions].
 */
@Stable
class ReactionState internal constructor(initial: List<Reaction>) {
    var chips by mutableStateOf(initial)
        internal set
    var error by mutableStateOf(false)
        internal set
    internal var pending = 0
    internal var myId: String? = null
    internal var run: (String) -> Unit = {}

    /** I am among this emoji's users. */
    fun mine(emoji: String): Boolean = myId?.let { me -> chips.any { it.emoji == emoji && me in it.userIds } } ?: false

    /** I am among anyone's users: the pill is highlighted (BA7). */
    val anyMine: Boolean get() = myId?.let { me -> chips.any { me in it.userIds } } ?: false

    /** Adds mine, or takes it away when it is already there. */
    fun toggle(emoji: String) = run(emoji)
}

@Composable
fun rememberReactionState(target: String, id: String, reactions: List<Reaction>, me: Citizen?, onChanged: () -> Unit = {}): ReactionState {
    val api = LocalReactionsApi.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val changed by rememberUpdatedState(onChanged)
    val state = remember(target, id) { ReactionState(reactions) }
    // Follow the server's chips (polls) unless a change of mine is in flight: its answer is newer.
    LaunchedEffect(reactions) { if (state.pending == 0) state.chips = reactions }
    state.myId = me?.id
    state.run = { emoji -> toggleOn(state, emoji, context, scope, api, target, id) { changed() } }
    return state
}

private fun toggleOn(
    state: ReactionState,
    emoji: String,
    context: Context,
    scope: CoroutineScope,
    api: ReactionsApi,
    target: String,
    id: String,
    changed: () -> Unit,
) {
    val me = state.myId ?: return
    val removing = state.mine(emoji)
    state.error = false
    state.chips = withMine(state.chips, emoji, me, !removing)
    if (!removing) pushRecent(context, emoji)
    state.pending++
    scope.launch {
        try {
            state.chips = api.toggle(target, id, emoji, !removing)
            changed()
        } catch (_: Exception) {
            state.chips = withMine(state.chips, emoji, me, removing)
            state.error = true
        } finally {
            state.pending--
        }
    }
}

/** The quick row's six emojis on this phone, most recent first (R2). */
fun reactionRecents(context: Context): List<String> = readRecents(context)
