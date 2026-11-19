package com.delulutopia.feature.emails

import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.Email
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Email Replies (doc "Delululand — Email Replies"): the swipe curve (RE6) and quotes built on the device (RE11). */
class EmailReplyTest {
    @Test
    fun swipeFollowsTowardTheCentreOnly_easesPastTheArmPoint_stopsAtTheMax() {
        assertEquals(0f, swipeOffset(-30f, 1), 0f)
        assertEquals(40f, swipeOffset(40f, 1), 0f)
        assertEquals(SWIPE_ARM_DP, swipeOffset(SWIPE_ARM_DP, 1), 0f)
        assertEquals(SWIPE_ARM_DP + 7f, swipeOffset(SWIPE_ARM_DP + 20f, 1), 0.001f)
        assertEquals(SWIPE_MAX_DP, swipeOffset(500f, 1), 0f)
        assertEquals(-40f, swipeOffset(-40f, -1), 0f)
        assertEquals(0f, swipeOffset(40f, -1), 0f)
    }

    @Test
    fun quoteIsOneLineOfAtMost120_withMediaCountsAndTheFirstThumb() {
        val base = Email(id = "e1", threadId = "t", senderId = "u2", sentAt = "2026-11-19T10:00:00Z")
        val long = quoteOf(base.copy(text = "a\n\n" + "b".repeat(200)))
        assertEquals(120, long.text!!.length)
        assertEquals("a b", long.text!!.take(3))
        val photo = quoteOf(base.copy(attachments = listOf(Att(id = "p", kind = "photo", url = "/p", thumbUrl = "/p-thumb", position = 0))))
        assertNull(photo.text)
        assertEquals("/p-thumb", photo.thumbUrl)
        assertEquals(1, photo.media!!.photos)
        assertEquals("u2", photo.senderId)
    }
}
