package com.delulutopia.core.data

import com.delulutopia.core.network.LiveUpdates
import com.delulutopia.core.network.Signal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * When a screen re-reads its data (web src/live/liveUpdates.ts):
 * - a live signal it names, or the stream (re)opening;
 * - while the stream is not live, every [pollMs] (15 s lists and Home, 5 s threads), as the web polls;
 * - at the next deadline its data holds (`arrivesAt`, `windowEndsAt`, `renewsAt`), + 500 ms, since
 *   a window closing or an arrival happens with no write to announce it.
 */
class Refresher(
    private val scope: CoroutineScope,
    private val live: LiveUpdates,
    private val signals: Set<String>,
    private val pollMs: Long,
    private val deadlines: () -> List<String?>,
    private val reload: suspend () -> Unit,
) {
    private var deadlineJob: Job? = null

    fun start(): Refresher {
        scope.launch {
            live.signals.filter { it.name == Signal.READY || it.name in signals }.collect {
                reload()
                armDeadline()
            }
        }
        // Polls only while the app is in front without a stream: a phone in a pocket must not
        // keep calling the server every 15 s (the web pauses with a hidden tab the same way).
        scope.launch {
            combine(live.live, live.active) { isLive, active -> active && !isLive }.distinctUntilChanged().collectLatest { poll ->
                while (poll && isActive) {
                    delay(pollMs)
                    reload()
                    armDeadline()
                }
            }
        }
        return this
    }

    /** Call after each successful load. */
    fun armDeadline() {
        deadlineJob?.cancel()
        val now = Time.now()
        val next = deadlines().mapNotNull { Time.ms(it) }.filter { it > now }.minOrNull() ?: return
        val wait = next - now + 500
        if (wait > 6 * 60 * 60_000L) return
        deadlineJob = scope.launch {
            delay(wait)
            reload()
            armDeadline()
        }
    }

    companion object {
        const val POLL_MS = 15_000L
        const val THREAD_POLL_MS = 5_000L
    }
}
