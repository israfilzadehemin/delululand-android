package com.delulutopia.core.ui.doodle

import com.delulutopia.core.i18n.t
import kotlin.math.hypot
import kotlin.math.roundToInt

/*
 * Doodles (web doodle/doodle.ts, doc "Delululand — Doodles"): the drawing model behind the pad, free
 * of Compose so it can be tested. A doodle is a list of actions (strokes and clears); undo and redo
 * move actions between two stacks; what the pad shows and exports is the strokes after the last
 * clear, drawn in order on the paper (D4).
 */

/** What a pad draws on: its stored size in pixels and the paper baked under the ink (ARGB). */
data class Surface(val width: Int, val height: Int, val paper: Int)

object Surfaces {
    /** D3, D4: a 1024 px paper square in `--dl-paper`, so a doodle looks the same in both themes. */
    val LETTER = Surface(1024, 1024, 0xFFF5F0E8.toInt())

    /** Board B5, B6: 1024 × 768 (4:3) on the whiteboard. */
    val BOARD = Surface(1024, 768, 0xFFFBFBF8.toInt())
}

/** Q4 Standard: 8 palette colours. */
val COLOURS: List<Pair<String, Int>> get() = listOf(
    t("doodle.colour.ink") to 0xFF33323B.toInt(),
    t("doodle.colour.navy") to 0xFF202B45.toInt(),
    t("doodle.colour.terracotta") to 0xFFC96F52.toInt(),
    t("doodle.colour.red") to 0xFFB23A3A.toInt(),
    t("doodle.colour.mustard") to 0xFFD4A24C.toInt(),
    t("doodle.colour.sage") to 0xFF5F8A7D.toInt(),
    t("doodle.colour.sky") to 0xFF5B8FC7.toInt(),
    t("doodle.colour.lavender") to 0xFF8A7BC0.toInt(),
)

/** Q4 Standard: 3 brush sizes, in canvas pixels. */
val SIZES: List<Pair<String, Float>> get() = listOf(t("doodle.size.fine") to 6f, t("doodle.size.medium") to 14f, t("doodle.size.bold") to 32f)

/** The eraser is wider than the brush it follows, as on paper. */
const val ERASER_FACTOR = 2.5f

/** A point closer than this to the last one adds nothing but work (canvas pixels). */
private const val MIN_STEP = 1.5f

class Stroke(val colour: Int, val width: Float, val erase: Boolean, val points: MutableList<FloatArray> = mutableListOf()) {
    /** Appends a point unless it is too close to the previous one; returns whether it was kept. */
    fun extend(x: Float, y: Float): Boolean {
        val last = points.lastOrNull()
        if (last != null && hypot(x - last[0], y - last[1]) < MIN_STEP) return false
        points += floatArrayOf(x, y)
        return true
    }
}

sealed interface Action {
    class Draw(val stroke: Stroke) : Action

    data object Clear : Action
}

data class History(val done: List<Action> = emptyList(), val undone: List<Action> = emptyList()) {
    /** The strokes on the pad now: everything after the last clear. */
    val visible: List<Stroke>
        get() {
            val out = ArrayList<Stroke>()
            for (a in done) if (a is Action.Draw) out += a.stroke else out.clear()
            return out
        }

    val cleared: Boolean get() = done.any { it is Action.Clear }

    /** D6: nothing drawn, or only eraser strokes. */
    val isBlank: Boolean get() = visible.none { !it.erase }

    /** Board edit §E: over a backdrop the pad can post once anything changed it; once wiped it is a blank pad again. */
    val isUnchanged: Boolean get() = if (cleared) isBlank else visible.isEmpty()

    val canUndo get() = done.isNotEmpty()
    val canRedo get() = undone.isNotEmpty()

    fun canClear(backdrop: Boolean) = visible.isNotEmpty() || (backdrop && !cleared)

    fun add(stroke: Stroke) = History(done + Action.Draw(stroke), emptyList())

    fun clear(backdrop: Boolean) = if (!canClear(backdrop)) this else History(done + Action.Clear, emptyList())

    fun undo() = if (!canUndo) this else History(done.dropLast(1), undone + done.last())

    fun redo() = if (!canRedo) this else History(done + undone.last(), undone.dropLast(1))
}

/** A position in the drawn box (px) as canvas pixels, clamped to the surface. */
fun toCanvas(x: Float, y: Float, boxWidth: Float, boxHeight: Float, surface: Surface): FloatArray {
    fun clamp(v: Float, max: Int) = (v.coerceIn(0f, max.toFloat()) * 10).roundToInt() / 10f
    return floatArrayOf(
        clamp(x / boxWidth.coerceAtLeast(1f) * surface.width, surface.width),
        clamp(y / boxHeight.coerceAtLeast(1f) * surface.height, surface.height),
    )
}
