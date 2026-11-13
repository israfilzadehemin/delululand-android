package com.delulutopia.feature.discussions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.Refresher
import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.DComment
import com.delulutopia.core.network.model.DComments
import com.delulutopia.core.network.model.DFull
import com.delulutopia.core.network.model.TextBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** Display order: oldest first by (createdAt, id), as the server pages them (FS-D12). */
internal val byTime = compareBy<DComment>({ it.createdAt }, { it.id })

/**
 * Folds one server snapshot into the comments held (Comment Edit & Delete E8; web
 * discussions/Comments.tsx `reconcile`): a snapshot is authoritative from its oldest item to the
 * newest (from the very first when `olderCursor` is null); own posts newer than it stay unless
 * deleted here.
 */
internal fun reconcile(held: List<DComment>, snap: DComments, posted: List<DComment>, gone: Set<Long>): List<DComment> {
    val items = snap.items.filter { it.id !in gone }
    val oldest = snap.items.minWithOrNull(byTime)
    val covers = { c: DComment -> snap.olderCursor == null || (oldest != null && byTime.compare(c, oldest) >= 0) }
    val maxId = snap.items.maxOfOrNull { it.id } ?: 0
    val out = LinkedHashMap<Long, DComment>()
    held.forEach { if (!covers(it) && it.id !in gone) out[it.id] = it }
    items.forEach { out[it.id] = it }
    posted.forEach { if (it.id > maxId && it.id !in gone && it.id !in out) out[it.id] = it }
    return out.values.sortedWith(byTime)
}

/**
 * S11 (FS-D9–D17; web discussions/DiscussionView.tsx, Comments.tsx): the thread re-read every 5 s
 * while the live stream is down and on its signals, the comments (newest five with the thread,
 * older pages by cursor and then re-read too), seen marks, posting, editing and deleting comments,
 * archive, unarchive, delete and take back.
 */
