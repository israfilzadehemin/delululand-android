package com.delulutopia.feature.emails

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.EmailDraft
import com.delulutopia.core.network.model.EmailSent
import com.delulutopia.core.network.model.SaveEmailDraftBody
import com.delulutopia.core.ui.media.Attachments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * One email composer's state (FS-E11–E16; web emails/EmailComposer.tsx): an optional Subject (a new
 * email), the text with its 1024 counter, the strip with one video, one autosaved draft per key
 * (`new` or a thread id) with the letter draft's saver and conflict rule.
 */
@Stable
class EmailComposer(
    val key: String,
    private val backend: Backend,
    private val home: HomeRepository,
    private val scope: CoroutineScope,
    private val context: Context,
) {
    val withSubject = key == "new"
    var loaded by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var subject by mutableStateOf("")
        private set
    var text by mutableStateOf("")
        private set
    var trimmed by mutableStateOf(false)
        private set
    var attachments by mutableStateOf<Attachments?>(null)
        private set
    val saver = DraftSaver(scope, save = { v -> backend.run { saveEmailDraft(key, SaveEmailDraftBody(if (withSubject) subject else "", text, v)) } })

    fun load() {
        scope.launch {
            try {
                apply(backend.run { emailDraft(key) })
                loaded = true
            } catch (_: ApiException) {
                failed = true
            }
        }
    }

    fun apply(d: EmailDraft) {
        subject = d.subject
        text = d.text
        saver.load(d.version, d.savedAt)
        val atts = attachments
        if (atts == null) {
            attachments = Attachments(d.attachments, "api/emails/drafts/$key/attachments", scope, backend.uploader, context)
        } else {
            atts.reset(d.attachments)
        }
    }

    /** Q5: typing simply stops at 1024; only an overflowing paste gets the notice. */
    fun changeText(raw: String) {
        val (next, cut) = clamp(raw)
        trimmed = cut && count(normalize(raw)) - count(text) > 1
        text = next
        saver.touch()
    }

    fun changeSubject(raw: String) {
        subject = raw.replace(Regex("[\\r\\n]+"), " ").take(SUBJECT_MAX)
        saver.touch()
    }

    suspend fun loadServer() {
        runCatching { backend.run { emailDraft(key) } }.onSuccess { apply(it) }
    }

    fun deleteAttachment(id: String, onFail: () -> Unit) {
        scope.launch {
            try {
                backend.run { deleteEmailDraftAttachment(key, id) }
            } catch (_: ApiException) {
                onFail()
            }
        }
    }

    sealed interface SendResult {
        data class Sent(val sent: EmailSent) : SendResult

        data object Conflict : SendResult

        data class Failed(val message: String) : SendResult
    }

    suspend fun send(): SendResult {
        val version = try {
            saver.flush()
        } catch (_: DraftSaver.ConflictException) {
            return SendResult.Conflict
        } catch (_: Exception) {
            return SendResult.Failed(t("common.notSent"))
        }
        return try {
            val sent = backend.run { if (key == "new") sendNewEmail(DraftVersionBody(version)) else reply(key.toLong(), DraftVersionBody(version)) }
            val next = runCatching { backend.run { emailDraft(key) } }.getOrElse { EmailDraft(version = version + 1, savedAt = sent.email.sentAt) }
            apply(next)
            trimmed = false
            home.load()
            SendResult.Sent(sent)
        } catch (e: ApiException) {
            when {
                e.code == "NO_CREDITS" -> {
                    home.refreshSoon()
                    SendResult.Failed(creditsLine(home.state.value.home?.emailCredits?.copy(left = 0)) ?: e.message)
                }
                e.status == 422 || e.code == "VIDEO_LIMIT" -> SendResult.Failed(e.message)
                else -> SendResult.Failed(t("common.notSent"))
            }
        }
    }
}
