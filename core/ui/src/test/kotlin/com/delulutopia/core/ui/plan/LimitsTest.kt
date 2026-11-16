package com.delulutopia.core.ui.plan

import com.delulutopia.core.network.model.Att
import com.delulutopia.core.network.model.Plan
import com.delulutopia.core.network.model.PlanLeft
import com.delulutopia.core.network.model.PlanLimits
import com.delulutopia.core.ui.media.Item
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Composer limits (doc "Delululand — Subscription Tiers" CM-1–CM-8; web src/plan/limits.test.ts). */
class LimitsTest {
    private fun free(photos: Int = 10, videos: Int = 3) = Plan(
        tier = "free",
        limits = PlanLimits(100, 5, 10, 15, 100, 3, 1),
        left = PlanLeft(100, photos, videos, 1),
        renewsAt = "2026-11-17T00:00:00Z",
    )

    @Test fun nothingWithoutAPlan() {
        assertThat(stripLimits(null, MessageMedia(30))).isNull()
    }

    @Test fun quietWhilePlentyIsLeft() {
        val l = stripLimits(free(), MessageMedia(2))!!
        assertThat(l.lines).isEmpty()
        assertThat(l.block).isNull()
        assertThat(l.photoRoom).isEqualTo(3)
    }

    @Test fun perMessageLimit() {
        val l = stripLimits(free(), MessageMedia(5))!!
        assertThat(l.photoRoom).isEqualTo(0)
        assertThat(l.lines.first()).isEqualTo(LimitLine.PerMessage(5))
    }

    @Test fun leftCountsTheDraft() {
        val l = stripLimits(free(photos = 4), MessageMedia(2))!!
        assertThat(l.lines).contains(LimitLine.PhotosLeft(2))
    }

    @Test fun overLimitDraftsAreRefused() {
        assertThat(stripLimits(free(), MessageMedia(7))!!.block).isEqualTo(LimitBlock.OverMessage(5, 2))
        assertThat(stripLimits(free(photos = 2), MessageMedia(3))!!.block).isEqualTo(LimitBlock.PhotosOver(2, 1))
    }

    @Test fun anEditSpendsOnlyWhatItAdds() {
        fun att(id: String, kind: String) = Item(id, att = Att(id = id, kind = kind, url = "/a/$id"))
        val items = listOf(att("a", "photo"), att("b", "photo"), att("c", "photo"), att("v", "video"))
        assertThat(mediaOf(items, setOf("a", "b"))).isEqualTo(MessageMedia(3, 1, 1))
    }
}
