package com.delulutopia.feature.discussions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.DRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The state shown now: the server's, flipped locally at 0:00 until the refetch lands. */
internal fun dStateAt(row: DRow, nowMs: Long): String = LetterState.discussion(nowMs, row.windowEndsAt, row.arrivesAt)

/** Q1: the friend's row before arrival carries no title. */
internal fun hiddenRow(row: DRow, nowMs: Long): Boolean = !row.mine && dStateAt(row, nowMs) != LetterState.ARRIVED

internal fun commentWords(count: Int?): String = if (count == null || count == 0) "" else tn("discussions.comments", count)

/**
 * S10 Discussions and the Archived list (FS-D1–D8, Discussion Controls A4, A5; web
 * discussions/DiscussionsScreen.tsx): newest activity first, 20 per page by keyset; page 1 re-read
 * on discussion signals and polled while the stream is down.
 */
@HiltViewModel(assistedFactory = DiscussionsViewModel.Factory::class)
class DiscussionsViewModel @AssistedInject constructor(
    @Assisted val archived: Boolean,
    private val backend: Backend,
    home: HomeRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(archived: Boolean): DiscussionsViewModel
    }
    val home: StateFlow<HomeRepository.State> = home.state

    var head by mutableStateOf<List<DRow>?>(null)
        private set
    private var older by mutableStateOf<List<DRow>>(emptyList())
    var nextCursor by mutableStateOf<String?>(null)
        private set
    var archivedCount by mutableStateOf(0)
        private set
    private var loadingMore = false

    /** Page 1 is the truth for what it holds; an older copy is kept only below its last row. */
    val rows: List<DRow>
        get() {
            val fresh = head ?: return older
            val seen = fresh.map { it.id }.toSet()
            val floor = if (fresh.size >= PAGE) sortKey(fresh.last()) else null
            val kept = if (floor != null) older.filter { it.id !in seen && below(sortKey(it), floor) } else emptyList()
            return fresh + kept
        }

    init {
        viewModelScope.launch {
            backend.refresher(viewModelScope, setOf("discussion", "comment", "reaction"), deadlines = {
                head.orEmpty().flatMap { listOf(it.windowEndsAt, it.arrivesAt) }
            }) { loadHead() }
            loadHead()
        }
    }

    suspend fun loadHead() {
        try {
            val page = backend.run { discussions(limit = PAGE, archived = archived.takeIf { it }) }
            head = page.items
            archivedCount = page.archivedCount
            if (older.isEmpty()) nextCursor = page.nextCursor
        } catch (_: ApiException) {
        }
    }

    fun refresh() {
        viewModelScope.launch { loadHead() }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore) return
        loadingMore = true
        viewModelScope.launch {
            try {
                val page = backend.run { discussions(cursor = cursor, limit = PAGE, archived = archived.takeIf { it }) }
                older = older + page.items
                nextCursor = page.nextCursor
            } catch (_: ApiException) {
            } finally {
                loadingMore = false
            }
        }
    }

    /** The keyset order (O2, A5): the later of arrival and latest update, or the archive time. */
    private fun sortKey(r: DRow): Pair<Long, Long> = if (archived) {
        (Time.ms(r.archivedAt) ?: 0) to r.id
    } else {
        maxOf(Time.ms(r.arrivesAt) ?: 0, Time.ms(r.activityAt) ?: 0) to r.id
    }

    private fun below(a: Pair<Long, Long>, b: Pair<Long, Long>) = a.first < b.first || (a.first == b.first && a.second < b.second)

    private companion object {
        const val PAGE = 20
    }
}
