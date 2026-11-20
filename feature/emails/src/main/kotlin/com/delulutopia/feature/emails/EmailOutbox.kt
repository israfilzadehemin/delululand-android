package com.delulutopia.feature.emails

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DraftSaver
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.DraftVersionBody
import com.delulutopia.core.network.model.Email
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.network.model.SaveEmailDraftBody
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Optimistic send (doc "Delululand — Optimistic Send" OS1–OS14; web emails/outbox.ts): a thread reply
 * shows as a bubble at once and is sent here, outliving the thread screen for the app's life (OS10).
 * One email per thread can be in the air (OS7): the reply draft holds it until the send succeeds, so
 * the draft version doubles as the idempotency key; a 409 is reconciled (OS8).
 */
@Singleton
class EmailOutbox internal constructor(private val backend: Backend, private val scope: CoroutineScope) {
    @Inject constructor(backend: Backend) : this(backend, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

    enum class Status { Sending, Failed }

    enum class FailKind { Offline, Timeout, Server, Credits, Refused }

    data class Fail(val kind: FailKind, val message: String = "") {
        /** OS5: what Retry can fix. */
        val retryable: Boolean get() = kind == FailKind.Offline || kind == FailKind.Timeout || kind == FailKind.Server
    }

    /** What the draft held at Send: saved again on a retry from a composer that mounted later. */
    data class Snapshot(val text: String, val replyToId: String?)

    data class Entry(
        val threadId: String,
        /** The bubble: a provisional email with a `local-` id. */
        val email: Email,
        val snapshot: Snapshot,
        val status: Status,
        val fail: Fail? = null,
    ) {
        val attIds: List<String> get() = email.attachments.map { it.id }
    }

    sealed interface Resolution {
        /** [credits] is null when a retry found the email had landed already (OS8). */
        data class Sent(val email: Email, val credits: EmailCredits?) : Resolution

        data class Cancelled(val attIds: List<String>) : Resolution
    }

    /** Compose state, so screens and composers read it directly. */
    var entries by mutableStateOf<Map<String, Entry>>(emptyMap())
        private set

    /** OS11: these have spent their credit already. */
    val sendingCount: Int get() = entries.values.count { it.status == Status.Sending }

    private val _resolutions = MutableSharedFlow<Pair<String, Resolution>>(extraBufferCapacity = 16)
    val resolutions: SharedFlow<Pair<String, Resolution>> = _resolutions

    private val prepares = mutableMapOf<String, suspend () -> Int>()

    fun entry(threadId: String): Entry? = entries[threadId]

    /** The mounted composer saves the held draft through its own saver (no race with its autosave). */
    fun bind(threadId: String, prepare: suspend () -> Int) {
        prepares[threadId] = prepare
    }

    fun unbind(threadId: String, prepare: suspend () -> Int) {
        if (prepares[threadId] === prepare) prepares.remove(threadId)
    }

    /** OS3: the bubble shows now; the save and the send run behind it. */
    fun enqueue(email: Email, snapshot: Snapshot, prepare: suspend () -> Int) {
        val id = email.threadId
        if (entries.containsKey(id)) return
        prepares[id] = prepare
        entries = entries + (id to Entry(id, email, snapshot, Status.Sending))
        run(id)
    }

    /** OS5: Retry (offered only for what it can fix). */
    fun retry(threadId: String) {
        val e = entries[threadId] ?: return
        if (e.status != Status.Failed) return
        entries = entries + (threadId to e.copy(status = Status.Sending, fail = null))
        run(threadId)
    }

    /** OS6: Cancel discards it; the composer clears the draft and deletes its attachments. */
    fun cancel(threadId: String) {
        val e = entries[threadId] ?: return
        if (e.status != Status.Failed) return
        entries = entries - threadId
        _resolutions.tryEmit(threadId to Resolution.Cancelled(e.attIds))
    }

    private fun run(threadId: String) {
        scope.launch {
            val e = entries[threadId] ?: return@launch
            val result = runCatching { withTimeout(SEND_TIMEOUT_MS) { attempt(e) } }
            val cur = entries[threadId] ?: return@launch
            result.fold(
                onSuccess = { (email, credits) ->
                    entries = entries - threadId
                    _resolutions.tryEmit(threadId to Resolution.Sent(email, credits))
                },
                onFailure = { err -> entries = entries + (threadId to cur.copy(status = Status.Failed, fail = classify(err))) },
            )
        }
    }

    private suspend fun attempt(e: Entry): Pair<Email, EmailCredits?> {
        val version = try {
            prepares[e.threadId]?.invoke() ?: saveSnapshot(e)
        } catch (x: DraftSaver.ConflictException) {
            return reconcile(e)
        } catch (x: ApiException) {
            if (x.code == "DRAFT_CONFLICT") return reconcile(e)
            throw x
        }
        return try {
            backend.run { reply(e.threadId, DraftVersionBody(version)) }.let { it.email to it.credits }
        } catch (x: ApiException) {
            if (x.code == "DRAFT_CONFLICT") reconcile(e) else throw x
        }
    }

    private suspend fun saveSnapshot(e: Entry): Int {
        val d = backend.run { emailDraft(e.threadId) }
        return backend.run { saveEmailDraft(e.threadId, SaveEmailDraftBody("", e.snapshot.text, d.version, e.snapshot.replyToId)) }.version
    }

    /** OS8: the draft moved on. Still holding this email → send at its version; empty → it landed. */
    private suspend fun reconcile(e: Entry): Pair<Email, EmailCredits?> {
        val d = backend.run { emailDraft(e.threadId) }
        val ids = e.attIds.toSet()
        val holds = normalize(d.text).trim() == e.email.text && d.attachments.map { it.id }.toSet() == ids
        if (holds && (d.text.isNotBlank() || d.attachments.isNotEmpty())) {
            return backend.run { reply(e.threadId, DraftVersionBody(d.version)) }.let { it.email to it.credits }
        }
        if (d.text.isBlank() && d.attachments.isEmpty()) {
            val items = backend.run { thread(e.threadId) }.emails.items
            val landed = items.firstOrNull { it.senderId == e.email.senderId && it.text == e.email.text && it.attachments.map { a -> a.id }.toSet() == ids }
                ?: items.firstOrNull { it.senderId == e.email.senderId && it.text == e.email.text }
            if (landed != null) return landed to null
        }
        throw ApiException(409, "DRAFT_CONFLICT", "")
    }

    /** Tests only. */
    internal fun reset() {
        entries = emptyMap()
        prepares.clear()
    }

    companion object {
        /** OS8: no answer within this long counts as failed. */
        const val SEND_TIMEOUT_MS = 20_000L

        private val seq = AtomicInteger()

        fun classify(err: Throwable): Fail = when (err) {
            is TimeoutCancellationException -> Fail(FailKind.Timeout)
            is ApiException -> when {
                err.status == 0 -> Fail(FailKind.Offline)
                err.code == "NO_CREDITS" -> Fail(FailKind.Credits)
                err.status >= 500 -> Fail(FailKind.Server)
                err.code == "DRAFT_CONFLICT" -> Fail(FailKind.Refused)
                else -> Fail(FailKind.Refused, err.message)
            }
            // A failed autosave ("not saved") or no network at all.
            else -> Fail(FailKind.Offline)
        }

        fun provisional(threadId: String, senderId: String, text: String, attachments: List<Att>, replyTo: EmailQuote?, now: Instant = Instant.now()): Email =
            Email(
                id = "local-${now.toEpochMilli().toString(36)}-${seq.incrementAndGet()}",
                threadId = threadId,
                senderId = senderId,
                // As the API stores it (EmailText.forSend): CRLF → LF, then stripped.
                text = normalize(text).trim(),
                sentAt = now.toString(),
                attachments = attachments,
                replyTo = replyTo,
            )
    }
}
