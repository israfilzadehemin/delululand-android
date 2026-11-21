package com.delulutopia.feature.emails.stickers

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.io.ByteArrayOutputStream
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.suspendCancellableCoroutine

/** FS-SK19: the white die-cut edge, in pixels of a 1024 px drawing (web files.ts OUTLINE_PX). */
internal const val OUTLINE_PX = 16

/**
 * FS-SK19: a white edge around the shape — the shape filled white and stamped at 16 points on a
 * circle of [px], then the picture on top (web files.ts withOutline).
 */
internal fun withOutline(png: ByteArray, px: Int): ByteArray {
    val src = BitmapFactory.decodeByteArray(png, 0, png.size) ?: return png
    val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    val white = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
    }
    for (i in 0 until 16) {
        val a = i / 16.0 * 2 * PI
        canvas.drawBitmap(src, (cos(a) * px).toFloat(), (sin(a) * px).toFloat(), white)
    }
    canvas.drawBitmap(src, 0f, 0f, null)
    return out.toPng()
}

/**
 * FS-SK20: the subject of a photo cut out on-device (ML Kit, all subjects together), cropped to what
 * is left with room for the outline. Null when nothing stands out.
 */
internal suspend fun cutOut(photo: Bitmap): ByteArray? {
    val options = SubjectSegmenterOptions.Builder().enableForegroundBitmap().build()
    val segmenter = SubjectSegmentation.getClient(options)
    val fg: Bitmap? = try {
        suspendCancellableCoroutine { cont ->
            segmenter.process(InputImage.fromBitmap(photo, 0))
                .addOnSuccessListener { cont.resume(it.foregroundBitmap) }
                .addOnFailureListener { cont.resume(null) }
        }
    } finally {
        segmenter.close()
    }
    return fg?.let { cropToSubject(it) }?.toPng()
}

/** The smallest box around the opaque pixels, plus a margin the outline can grow into. */
private fun cropToSubject(b: Bitmap): Bitmap? {
    val w = b.width
    val h = b.height
    val px = IntArray(w * h)
    b.getPixels(px, 0, w, 0, 0, w, h)
    var left = w
    var top = h
    var right = -1
    var bottom = -1
    for (y in 0 until h) {
        for (x in 0 until w) {
            if ((px[y * w + x] ushr 24) > 16) {
                if (x < left) left = x
                if (x > right) right = x
                if (y < top) top = y
                if (y > bottom) bottom = y
            }
        }
    }
    if (right < 0) return null
    val cw = right - left + 1
    val ch = bottom - top + 1
    val margin = (max(cw, ch) * 2.5f * OUTLINE_PX / 1024f).roundToInt() + 2
    val out = Bitmap.createBitmap(cw + margin * 2, ch + margin * 2, Bitmap.Config.ARGB_8888)
    Canvas(out).drawBitmap(Bitmap.createBitmap(b, left, top, cw, ch), margin.toFloat(), margin.toFloat(), null)
    // The server keeps 512 px; a photo's cut-out as a larger PNG could pass the 1 MB limit (SK15).
    val edge = max(out.width, out.height)
    if (edge <= CUT_EDGE) return out
    val k = CUT_EDGE.toFloat() / edge
    return Bitmap.createScaledBitmap(out, (out.width * k).roundToInt().coerceAtLeast(1), (out.height * k).roundToInt().coerceAtLeast(1), true)
}

private const val CUT_EDGE = 640

/** The outline width for a picture of this size, matching 16 px on the 1024 px square. */
internal fun outlinePx(png: ByteArray): Int {
    val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(png, 0, png.size, o)
    return max(2, (max(o.outWidth, o.outHeight) * OUTLINE_PX / 1024f).roundToInt())
}

private fun Bitmap.toPng(): ByteArray = ByteArrayOutputStream().also { compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
