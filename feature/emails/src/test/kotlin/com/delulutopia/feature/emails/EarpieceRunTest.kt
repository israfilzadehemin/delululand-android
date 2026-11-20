package com.delulutopia.feature.emails

import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.Email
import org.junit.Assert.assertEquals
import org.junit.Test

/** Earpiece Playback EP7 (doc "Delululand — Earpiece Playback"): which emails' voice notes play on. */
class EarpieceRunTest {
    private fun email(id: String, sender: String, vararg voices: String, photo: Boolean = false) = Email(
        id = id,
        threadId = "t",
        senderId = sender,
        sentAt = "2026-11-20T10:00:00Z",
        attachments = voices.map { Att(id = it, kind = "audio", url = "/media/$it") } +
            if (photo) listOf(Att(id = "p$id", kind = "photo", url = "/media/p$id")) else emptyList(),
    )

    // Newest first, as the thread holds them.
    private val items = listOf(
        email("6", "friend", "f1"),
        email("5", "me", "c"),
        email("4", "me", "b1", "b2"),
        email("3", "me", "a"),
        email("2", "me", photo = true),
        email("1", "me", "z"),
    )

    @Test
    fun followsTheSameSendersNewerEmailsInChatOrder() {
        assertEquals(listOf("/media/b1", "/media/b2", "/media/c"), voiceRunAfter(items, 3))
    }

    @Test
    fun stopsAtTheFriendsEmail() {
        assertEquals(emptyList<String>(), voiceRunAfter(items, 1))
    }

    @Test
    fun stopsAtAnEmailWithoutAVoiceNote() {
        assertEquals(emptyList<String>(), voiceRunAfter(items, 5))
    }

    @Test
    fun theNewestEmailHasNothingAfterIt() {
        assertEquals(emptyList<String>(), voiceRunAfter(items, 0))
    }
}
