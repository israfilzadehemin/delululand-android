package com.delulutopia.core.ui.doodle

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import java.io.ByteArrayOutputStream

private val inkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    strokeCap = Paint.Cap.ROUND
    strokeJoin = Paint.Join.ROUND
}

/**
 * One stroke, smoothed: a quadratic curve through the midpoints of successive points. A tap is a
 * dot. The eraser paints paper (the paper is part of the image, so erasing to it is exact).
 */
fun drawStroke(canvas: Canvas, s: Stroke, paper: Int) {
    val colour = if (s.erase) paper else s.colour
    val pts = s.points
    if (pts.isEmpty()) return
    if (pts.size == 1) {
        inkPaint.style = Paint.Style.FILL
        inkPaint.color = colour
        canvas.drawCircle(pts[0][0], pts[0][1], s.width / 2, inkPaint)
        return
    }
    inkPaint.style = Paint.Style.STROKE
    inkPaint.color = colour
    inkPaint.strokeWidth = s.width
    val path = Path()
    path.moveTo(pts[0][0], pts[0][1])
    for (i in 1 until pts.size - 1) {
        val p = pts[i]
        val q = pts[i + 1]
        path.quadTo(p[0], p[1], (p[0] + q[0]) / 2, (p[1] + q[1]) / 2)
    }
    path.lineTo(pts.last()[0], pts.last()[1])
    canvas.drawPath(path, inkPaint)
}

/** Paper, then the backdrop unless a clear wiped it (the doodle being edited), then every visible stroke. */
fun render(canvas: Canvas, history: History, surface: Surface, backdrop: Bitmap?) {
    canvas.drawColor(surface.paper)
    if (backdrop != null && !history.cleared) {
        canvas.drawBitmap(backdrop, null, RectF(0f, 0f, surface.width.toFloat(), surface.height.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    }
    for (s in history.visible) drawStroke(canvas, s, surface.paper)
}

/** The PNG Done exports. */
fun exportPng(history: History, surface: Surface, backdrop: Bitmap?): ByteArray {
    val bitmap = Bitmap.createBitmap(surface.width, surface.height, Bitmap.Config.ARGB_8888)
    render(Canvas(bitmap), history, surface, backdrop)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    bitmap.recycle()
    return out.toByteArray()
}
