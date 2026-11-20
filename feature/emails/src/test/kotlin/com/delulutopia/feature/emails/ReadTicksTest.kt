package com.delulutopia.feature.emails

import com.delulutopia.core.network.model.Email
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Read ticks (doc "Delululand — Read Ticks"): the state of my email (TK5) and the delivery card's rows (TK6, TK7). */
class ReadTicksTest {
    private fun email(delivered: String? = null, read: String? = null) =
        Email(id = "e1", threadId = "t1", senderId = "1", text = "Hi", sentAt = "2026-11-20T14:16:00Z", deliveredAt = delivered, readAt = read)

    @Test
    fun stateFollowsTheLatestMark() {
        assertEquals(TickStatus.Sent, TickStatus.of(email()))
        assertEquals(TickStatus.Delivered, TickStatus.of(email(delivered = "2026-11-20T14:16:05Z")))
        assertEquals(TickStatus.Read, TickStatus.of(email(delivered = "2026-11-20T14:16:05Z", read = "2026-11-20T14:17:00Z")))
        // Read implies delivered even when an older API sent no delivery time.
        assertEquals(TickStatus.Read, TickStatus.of(email(read = "2026-11-20T14:17:00Z")))
    }

    @Test
    fun listStatusFromTheWire() {
        assertEquals(TickStatus.Delivered, TickStatus.ofWire("delivered"))
        assertNull(TickStatus.ofWire(null))
        assertNull(TickStatus.ofWire("seen"))
    }

    @Test
    fun cardListsEveryStateReachedOnMine_sentOnlyOnTheFriends() {
        val e = email(delivered = "2026-11-20T14:16:05Z", read = "2026-11-20T14:17:00Z")
        assertEquals(listOf(TickStatus.Sent, TickStatus.Delivered, TickStatus.Read), deliveryRows(e, mine = true).map { it.first })
        assertEquals("2026-11-20T14:17:00Z", deliveryRows(e, mine = true).last().second)
        assertEquals(listOf(TickStatus.Sent), deliveryRows(e, mine = false).map { it.first })
        assertEquals(listOf(TickStatus.Sent), deliveryRows(email(), mine = true).map { it.first })
    }
}
