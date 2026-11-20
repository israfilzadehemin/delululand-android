package com.delulutopia.feature.emails

import com.delulutopia.core.network.model.Reaction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Bubble actions (doc "Delululand — Bubble Actions"): the pill's numbers (BA7) and where the time goes (BA1). */
class BubbleActionsTest {
    @Test
    fun pillShowsTheFirstThreeEmojisInChipOrder_andTheTotalOfReactions() {
        val chips = listOf(
            Reaction("☕", listOf("1", "2")),
            Reaction("🔥", listOf("2")),
            Reaction("❤️", listOf("1")),
            Reaction("😂", listOf("2")),
        )
        val (shown, total) = pillParts(chips)
        assertEquals(listOf("☕", "🔥", "❤️"), shown)
        assertEquals(5, total)
        assertEquals(listOf("🔥") to 1, pillParts(listOf(Reaction("🔥", listOf("2")))))
    }

    @Test
    fun timeSharesTheLastLineOnlyWhenItFits() {
        assertTrue(timeFits(lastLineWidth = 100f, gap = 8f, timeWidth = 40f, maxWidth = 200f))
        assertTrue(timeFits(lastLineWidth = 152f, gap = 8f, timeWidth = 40f, maxWidth = 200f))
        assertFalse(timeFits(lastLineWidth = 153f, gap = 8f, timeWidth = 40f, maxWidth = 200f))
    }

    @Test
    fun longPressIsTheSameAsTheWeb() {
        assertEquals(450L, LONG_PRESS_MS)
        assertEquals(10f, LONG_PRESS_SLOP_DP, 0f)
    }
}
