package com.delulutopia.feature.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlin.math.pow
import kotlin.math.sin

/**
 * Selfies DS2: cork and wood drawn once per theme from a fixed seed, so they read as material
 * (dense granules in many tones, pores, soft mottling; wood grain with knots) instead of flat
 * colour. The same recipe, seeds and generator as iOS BoardTextures, so both draw the same board.
 */
internal object BoardTextures {
    private const val SCALE = 2f
    private val cache = HashMap<String, ImageBitmap>()

    fun cork(dark: Boolean): ImageBitmap = cached("cork$dark") { renderCork(dark) }

    fun wood(dark: Boolean): ImageBitmap = cached("wood$dark") { renderWood(dark) }

    @Synchronized
    private fun cached(key: String, make: () -> Bitmap): ImageBitmap = cache.getOrPut(key) { make().asImageBitmap() }

    private class Rgb(val r: Float, val g: Float, val b: Float) {
        fun mix(o: Rgb, t: Float) = Rgb(r + (o.r - r) * t, g + (o.g - g) * t, b + (o.b - b) * t)

        fun argb(alpha: Float): Int = android.graphics.Color.argb(
            (alpha.coerceIn(0f, 1f) * 255).toInt(),
            (r.coerceIn(0f, 1f) * 255).toInt(),
            (g.coerceIn(0f, 1f) * 255).toInt(),
            (b.coerceIn(0f, 1f) * 255).toInt(),
        )
    }

    private fun canvas(w: Int, h: Int): Pair<Bitmap, Canvas> {
        val bitmap = Bitmap.createBitmap((w * SCALE).toInt(), (h * SCALE).toInt(), Bitmap.Config.ARGB_8888)
        return bitmap to Canvas(bitmap).apply { scale(SCALE, SCALE) }
    }

    private fun renderCork(dark: Boolean): Bitmap {
        val w = 420
        val h = 250
        val shade = if (dark) 0.72f else 1f
        val deep = Rgb(0.52f * shade, 0.36f * shade, 0.22f * shade)
        val mid = Rgb(0.74f * shade, 0.57f * shade, 0.39f * shade)
        val light = Rgb(0.90f * shade, 0.76f * shade, 0.57f * shade)
        val (bitmap, cg) = canvas(w, h)
        val rng = SeededRandom(41)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val oval = RectF()
        cg.drawColor(mid.argb(1f))
        // Soft mottling: the pressed cork is never one tone.
        repeat(70) {
            val r = 14 + 40 * rng.next()
            paint.color = (if (rng.next() < 0.5f) deep else light).argb(0.10f)
            val x = rng.next() * w - r
            val y = rng.next() * h - r
            oval.set(x, y, x + 2 * r, y + 1.6f * r)
            cg.drawOval(oval, paint)
        }
        // Granules: thousands of irregular crumbs, each its own tone.
        repeat(14_000) {
            val gw = 0.6f + 2.4f * rng.next().pow(1.6f)
            val gh = gw * (0.45f + 0.7f * rng.next())
            val t = rng.next()
            val c = if (t < 0.5f) deep.mix(mid, t * 2) else mid.mix(light, (t - 0.5f) * 2)
            cg.save()
            cg.translate(rng.next() * w, rng.next() * h)
            cg.rotate(rng.next() * 180f)
            paint.color = c.argb(0.35f + 0.5f * rng.next())
            oval.set(-gw / 2, -gh / 2, gw / 2, gh / 2)
            cg.drawOval(oval, paint)
            cg.restore()
        }
        // Pores and gaps between crumbs.
        val pore = Rgb(0.24f * shade, 0.15f * shade, 0.08f * shade)
        repeat(3_200) {
            val r = 0.25f + 0.6f * rng.next()
            paint.color = pore.argb(0.25f + 0.4f * rng.next())
            val x = rng.next() * w
            val y = rng.next() * h
            oval.set(x, y, x + 2 * r, y + 2 * r)
            cg.drawOval(oval, paint)
        }
        // A few bright flecks catching light.
        val fleck = Rgb(1f, 0.93f, 0.80f)
        repeat(700) {
            val r = 0.2f + 0.4f * rng.next()
            paint.color = fleck.argb(0.18f + 0.25f * rng.next())
            val x = rng.next() * w
            val y = rng.next() * h
            oval.set(x, y, x + 2 * r, y + 2 * r)
            cg.drawOval(oval, paint)
        }
        return bitmap
    }

    private fun renderWood(dark: Boolean): Bitmap {
        val w = 440
        val h = 280
        val shade = if (dark) 0.62f else 1f
        val base = Rgb(0.70f * shade, 0.51f * shade, 0.33f * shade)
        val grain = Rgb(0.50f * shade, 0.33f * shade, 0.19f * shade)
        val light = Rgb(0.82f * shade, 0.64f * shade, 0.44f * shade)
        val (bitmap, cg) = canvas(w, h)
        val rng = SeededRandom(23)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
        cg.drawColor(base.argb(1f))
        // Long, slightly wavy grain lines in bands of tone.
        var y = 0f
        val path = Path()
        while (y < h) {
            val c = if (rng.next() < 0.55f) grain else light
            val width = 0.4f + 1.6f * rng.next()
            val amp = 0.6f + 2.2f * rng.next()
            val freq = 0.004f + 0.012f * rng.next()
            val phase = rng.next() * 6.28f
            path.reset()
            path.moveTo(0f, y)
            var x = 0f
            while (x <= w) {
                path.lineTo(x, y + amp * sin(x * freq * 6.28f + phase))
                x += 6f
            }
            paint.color = c.argb(0.18f + 0.35f * rng.next())
            paint.strokeWidth = width
            cg.drawPath(path, paint)
            y += 0.8f + 2.6f * rng.next()
        }
        // Two small knots.
        val oval = RectF()
        repeat(2) {
            val cx = rng.next() * w
            val cy = rng.next() * h
            for (ring in 0 until 5) {
                val r = 2f + ring * 2f
                paint.color = grain.argb(0.35f - ring * 0.05f)
                paint.strokeWidth = 0.8f
                oval.set(cx - r * 2.2f, cy - r, cx + r * 2.2f, cy + r)
                cg.drawOval(oval, paint)
            }
        }
        return bitmap
    }

    /** iOS SeededRandom: a 64-bit LCG, the top 53 bits as a 0..1 fraction. */
    private class SeededRandom(seed: Long) {
        private var state = seed * MUL + 1

        fun next(): Float {
            state = state * MUL + INC
            return ((state ushr 11).toDouble() / TWO_53).toFloat()
        }

        private companion object {
            const val MUL = 6_364_136_223_846_793_005L
            const val INC = 1_442_695_040_888_963_407L
            val TWO_53 = 2.0.pow(53)
        }
    }
}
