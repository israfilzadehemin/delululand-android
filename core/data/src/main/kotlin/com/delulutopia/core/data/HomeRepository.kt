package com.delulutopia.core.data

import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.Mood
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * S2's payload, shared by every screen that needs the friend, the moods, the badges or `draftEmpty`
 * (web `useHome`). Refetched on the signals that touch Home, polled every 15 s while the live stream
 * is down, and at the next arrival / window end / credit renewal it holds.
 */
@Singleton
class HomeRepository @Inject constructor(private val backend: Backend) {
    data class State(
        val home: Home? = null,
        val offline: Boolean = false,
        val moods: List<Mood> = Moods.DEFAULTS,
        val moodLimit: Int = Moods.LIMIT,
        /** The universe's own list arrived (custom moods can be added). */
        val moodsLoaded: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var started: Job? = null
    private val mutex = Mutex()
    private var refresher: Refresher? = null

    /** Called once the session is known (shell shown). */
    fun start() {
        if (started != null) return
        started = scope.launch {
            refresher = backend.refresher(
                scope,
                setOf("letter", "discussion", "comment", "email", "reaction", "board", "mood", "moods", "profile", "universe", "language"),
                deadlines = { deadlines() },
            ) { load() }
            launch { backend.live.signals.collect { if (it.name == "moods") loadMoods() } }
            // A switch reaches the server's own words too: default moods, stamps, errors (L9).
            launch {
                I18n.changes.drop(1).collect {
                    load()
                    loadMoods()
                }
            }
            load()
            loadMoods()
        }
    }

    fun stop() {
        scope.coroutineContext.cancelChildren()
        started = null
        _state.value = State()
    }

    suspend fun load() = mutex.withLock {
        try {
            val home = backend.run { home() }
            // L8: the account's language wins on every device.
            Lang.parse(home.me.language)?.let { if (it != I18n.lang) I18n.set(it) }
            _state.update { it.copy(home = home, offline = false) }
            refresher?.armDeadline()
        } catch (e: ApiException) {
            if (e.isOffline) _state.update { it.copy(offline = true) }
        }
    }

    suspend fun loadMoods() {
        runCatching { backend.run { moods() } }.onSuccess { c -> _state.update { it.copy(moods = c.moods, moodLimit = c.limit, moodsLoaded = true) } }
    }

    /** Local patch after a write that answers with the new value (mood, board). */
    fun patch(block: (Home) -> Home) = _state.update { s -> s.copy(home = s.home?.let(block)) }

    fun refreshSoon() {
        scope.launch { load() }
    }

    private fun deadlines(): List<String?> {
        val h = _state.value.home ?: return emptyList()
        return (h.from + h.to).flatMap { listOf(it.windowEndsAt, it.arrivesAt) } +
            h.newDiscussions.map { it.arrivesAt } + listOf(h.emailCredits?.renewsAt)
    }
}
