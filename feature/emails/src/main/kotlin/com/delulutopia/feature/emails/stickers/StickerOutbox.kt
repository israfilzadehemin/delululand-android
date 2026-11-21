package com.delulutopia.feature.emails.stickers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Backend
import com.delulutopia.core.network.model.Email
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.network.model.SendStickerBody
import com.delulutopia.core.network.model.StickerRef
import com.delulutopia.feature.emails.EmailOutbox
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Optimistic sticker sends (doc "Delululand — Stickers" FS-SK6, FS-SK7; web stickers/outbox.ts): one tap
 * shows the sticker at once and sends it behind it. Several can be in the air: each has its own client
 * id, so a retry after a lost answer returns the first email and spends nothing.
 */
@Singleton
class StickerOutbox internal constructor(private val backend: Backend, private val scope: CoroutineScope) {
    @Inject constructor(backend: Backend) : this(backend, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

    data class Entry(
        val threadId: String,
        /** The provisional email (a `local-` id) drawn as a sticker. */
        val email: Email,
        val clientId: String,
        val status: EmailOutbox.Status,
        val fail: EmailOutbox.Fail? = null,
    )

    data class Sent(val threadId: String, val local: String, val email: Email, val credits: EmailCredits)

    /** By local id, in the order they were sent. */
    var entries by mutableStateOf<Map<String, Entry>>(linkedMapOf())
        private set

    private val _sent = MutableSharedFlow<Sent>(extraBufferCapacity = 16)
    val sent: SharedFlow<Sent> = _sent

    fun of(threadId: String): List<Entry> = entries.values.filter { it.threadId == threadId }

    val sendingCount: Int get() = entries.values.count { it.status == EmailOutbox.Status.Sending }

    /** FS-SK6: the sticker shows now; the send runs behind it. */
    fun send(threadId: String, senderId: String, sticker: StickerRef, replyTo: EmailQuote?, now: Instant = Instant.now()): Email {
        val email = Email(
            id = "local-s${now.toEpochMilli().toString(36)}-${seq.incrementAndGet()}",
            threadId = threadId,
            senderId = senderId,
            text = "",
            sentAt = now.toString(),
            replyTo = replyTo,
            sticker = sticker,
        )
        entries = LinkedHashMap(entries).apply { put(email.id, Entry(threadId, email, UUID.randomUUID().toString(), EmailOutbox.Status.Sending)) }
        run(email.id)
        return email
    }

    /** Retry keeps the client id, so the server never makes a second one. */
    fun retry(local: String) {
        val e = entries[local] ?: return
        if (e.status != EmailOutbox.Status.Failed) return
        entries = LinkedHashMap(entries).apply { put(local, e.copy(status = EmailOutbox.Status.Sending, fail = null)) }
        run(local)
    }

    fun cancel(local: String) {
        val e = entries[local] ?: return
        if (e.status != EmailOutbox.Status.Failed) return
        entries = LinkedHashMap(entries).apply { remove(local) }
    }

    private fun run(local: String) {
        scope.launch {
            val e = entries[local] ?: return@launch
            val sticker = e.email.sticker ?: return@launch
            val result = runCatching {
                withTimeout(EmailOutbox.SEND_TIMEOUT_MS) {
                    backend.run { sendSticker(e.threadId, SendStickerBody(sticker.id, e.email.replyTo?.id, e.clientId)) }
                }
            }
            val cur = entries[local] ?: return@launch
            result.fold(
                onSuccess = { s ->
                    entries = LinkedHashMap(entries).apply { remove(local) }
                    _sent.tryEmit(Sent(e.threadId, local, s.email, s.credits))
                },
                onFailure = { err ->
                    entries = LinkedHashMap(entries).apply { put(local, cur.copy(status = EmailOutbox.Status.Failed, fail = EmailOutbox.classify(err))) }
                },
            )
        }
    }

    /** Tests only. */
    internal fun reset() {
        entries = linkedMapOf()
    }

    private companion object {
        val seq = AtomicInteger()
    }
}
