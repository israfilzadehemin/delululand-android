package com.delulutopia.core.ui.leaves

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Leaf artwork (doc "Delululand — Falling Leaves" §7; web sprites.ts): maple, oak, birch and linden
 * as flat fills with a darker midrib, side veins and stem, painted once per theme as 96 px sprites
 * (front, back) plus a silhouette for the contact shadow. Leaves point up, stem down, centred.
 */
private val LIGHT = listOf(0xD99A82, 0xC46A3C, 0xD8A23E, 0xA9472F, 0x9C9A55)
private const val PAPER_LIGHT = 0xF5F0E8
private const val INK_LIGHT = 0x33323B
private const val PAPER_DARK = 0x16181F
private const val INK_DARK = 0x0B0D12
private const val SPRITE_PX = 96

class SpeciesSprites(val faces: List<List<ImageBitmap>>, val silhouette: ImageBitmap)

private fun mix(a: Int, b: Int, t: Float): Int {
    fun ch(v: Int, s: Int) = (v shr s) and 0xFF
    fun m(s: Int) = (ch(a, s) + (ch(b, s) - ch(a, s)) * t).roundToInt()
    return (0xFF shl 24) or (m(16) shl 16) or (m(8) shl 8) or m(0)
}

private fun opaque(c: Int) = (0xFF shl 24) or c

/** The outline in a 100 × 100 box. */
private fun outline(sp: Int): List<Pair<Float, Float>> {
    if (sp == 0) {
        val right = listOf(
            50f to 4f, 56f to 21f, 67f to 12f, 65f to 31f, 85f to 20f, 78f to 40f, 97f to 44f, 81f to 55f, 89f to 66f, 67f to 63f, 59f to 76f,
            51f to 72f,
        )
        return right + right.reversed().map { (x, y) -> (100 - x) to y }
    }
    val pi = PI.toFloat()
    val env: (Float) -> Float = when (sp) {
        1 -> { s -> 25 * sin(pi * minOf(1f, s * 1.02f)).coerceAtLeast(0f).pow(0.75f) * (0.8f + 0.25f * s) }
        2 -> { s -> 23 * sin(pi * s.pow(0.72f)).coerceAtLeast(0f).pow(0.95f) }
        else -> { s -> 32 * sin(pi * (0.14f + 0.86f * s)).coerceAtLeast(0f).pow(0.85f) }
    }
    val mod: (Float) -> Float = when (sp) {
        1 -> { s -> 0.34f * abs(sin(s * pi * 4.5f)) }
        2 -> { s -> 0.07f * sin(s * pi * 34) }
        else -> { s -> 0.035f * sin(s * pi * 26) }
    }
    val n = 64
    val base = 80f
    val tip = 6f
    val pts = mutableListOf<Pair<Float, Float>>()
    for (i in 0..n) {
        val s = i.toFloat() / n
        pts += (50 + env(s) * (1 + mod(s))) to (base - s * (base - tip))
    }
    for (i in n downTo 0) {
        val s = i.toFloat() / n
        pts += (50 - env(s) * (1 + mod(s))) to (base - s * (base - tip))
    }
    return pts
}

private fun veins(sp: Int): List<FloatArray> {
    if (sp ==
        0
    ) {
        return listOf(
            floatArrayOf(50f, 72f, 50f, 12f),
            floatArrayOf(50f, 60f, 82f, 27f),
            floatArrayOf(50f, 60f, 18f, 27f),
            floatArrayOf(50f, 64f, 88f, 52f),
            floatArrayOf(50f, 64f, 12f, 52f),
        )
    }
    val v = mutableListOf(floatArrayOf(50f, 80f, 50f, 10f))
    for (k in 0 until 4) {
        val y = 70f - k * 14
        val w = if (sp == 3) 22f - k * 3 else 16f - k * 2
        v += floatArrayOf(50f, y, 50 + w, y - 9)
        v += floatArrayOf(50f, y, 50 - w, y - 9)
    }
    return v
}

private fun paint(sp: Int, fill: Int, vein: Int?): ImageBitmap {
    val bmp = Bitmap.createBitmap(SPRITE_PX, SPRITE_PX, Bitmap.Config.ARGB_8888)
    val g = Canvas(bmp)
    g.scale(SPRITE_PX / 100f, SPRITE_PX / 100f)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    stroke.color = vein ?: fill
    stroke.strokeWidth = 3.2f
    g.drawPath(
        Path().apply {
            moveTo(50f, 70f)
            quadTo(51f, 86f, 48f, 97f)
        },
        stroke,
    )
    val shape = Path()
    outline(sp).forEachIndexed { i, (x, y) -> if (i == 0) shape.moveTo(x, y) else shape.lineTo(x, y) }
    shape.close()
    g.drawPath(shape, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
    stroke.color = fill
    stroke.strokeWidth = 1.6f
    g.drawPath(shape, stroke)
    if (vein != null) {
        stroke.color = vein
        stroke.strokeWidth = 2f
        for (v in veins(sp)) g.drawLine(v[0], v[1], v[2], v[3], stroke)
    }
    return bmp.asImageBitmap()
}

private val cache = mutableMapOf<Boolean, List<SpeciesSprites>>()

/** The sprites for the theme, painted on first use. */
fun spritesFor(dark: Boolean): List<SpeciesSprites> = cache.getOrPut(dark) {
    SPECIES.indices.map { sp ->
        SpeciesSprites(
            faces = LIGHT.take(COLOURS).map { base ->
                // §7 dark: each colour 24% toward the dark paper so it sits on Dusk without glare.
                val front = if (!dark) opaque(base) else mix(base, PAPER_DARK, 0.24f)
                val back = if (!dark) mix(base, PAPER_LIGHT, 0.28f) else mix(base, PAPER_DARK, 0.08f)
                val vein = if (!dark) mix(base, INK_LIGHT, 0.38f) else mix(base, INK_DARK, 0.45f)
                val veinBack = if (!dark) mix(base, INK_LIGHT, 0.22f) else mix(base, INK_DARK, 0.3f)
                listOf(paint(sp, front, vein), paint(sp, back, veinBack))
            },
            silhouette = paint(sp, opaque(0), null),
        )
    }
}
