package com.delulutopia.feature.emails

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.EmailDraft
import com.delulutopia.core.network.model.EmailQuote
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
    /** Optimistic send for thread replies (doc "Delululand — Optimistic Send" OS1); null on New email. */
    private val outbox: EmailOutbox? = null,
) {
    /** Home, for the plan's limits (Subscription Tiers CM-1–CM-7). */
    val homeState = home.state
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

    /** Email Replies RE10: the email this reply answers, saved with the draft. */
    var replyTo by mutableStateOf<EmailQuote?>(null)
        private set

    /** RE8: bumped when a reply is picked, so the bar puts the cursor in the field. */
    var focusAsk by mutableIntStateOf(0)
        private set
    val optimistic = outbox != null && !withSubject

    /** OS11: emails in the air have spent their credit already. */
    val inAir: Int get() = outbox?.sendingCount ?: 0

    /** OS7: what the draft holds while an email of this thread is in the air or failed. */
    private var hold: EmailOutbox.Snapshot? = null
    private var releasing by mutableStateOf(false)

    /** OS7: Send and + wait while this thread has an email in the air or failed. */
    val unsent: EmailOutbox.Entry? get() = if (optimistic) outbox?.entries?.get(key) else null
    val held: Boolean get() = optimistic && (unsent != null || releasing)

    val saver = DraftSaver(scope, save = { v ->
        val h = hold
        val body = if (h != null) {
            SaveEmailDraftBody("", h.text, v, h.replyToId)
        } else {
            SaveEmailDraftBody(if (withSubject) subject else "", text, v, if (withSubject) null else replyTo?.id)
        }
        backend.run { saveEmailDraft(key, body) }
    })

    private val prepare: suspend () -> Int = {
        saver.touch()
        saver.flush()
    }

    init {
        if (optimistic) {
            outbox!!.bind(key, prepare)
            scope.launch { outbox.resolutions.collect { (k, r) -> if (k == key) release(r) } }
        }
    }

    /** The view model is going: Retry falls back to saving the snapshot itself. */
    fun dispose() {
        outbox?.unbind(key, prepare)
    }

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
        // OS10: opened while one is in the air or failed: the draft is that email, the screen starts empty.
        val e = unsent
        hold = e?.snapshot
        subject = d.subject
        text = if (e != null) "" else d.text
        replyTo = if (e != null) null else d.replyTo
        saver.load(d.version, d.savedAt)
        val shown = if (e != null) emptyList() else d.attachments
        val atts = attachments
        if (atts == null) {
            attachments = Attachments(shown, "api/emails/drafts/$key/attachments", scope, backend.uploader, context)
        } else {
            atts.reset(shown)
        }
    }

    /** OS3: the bubble shows at once; the composer clears and the send runs behind it. */
    fun sendOptimistic(senderId: String) {
        val box = outbox ?: return
        val atts = attachments ?: return
        val snap = EmailOutbox.Snapshot(text, replyTo?.id)
        val email = EmailOutbox.provisional(key, senderId, text, atts.items.mapNotNull { it.att }, replyTo)
        hold = snap
        text = ""
        replyTo = null
        trimmed = false
        atts.reset(emptyList())
        box.enqueue(email, snap, prepare)
    }

    /** Sent or cancelled: the draft is free again; what was typed meanwhile becomes the draft. */
    private suspend fun release(r: EmailOutbox.Resolution) {
        releasing = true
        try {
            if (r is EmailOutbox.Resolution.Cancelled) {
                r.attIds.forEach { id -> runCatching { backend.run { deleteEmailDraftAttachment(key, id) } } }
            }
            val d = runCatching { backend.run { emailDraft(key) } }.getOrNull()
            hold = null
            saver.load(d?.version ?: saver.version, d?.savedAt)
            // OS6: Cancel overwrites the held text; otherwise save only what is worth keeping.
            if (r is EmailOutbox.Resolution.Cancelled || text.isNotBlank() || replyTo != null) saver.touch()
        } finally {
            releasing = false
        }
        home.load()
    }

    /** Q5: typing simply stops at 1024; only an overflowing paste gets the notice. */
    fun changeText(raw: String) {
        val (next, cut) = clamp(raw)
        trimmed = cut && count(normalize(raw)) - count(text) > 1
        text = next
        if (hold == null) saver.touch()
    }

    /** RE6–RE8: answer this email; a new pick replaces the last. */
    fun pickReply(quote: EmailQuote) {
        if (withSubject) return
        replyTo = quote
        if (hold == null) saver.touch()
        focusAsk++
    }

    /** RE9: × on the strip. */
    fun cancelReply() {
        replyTo = null
        if (hold == null) saver.touch()
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
            val sent = backend.run { if (key == "new") sendNewEmail(DraftVersionBody(version)) else reply(key, DraftVersionBody(version)) }
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
