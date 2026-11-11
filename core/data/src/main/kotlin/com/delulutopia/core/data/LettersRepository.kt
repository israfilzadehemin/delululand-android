package com.delulutopia.core.data

import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Row
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * S6's rows, newest first (web letters/useLetterRows.ts): page 1 re-read on `letter` signals and
 * polled while the stream is down; older pages by keyset, merged so a row never shows twice. Shared
 * by the list and by S7's swipe between letters, so both walk the same order.
 */
@Singleton
class LettersRepository @Inject constructor(private val backend: Backend) {
    data class State(
        val head: List<Row>? = null,
        val older: List<Row> = emptyList(),
        val nextCursor: String? = null,
        val loadingMore: Boolean = false,
        val failed: Boolean = false,
    ) {
        val loaded: Boolean get() = head != null
        val hasMore: Boolean get() = nextCursor != null

        /** Page 1 as it is now, then older pages; a letter cancelled since must not linger from an older copy. */
        val rows: List<Row>
            get() {
                val fresh = head.orEmpty()
                val seen = fresh.map { it.id }.toSet()
                val oldest = fresh.lastOrNull()
                val kept = older.filter { it.id !in seen }.filter { r ->
                    oldest == null || r.sentAt < oldest.sentAt ||
                        (r.sentAt == oldest.sentAt && r.id < oldest.id)
                }
                return fresh + kept
            }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private var started: Job? = null
    private var users = 0

    /** A screen that shows letters is on screen; the first one starts the refresh. */
    fun attach() {
        users++
        if (started != null) {
            scope.launch { loadHead() }
            return
        }
        started = scope.launch {
            backend.refresher(scope, setOf("letter")) { loadHead() }
            loadHead()
        }
    }

    fun detach() {
        users = (users - 1).coerceAtLeast(0)
        if (users == 0) {
            scope.coroutineContext.cancelChildren()
            started = null
        }
    }

    suspend fun loadHead() = mutex.withLock {
        try {
            val page = backend.run { letters(limit = PAGE) }
            _state.update { s -> s.copy(head = page.items, nextCursor = if (s.older.isEmpty()) page.nextCursor else s.nextCursor, failed = false) }
        } catch (e: ApiException) {
            _state.update { it.copy(failed = true) }
        }
    }

    fun loadMore() {
        val s = _state.value
        val cursor = s.nextCursor ?: return
        if (s.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        scope.launch {
            try {
                val page = backend.run { letters(cursor = cursor, limit = PAGE) }
                _state.update { it.copy(older = it.older + page.items, nextCursor = page.nextCursor, loadingMore = false) }
            } catch (_: ApiException) {
                _state.update { it.copy(loadingMore = false) }
            }
        }
    }

    fun refreshSoon() {
        scope.launch { loadHead() }
    }

    /** Sign-out: forget everything. */
    fun clear() {
        scope.coroutineContext.cancelChildren()
        started = null
        users = 0
        _state.value = State()
    }

    private companion object {
        const val PAGE = 20
    }
}
