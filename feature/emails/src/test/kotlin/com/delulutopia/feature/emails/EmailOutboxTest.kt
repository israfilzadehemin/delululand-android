package com.delulutopia.feature.emails

import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Att
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Optimistic send (doc "Delululand — Optimistic Send"): what Retry can fix (OS5) and the provisional bubble (OS3). */
class EmailOutboxTest {
    @Test
    fun offlineTimeoutAndServerAreRetryable_refusalsAndCreditsAreNot() {
        assertEquals(EmailOutbox.FailKind.Offline, EmailOutbox.classify(ApiException(0, ApiException.OFFLINE, "")).kind)
        assertTrue(EmailOutbox.classify(ApiException(0, ApiException.OFFLINE, "")).retryable)
        assertTrue(EmailOutbox.classify(ApiException(503, "HTTP_503", "")).retryable)
        assertTrue(EmailOutbox.classify(IllegalStateException("not saved")).retryable)
        assertFalse(EmailOutbox.classify(ApiException(409, "NO_CREDITS", "")).retryable)
        val refused = EmailOutbox.classify(ApiException(422, "TOO_LONG", "Too long"))
        assertFalse(refused.retryable)
        assertEquals("Too long", refused.message)
    }

    @Test
    fun provisionalIsStoredLikeTheApi_withALocalIdAndItsAttachments() {
        val now = Instant.parse("2026-11-20T10:00:00Z")
        val att = Att(id = "a1", kind = "photo", url = "/p", position = 0)
        val a = EmailOutbox.provisional("t1", "u1", "  hi\r\nthere \n", listOf(att), null, now)
        val b = EmailOutbox.provisional("t1", "u1", "x", emptyList(), null, now)
        assertEquals("hi\nthere", a.text)
        assertTrue(a.id.startsWith("local-"))
        assertNotEquals(a.id, b.id)
        assertEquals("2026-11-20T10:00:00Z", a.sentAt)
        assertEquals(listOf("a1"), a.attachments.map { it.id })
    }
}
