package com.delulutopia.feature.letters

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.LettersRepository
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.AppConfig
import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.BodyOnly
import com.delulutopia.core.network.model.Doc
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.LetterFull
import com.delulutopia.core.network.model.Row
import com.delulutopia.core.network.model.SaveDraftBody
import com.delulutopia.core.network.model.SendAgainBody
import com.delulutopia.core.network.model.StampCatalogue
import com.delulutopia.core.network.model.StampMark
import com.delulutopia.core.network.model.isEmptyDoc
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.media.Attachments
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * S4 Write (FS-W1–W16; web write/DraftWrite.tsx and EditWrite.tsx): the one draft with autosave
 * and conflict handling, or a sent letter still in its window. Attachments upload as they are added;
 * the stamp, the pin and the text ride along on every save.
 */
@HiltViewModel
class WriteViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val backend: Backend,
    private val home: HomeRepository,
    private val letters: LettersRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val route = handle.toRoute<WriteRoute>()
    val editId: Long? = route.editId
    val homeState: StateFlow<HomeRepository.State> = home.state

    sealed interface Load {
        data object Loading : Load

        data object Failed : Load

        data class Ready(val body: Doc) : Load

        /** Edit mode on a letter that can no longer be edited: show the letter instead. */
        data class NotEditable(val id: Long) : Load
    }

    var load by mutableStateOf<Load>(Load.Loading)
        private set
    var body: Doc = Doc.EMPTY
        private set
    var stamp by mutableStateOf<StampMark?>(null)
        private set
    var pinned by mutableStateOf<Long?>(null)
        private set
    var attachments by mutableStateOf<Attachments?>(null)
        private set
    var empty by mutableStateOf(true)
        private set

    /** Draft mode. */
    var saver: DraftSaver? = null
        private set

    /** Edit mode: the letter as sent. */
    var letter by mutableStateOf<LetterFull?>(null)
        private set
    var lateByServer by mutableStateOf(false)
        private set

    /** The delay and window S5 states (from `/api/config`). */
    var config by mutableStateOf<AppConfig?>(null)
        private set

    init {
        viewModelScope.launch { if (editId != null) loadEdit(editId) else loadDraft() }
        viewModelScope.launch { config = runCatching { backend.run { config() } }.getOrNull() }
    }

    private suspend fun loadDraft() {
        val d = try {
            backend.run { draft() }
        } catch (_: ApiException) {
            load = Load.Failed
            return
        }
        val s = DraftSaver(viewModelScope, save = { v -> backend.run { saveDraft(SaveDraftBody(body, pinned, stamp?.slug, v)) } })
        saver = s
        s.load(d.version, d.savedAt)
        body = d.body
        stamp = d.stamp
        attachments = Attachments(d.attachments, "api/draft/attachments", viewModelScope, backend.uploader, context)
        // Replies A1, A2: Reply pins; an empty draft opened without it is a new letter and drops an old pin.
        val freshStart = route.replyTo == null && d.body.isEmptyDoc() && d.attachments.isEmpty()
        pinned = route.replyTo ?: if (freshStart) null else d.pinnedLetterId
        if (pinned != d.pinnedLetterId) s.touch()
        empty = d.body.isEmptyDoc()
        load = Load.Ready(d.body)
    }

    private suspend fun loadEdit(id: Long) {
        val l = try {
            backend.run { letter(id) }
        } catch (_: ApiException) {
            load = Load.Failed
            return
        }
        if (!l.canEdit) {
            load = Load.NotEditable(id)
            return
        }
        letter = l
        body = l.body
        stamp = l.stamp
        val atts = (l.attachments + l.editAttachments.orEmpty()).sortedBy { it.position }
        attachments = Attachments(atts, "api/letters/$id/attachments", viewModelScope, backend.uploader, context)
        empty = l.body.isEmptyDoc()
        load = Load.Ready(l.body)
    }

    fun onBody(doc: Doc) {
        body = doc
        empty = doc.isEmptyDoc()
        saver?.touch()
    }

    /** FS-ST3: a stamp is never content. */
    fun chooseStamp(s: StampMark?) {
        stamp = s
        saver?.touch()
    }

    fun pin(id: Long?) {
        if (id == pinned) return
        pinned = id
        saver?.touch()
    }

    /** Leaving or hiding the screen saves at once (FS-W3). */
    fun flushOnHide() {
        saver?.flushOnHide()
    }

    /** Back on this screen: take a newer draft saved elsewhere if nothing here is unsaved (FS-W4). */
    suspend fun checkNewer(): Doc? {
        val s = saver ?: return null
        if (s.dirty) return null
        val d = runCatching { backend.run { draft() } }.getOrNull() ?: return null
        if (d.version <= s.version || s.dirty) return null
        apply(d.body, d.version, d.savedAt, d.pinnedLetterId, d.stamp, d.attachments)
        return d.body
    }

    /** The conflict dialog's Load. */
    suspend fun loadServer(): Doc? {
        val d = runCatching { backend.run { draft() } }.getOrNull() ?: return null
        apply(d.body, d.version, d.savedAt, d.pinnedLetterId, d.stamp, d.attachments)
        return d.body
    }

    private fun apply(doc: Doc, version: Int, savedAt: String?, pin: Long?, st: StampMark?, atts: List<Att>) {
        body = doc
        empty = doc.isEmptyDoc()
        saver?.load(version, savedAt)
        pinned = pin
        stamp = st
        attachments?.reset(atts)
    }

    fun keepMine() = saver?.keepMine()

    fun deleteDraftAttachment(id: String, onFail: () -> Unit) {
        viewModelScope.launch {
            try {
                backend.run { deleteDraftAttachment(id) }
            } catch (_: ApiException) {
                onFail()
            }
        }
    }

    sealed interface SendResult {
        data object Sent : SendResult

        data object Conflict : SendResult

        data class Failed(val message: String) : SendResult
    }

    /** S5 Send: flush, then send exactly that version (FS-S1–S3). */
    suspend fun send(): SendResult {
        val s = saver ?: return SendResult.Failed("Not sent. Try again.")
        val version = try {
            s.flush()
        } catch (_: DraftSaver.ConflictException) {
            return SendResult.Conflict
        } catch (_: Exception) {
            return SendResult.Failed("Not sent. Try again.")
        }
        return try {
            backend.run { send(DraftVersionBody(version)) }
            home.load()
            letters.refreshSoon()
            SendResult.Sent
        } catch (e: ApiException) {
            SendResult.Failed(if (e.code == "EMPTY_LETTER") "Nothing to send yet." else "Not sent. Try again.")
        }
    }

    // Edit mode (FS-S7–S12)

    suspend fun sendAgain(): String? {
        val l = letter ?: return null
        val atts = attachments ?: return null
        return try {
            backend.run { sendAgain(l.id, SendAgainBody(body, atts.done.map { it.id }, stamp?.slug)) }
            home.load()
            letters.refreshSoon()
            null
        } catch (e: ApiException) {
            if (e.code == "WINDOW_CLOSED") {
                lateByServer = true
                ""
            } else {
                if (e.status in 400..499) e.message else "Not sent. Try again."
            }
        }
    }

    suspend fun discardEdit() {
        val l = letter ?: return
        runCatching { backend.run { discardEdit(l.id) } }
        home.refreshSoon()
        letters.refreshSoon()
    }

    suspend fun keepInDraft(): Boolean {
        val l = letter ?: return false
        return try {
            backend.run { keepInDraft(l.id, BodyOnly(body)) }
            home.load()
            letters.refreshSoon()
            true
        } catch (_: ApiException) {
            false
        }
    }

    // Stamps (doc "Delululand — Stamps")

    var stamps by mutableStateOf<StampCatalogue?>(null)
        private set
    var stampsFailed by mutableStateOf(false)
        private set

    fun loadStamps() {
        if (stamps != null) return
        viewModelScope.launch {
            try {
                stamps = backend.run { stamps() }
                stampsFailed = false
            } catch (_: ApiException) {
                stampsFailed = true
            }
        }
    }

    // The reference panel (FS-W5; Replies A1–A4): received letters that have arrived, and the pinned one.

    var received by mutableStateOf<List<Row>>(emptyList())
        private set
    private var receivedCursor: String? = null
    var receivedMore by mutableStateOf(true)
        private set
    var referenced by mutableStateOf<LetterFull?>(null)
        private set

    fun loadReceived() {
        if (!receivedMore) return
        viewModelScope.launch {
            runCatching { backend.run { letters(cursor = receivedCursor, limit = 20, direction = "from") } }.onSuccess { page ->
                received = received + page.items.filter { it.state == LetterState.DELIVERED || it.state == LetterState.READ }
                receivedCursor = page.nextCursor
                receivedMore = page.nextCursor != null
            }
        }
    }

    /** Showing a letter here fetches it in full, which marks it read (T8), as opening it does. */
    fun loadReferenced(id: Long) {
        if (referenced?.id == id) return
        viewModelScope.launch { runCatching { backend.run { letter(id) } }.onSuccess { referenced = it } }
    }

    override fun onCleared() {
        saver?.flushOnHide()
        saver?.dispose()
        home.refreshSoon()
    }
}
