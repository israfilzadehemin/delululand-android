package com.delulutopia.feature.discussions

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.designsystem.theme.NoteColor
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.AppConfig
import com.delulutopia.core.network.model.DDraft
import com.delulutopia.core.network.model.DFull
import com.delulutopia.core.network.model.Doc
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.EditDiscussionBody
import com.delulutopia.core.network.model.SaveDiscussionDraftBody
import com.delulutopia.core.network.model.isEmptyDoc
import com.delulutopia.core.ui.media.Attachments
import com.delulutopia.core.ui.noteColorOf
import com.delulutopia.core.ui.wire
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

const val TITLE_MAX = 120

/**
 * S12 Compose (FS-D18–D23; web discussions/ComposeScreen.tsx) on the one autosaved discussion
 * draft, or editing a posted discussion (Discussion Controls D1–D5; EditDiscussionScreen.tsx), where
 * nothing is saved until Save.
 */
@HiltViewModel(assistedFactory = ComposeViewModel.Factory::class)
class ComposeViewModel @AssistedInject constructor(
    @Assisted val editId: Long?,
    private val backend: Backend,
    private val home: HomeRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(editId: Long?): ComposeViewModel
    }

    val homeState: StateFlow<HomeRepository.State> = home.state

    sealed interface Load {
        data object Loading : Load

        data object Failed : Load

        data class Ready(val body: Doc) : Load

        data class NotEditable(val id: Long) : Load
    }

    var load by mutableStateOf<Load>(Load.Loading)
        private set
    var title by mutableStateOf("")
        private set
    var colour by mutableStateOf(NoteColor.Lemon)
        private set
    var body: Doc = Doc.EMPTY
        private set
    var empty by mutableStateOf(true)
        private set
    var attachments by mutableStateOf<Attachments?>(null)
        private set
    var saver: DraftSaver? = null
        private set
    var original by mutableStateOf<DFull?>(null)
        private set
    var config by mutableStateOf<AppConfig?>(null)
        private set

    init {
        viewModelScope.launch { if (editId != null) loadEdit(editId) else loadDraft() }
        viewModelScope.launch { config = runCatching { backend.run { config() } }.getOrNull() }
    }

    private suspend fun loadDraft() {
        val d = try {
            backend.run { discussionDraft() }
        } catch (_: ApiException) {
            load = Load.Failed
            return
        }
        val s = DraftSaver(viewModelScope, save = { v -> backend.run { saveDiscussionDraft(SaveDiscussionDraftBody(title, body, v, colour.wire)) } })
        saver = s
        s.load(d.version, d.savedAt)
        title = d.title
        colour = noteColorOf(d.color)
        body = d.body
        empty = d.body.isEmptyDoc()
        attachments = Attachments(d.attachments, "api/discussions/draft/attachments", viewModelScope, backend.uploader, context)
        load = Load.Ready(d.body)
    }

    private suspend fun loadEdit(id: Long) {
        val d = try {
            backend.run { discussion(id) }
        } catch (_: ApiException) {
            load = Load.NotEditable(id)
            return
        }
        if (!d.canEdit) {
            load = Load.NotEditable(id)
            return
        }
        original = d
        title = d.title
        colour = noteColorOf(d.color)
        body = d.body
        empty = d.body.isEmptyDoc()
        attachments = Attachments(d.attachments, "api/discussions/$id/attachments", viewModelScope, backend.uploader, context)
        load = Load.Ready(d.body)
    }

    fun onBody(doc: Doc) {
        body = doc
        empty = doc.isEmptyDoc()
        saver?.touch()
    }

    fun changeTitle(t: String) {
        title = t.take(TITLE_MAX)
        saver?.touch()
    }

    fun changeColour(c: NoteColor) {
        colour = c
        saver?.touch()
    }

    fun flushOnHide() {
        saver?.flushOnHide()
    }

    suspend fun checkNewer(): Doc? {
        val s = saver ?: return null
        if (s.dirty) return null
        val d = runCatching { backend.run { discussionDraft() } }.getOrNull() ?: return null
        if (d.version <= s.version || s.dirty) return null
        return apply(d)
    }

    suspend fun loadServer(): Doc? = runCatching { backend.run { discussionDraft() } }.getOrNull()?.let { apply(it) }

    private fun apply(d: DDraft): Doc {
        title = d.title
        colour = noteColorOf(d.color)
        body = d.body
        empty = d.body.isEmptyDoc()
        saver?.load(d.version, d.savedAt)
        attachments?.reset(d.attachments)
        return d.body
    }

    fun keepMine() = saver?.keepMine()

    fun deleteDraftAttachment(id: String, onFail: () -> Unit) {
        viewModelScope.launch {
            try {
                backend.run { deleteDiscussionDraftAttachment(id) }
            } catch (_: ApiException) {
                onFail()
            }
        }
    }

    sealed interface PostResult {
        data class Posted(val id: Long) : PostResult

        data object Conflict : PostResult

        data class Failed(val message: String) : PostResult
    }

    /** FS-D21: flush, then post exactly that version. */
    suspend fun post(): PostResult {
        val s = saver ?: return PostResult.Failed(t("common.notPosted"))
        val version = try {
            s.flush()
        } catch (_: DraftSaver.ConflictException) {
            return PostResult.Conflict
        } catch (_: Exception) {
            return PostResult.Failed(t("common.notPosted"))
        }
        return try {
            val posted = backend.run { post(DraftVersionBody(version)) }
            home.load()
            PostResult.Posted(posted.id)
        } catch (e: ApiException) {
            PostResult.Failed(if (e.code in setOf("TITLE_REQUIRED", "EMPTY_DISCUSSION", "TITLE_TOO_LONG")) e.message else t("common.notPosted"))
        }
    }

    /** D1–D5: null when saved; "gone" when the discussion was deleted meanwhile; else the error line. */
    suspend fun save(): String? {
        val d = original ?: return null
        val atts = attachments ?: return null
        return try {
            backend.run { editDiscussion(d.id, EditDiscussionBody(title.trim(), body, colour.wire, atts.done.map { it.id })) }
            home.refreshSoon()
            null
        } catch (e: ApiException) {
            when {
                e.status == 404 -> GONE
                e.status in 400..499 -> e.message
                else -> t("reactions.notSavedTryAgain")
            }
        }
    }

    suspend fun discardEdit() {
        val d = original ?: return
        runCatching { backend.run { discardDiscussionEdit(d.id) } }
    }

    override fun onCleared() {
        saver?.flushOnHide()
        saver?.dispose()
        home.refreshSoon()
    }

    companion object {
        const val GONE = "gone"
    }
}
