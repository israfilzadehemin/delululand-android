package com.delulutopia.core.ui.doodle

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** The pad's history (web doodle/doodle.test.ts): undo, redo, clear, blank and unchanged. */
class DoodleTest {
    private fun stroke(erase: Boolean = false) = Stroke(0, 6f, erase).apply { extend(1f, 1f) }

    @Test fun blankUntilInk() {
        var h = History()
        assertThat(h.isBlank).isTrue()
        h = h.add(stroke(erase = true))
        assertThat(h.isBlank).isTrue()
        h = h.add(stroke())
        assertThat(h.isBlank).isFalse()
    }

    @Test fun undoRedoAndANewStrokeDropsRedo() {
        var h = History().add(stroke()).add(stroke())
        h = h.undo()
        assertThat(h.visible).hasSize(1)
        assertThat(h.canRedo).isTrue()
        h = h.redo()
        assertThat(h.visible).hasSize(2)
        h = h.undo().add(stroke())
        assertThat(h.canRedo).isFalse()
    }

    @Test fun clearIsOneUndoableAction() {
        var h = History().add(stroke())
        h = h.clear(backdrop = false)
        assertThat(h.visible).isEmpty()
        assertThat(h.clear(backdrop = false)).isEqualTo(h)
        h = h.undo()
        assertThat(h.visible).hasSize(1)
    }

    @Test fun editOverABackdrop() {
        var h = History()
        assertThat(h.isUnchanged).isTrue()
        assertThat(h.canClear(backdrop = true)).isTrue()
        h = h.add(stroke(erase = true))
        assertThat(h.isUnchanged).isFalse()
        h = History().clear(backdrop = true)
        assertThat(h.cleared).isTrue()
        assertThat(h.isUnchanged).isTrue()
    }

    @Test fun pointsCloserThanAStepAreDropped() {
        val s = Stroke(0, 6f, false)
        assertThat(s.extend(0f, 0f)).isTrue()
        assertThat(s.extend(1f, 0f)).isFalse()
        assertThat(s.extend(2f, 0f)).isTrue()
    }

    @Test fun toCanvasScalesAndClamps() {
        val p = toCanvas(50f, 200f, 100f, 100f, Surfaces.BOARD)
        assertThat(p[0]).isEqualTo(512f)
        assertThat(p[1]).isEqualTo(768f)
    }
}
