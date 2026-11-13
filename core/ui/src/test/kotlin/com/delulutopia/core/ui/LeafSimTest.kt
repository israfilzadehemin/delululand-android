package com.delulutopia.core.ui

import com.delulutopia.core.ui.leaves.LeafSim
import com.delulutopia.core.ui.leaves.LeafState
import com.delulutopia.core.ui.leaves.LeafView
import com.delulutopia.core.ui.leaves.Surface
import com.delulutopia.core.ui.leaves.SurfaceKind
import com.google.common.truth.Truth.assertThat
import kotlin.random.Random
import org.junit.Test

/** The leaves' simulation, seeded (web leaves.test.ts): budgets hold and leaves land on cards and the bar. */
class LeafSimTest {
    private val view = LeafView(412f, 800f, 64f, 720f)
    private val bar = Surface("bar", "ground", SurfaceKind.Ground, 0f, 412f, 720f, 800f)
    private val card = Surface("card", "/letters/1", SurfaceKind.Flat, 16f, 396f, 300f, 380f)

    @Test fun staysWithinBudgetAndLands() {
        val sim = LeafSim(1f, Random(7))
        sim.start(view)
        repeat(60 * 40) { sim.step(1f / 60, view, listOf(bar, card)) }
        val air = sim.leaves.count { it.state == LeafState.Fall && !it.fading }
        val onBar = sim.leaves.count { it.state != LeafState.Fall && it.ground == true && !it.fading }
        val onCard = sim.leaves.count { it.state != LeafState.Fall && it.ground == false && !it.fading }
        assertThat(air).isAtMost(30)
        assertThat(onBar).isAtMost(16 + 2)
        assertThat(onBar + onCard).isGreaterThan(0)
    }

    @Test fun restingLeafFollowsItsCard() {
        val sim = LeafSim(1f, Random(3))
        sim.start(view)
        sim.step(1f / 60, view, listOf(bar, card))
        val resting = sim.leaves.filter { it.ground == false && it.state == LeafState.Rest }
        if (resting.isEmpty()) return
        val before = resting.first().y
        val moved = Surface("card", "/letters/1", SurfaceKind.Flat, 16f, 396f, 250f, 330f)
        sim.step(1f / 60, view, listOf(bar, moved))
        assertThat(resting.first().y).isLessThan(before - 40f)
    }
}
