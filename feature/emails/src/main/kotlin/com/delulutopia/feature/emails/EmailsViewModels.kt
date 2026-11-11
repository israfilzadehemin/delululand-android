package com.delulutopia.feature.emails

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.Refresher
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Email
import com.delulutopia.core.network.model.ThreadFull
import com.delulutopia.core.network.model.ThreadRow
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** S13 Emails (FS-E1–E5; web emails/EmailsScreen.tsx): threads, newest activity first, and whether a new email is waiting. */
@HiltViewModel
class EmailsViewModel @Inject constructor(private val backend: Backend, home: HomeRepository) : ViewModel() {
    val home: StateFlow<HomeRepository.State> = home.state
    var head by mutableStateOf<List<ThreadRow>?>(null)
        private set
    private var older by mutableStateOf<List<ThreadRow>>(emptyList())
    var nextCursor by mutableStateOf<String?>(null)
        private set
    var draftEmpty by mutableStateOf(true)
        private set
    private var loadingMore = false

    val rows: List<ThreadRow>
        get() {
            val fresh = head.orEmpty()
            val seen = fresh.map { it.id }.toSet()
            val oldest = fresh.lastOrNull()
            return fresh + older.filter { it.id !in seen }.filter { r -> oldest == null || r.lastEmailAt < oldest.lastEmailAt || (r.lastEmailAt == oldest.lastEmailAt && r.id < oldest.id) }
        }

    init {
        viewModelScope.launch {
            backend.refresher(viewModelScope, setOf("email", "reaction")) { loadHead() }
            loadHead()
        }
    }

    suspend fun loadHead() {
        runCatching { backend.run { threads(limit = 20) } }.onSuccess { page ->
            head = page.items
            if (older.isEmpty()) nextCursor = page.nextCursor
        }
        runCatching { backend.run { emailDraft("new") } }.onSuccess { d ->
            draftEmpty = d.subject.isBlank() && d.text.isBlank() && d.attachments.isEmpty()
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
                val page = backend.run { threads(cursor = cursor, limit = 20) }
                older = older + page.items
                nextCursor = page.nextCursor
            } catch (_: ApiException) {
            } finally {
                loadingMore = false
            }
        }
    }
}

/**
 * S14 (FS-E6–E10; web emails/ThreadView.tsx): the subject, the reply composer on top, the emails
 * newest first (the newest unfolded), "See older" at the bottom. Re-read every 5 s while the stream
 * is down. Opening it, and the friend's new emails or reactions showing, mark them read (Q24, C7).
 */
@HiltViewModel(assistedFactory = ThreadViewModel.Factory::class)
class ThreadViewModel @AssistedInject constructor(
    @Assisted val id: Long,
    private val backend: Backend,
    private val homeRepo: HomeRepository,
    @ApplicationContext context: Context,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(id: Long): ThreadViewModel
    }

    val home: StateFlow<HomeRepository.State> = homeRepo.state

    sealed interface Ui {
        data object Loading : Ui

        data class Shown(val t: ThreadFull) : Ui

        data object Gone : Ui

        data object Unreachable : Ui
    }

    var ui by mutableStateOf<Ui>(Ui.Loading)
        private set
    private var known by mutableStateOf<Map<Long, Email>>(emptyMap())
    private var olderCursor: String? = null
    private var olderLoaded = false
    var loadingOlder by mutableStateOf(false)
        private set
    var olderFailed by mutableStateOf(false)
        private set
    private var markedFor: String? = null
    val composer = EmailComposer(id.toString(), backend, homeRepo, viewModelScope, context).also { it.load() }

    /** Newest first. */
    val items: List<Email> get() = known.values.sortedWith(compareByDescending<Email> { it.sentAt }.thenByDescending { it.id })

    val cursor: String? get() = if (olderLoaded) olderCursor else (ui as? Ui.Shown)?.t?.emails?.olderCursor

    init {
        viewModelScope.launch {
            backend.refresher(viewModelScope, setOf("email", "reaction"), Refresher.THREAD_POLL_MS) { load() }
            load()
        }
    }

    suspend fun load() {
        try {
            val t = backend.run { thread(id) }
            ui = Ui.Shown(t)
            known = known + t.emails.items.associateBy { it.id }
            mark(t)
        } catch (e: ApiException) {
            ui = when {
                e.status == 404 -> Ui.Gone
                ui is Ui.Shown -> ui
                else -> Ui.Unreachable
            }
        }
    }

    private fun mark(t: ThreadFull) {
        val me = homeRepo.state.value.home?.me?.id ?: return
        val unread = items.firstOrNull { it.senderId != me && it.readAt == null }?.id
        val reacted = if (t.thread.unseenReactions > 0) "${t.thread.unseenReactions}:${t.thread.lastReaction?.at.orEmpty()}" else null
        if (unread == null && reacted == null) return
        val key = "${unread ?: ""}|${reacted ?: ""}"
        if (markedFor == key) return
        markedFor = key
        viewModelScope.launch {
            try {
                backend.run { threadSeen(id) }
            } catch (_: ApiException) {
                markedFor = null
            }
            homeRepo.refreshSoon()
        }
    }

    fun seeOlder() {
        val c = cursor ?: return
        if (loadingOlder) return
        loadingOlder = true
        olderFailed = false
        viewModelScope.launch {
            try {
                val page = backend.run { olderEmails(id, c, 20) }
                known = known + page.items.associateBy { it.id }
                olderCursor = page.olderCursor
                olderLoaded = true
            } catch (_: ApiException) {
                olderFailed = true
            } finally {
                loadingOlder = false
            }
        }
    }

    fun sent(e: Email) {
        known = known + (e.id to e)
        viewModelScope.launch { load() }
    }

    fun reacted() {
        viewModelScope.launch { load() }
    }

    override fun onCleared() {
        composer.saver.flushOnHide()
        composer.saver.dispose()
    }
}

/** S15 New email (FS-E11–E15). */
@HiltViewModel
class NewEmailViewModel @Inject constructor(backend: Backend, home: HomeRepository, @ApplicationContext context: Context) : ViewModel() {
    val home: StateFlow<HomeRepository.State> = home.state
    val composer = EmailComposer("new", backend, home, viewModelScope, context).also { it.load() }

    override fun onCleared() {
        composer.saver.flushOnHide()
        composer.saver.dispose()
    }
}