@HiltViewModel(assistedFactory = DiscussionViewModel.Factory::class)
class DiscussionViewModel @AssistedInject constructor(
    @Assisted val id: Long,
    private val backend: Backend,
    private val homeRepo: HomeRepository,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(id: Long): DiscussionViewModel
    }

    val home: StateFlow<HomeRepository.State> = homeRepo.state

    sealed interface Ui {
        data object Loading : Ui

        data class Shown(val d: DFull) : Ui

        data class NotArrived(val arrivesAt: String) : Ui

        data object Gone : Ui

        data object Unreachable : Ui
    }

    var ui by mutableStateOf<Ui>(Ui.Loading)
        private set
    var comments by mutableStateOf<List<DComment>>(emptyList())
        private set
    private var posted = listOf<DComment>()
    private val gone = mutableSetOf<Long>()
    private var olderCursor: String? = null
    private var olderLoaded = false
    var loadingOlder by mutableStateOf(false)
        private set
    var olderFailed by mutableStateOf(false)
        private set
    private var seenNewest: Long? = null
    private var rangeJob: Job? = null

    /** Where "See older comments" continues from, or null when everything is shown. */
    val cursor: String? get() = if (olderLoaded) olderCursor else (ui as? Ui.Shown)?.d?.comments?.olderCursor

    init {
        viewModelScope.launch {
            backend.refresher(viewModelScope, setOf("discussion", "comment", "reaction"), Refresher.THREAD_POLL_MS) { load() }
            load()
            // The first open cleared NEW on the server: the list and the tab bar follow.
            homeRepo.refreshSoon()
        }
    }

    suspend fun load() {
        try {
            val d = backend.run { discussion(id) }
            ui = Ui.Shown(d)
            comments = reconcile(comments, d.comments, posted, gone)
            markSeen(d)
            if (olderLoaded) reloadRange()
        } catch (e: ApiException) {
            when {
                e.code == "NOT_ARRIVED" -> {
                    val at = (e.extra["arrivesAt"] as? JsonPrimitive)?.content.orEmpty()
                    ui = Ui.NotArrived(at)
                    val wait = (Time.ms(at) ?: 0) + 300 - Time.now()
                    if (wait < 5 * 60_000) {
                        viewModelScope.launch {
                            delay(wait.coerceAtLeast(0))
                            load()
                        }
                    }
                }
                e.status == 404 -> ui = Ui.Gone
                ui is Ui.Shown -> Unit
                else -> ui = Ui.Unreachable
            }
        }
    }

    /** D6: a newer friend comment shown here moves the seen mark, so the list chip and the tab dot follow. */
    private fun markSeen(d: DFull) {
        val me = homeRepo.state.value.home?.me?.id
        val newestOther = d.comments.items.lastOrNull { it.authorId != me }?.id ?: return
        if (seenNewest == newestOther) return
        seenNewest = newestOther
        viewModelScope.launch {
            runCatching { backend.run { discussionSeen(id) } }
            homeRepo.refreshSoon()
        }
    }

    /** E8: with older pages loaded, the span from the oldest loaded to the newest is re-read. */
    private fun reloadRange() {
        rangeJob?.cancel()
        rangeJob = viewModelScope.launch {
            val snap = runCatching { backend.run { comments(id, from = olderCursor ?: "start") } }.getOrNull() ?: return@launch
            comments = reconcile(comments, snap, posted, gone)
            if (olderCursor != snap.olderCursor) olderCursor = snap.olderCursor
        }
    }

    fun seeOlder() {
        val c = cursor ?: return
        if (loadingOlder) return
        loadingOlder = true
        olderFailed = false
        viewModelScope.launch {
            try {
                val page = backend.run { comments(id, cursor = c, limit = 20) }
                comments = reconcile(comments, DComments(page.items + comments, page.olderCursor), emptyList(), gone)
                olderCursor = page.olderCursor
                olderLoaded = true
            } catch (_: ApiException) {
                olderFailed = true
            } finally {
                loadingOlder = false
            }
        }
    }

    /** FS-D14: null when posted; the error line otherwise (the text stays). */
    suspend fun post(text: String): String? = try {
        val c = backend.run { comment(id, TextBody(text.trim())) }
        posted = posted + c
        comments = (comments + c).distinctBy { it.id }.sortedWith(byTime)
        load()
        null
    } catch (_: ApiException) {
        t("common.notPosted")
    }

    /** E2, E3: the edited comment, or an error line. */
    suspend fun edit(c: DComment, text: String): String? {
        val trimmed = text.trim()
        if (trimmed == c.text) return null
        return try {
            val updated = backend.run { editComment(id, c.id, TextBody(trimmed)) }
            comments = comments.map { if (it.id == c.id) updated else it }
            null
        } catch (e: ApiException) {
            if (e.status == 404) {
                drop(c.id)
                null
            } else {
                t("reactions.notSavedTryAgain")
            }
        }
    }

    /** E6, E7: null when gone, else the notice. */
    suspend fun delete(c: DComment): String? = try {
        backend.run { deleteComment(id, c.id) }
        drop(c.id)
        load()
        homeRepo.refreshSoon()
        null
    } catch (e: ApiException) {
        if (e.status == 404) {
            drop(c.id)
            null
        } else {
            t("common.notDeleted")
        }
    }

    private fun drop(commentId: Long) {
        gone += commentId
        comments = comments.filterNot { it.id == commentId }
    }

    fun reacted() {
        viewModelScope.launch { load() }
    }

    // Discussion Controls

    suspend fun archive(): String = try {
        ui = Ui.Shown(backend.run { archive(id) })
        homeRepo.refreshSoon()
        t("discussions.archivedForBoth")
    } catch (e: ApiException) {
        if (e.status in 400..499) e.message else t("discussions.notArchived")
    }

    suspend fun unarchive(): String = try {
        ui = Ui.Shown(backend.run { unarchive(id) })
        homeRepo.refreshSoon()
        t("discussions.backInDiscussions")
    } catch (e: ApiException) {
        if (e.status in 400..499) e.message else t("discussions.notUnarchived")
    }

    /** Delete (X1–X3) and take back (D2) are the same call; null when done or already gone. */
    suspend fun remove(takeBack: Boolean): String? = try {
        backend.run { deleteDiscussion(id) }
        homeRepo.refreshSoon()
        null
    } catch (e: ApiException) {
        when {
            e.status == 404 -> null
            takeBack && e.code == "WINDOW_CLOSED" -> t("common.tooLate")
            takeBack -> t("discussions.notTakenBack")
            else -> t("common.notDeleted")
        }.also { if (it != null) load() }
    }
}
