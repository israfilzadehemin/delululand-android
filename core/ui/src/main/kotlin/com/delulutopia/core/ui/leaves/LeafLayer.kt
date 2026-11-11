package com.delulutopia.core.ui.leaves

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotateRad
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.delulutopia.core.designsystem.theme.DlTheme
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * What leaves can land on (§5): cards register themselves with [leafSurface]; the top bar is a
 * ceiling ([leafCeiling]) and the navigation bar the ground. Positions are in window px.
 */
class LeafSurfaces {
    internal val items = LinkedHashMap<Any, LayoutCoordinates>()
    internal val meta = HashMap<Any, Pair<String?, SurfaceKind>>()
    internal var ceiling: LayoutCoordinates? = null
}

val LocalLeafSurfaces = staticCompositionLocalOf<LeafSurfaces?> { null }

/** A card leaves can land on ([id] keeps resting leaves across a remount), or the ground or a round button. */
fun Modifier.leafSurface(id: String? = null, kind: SurfaceKind = SurfaceKind.Flat): Modifier = composed {
    val registry = LocalLeafSurfaces.current ?: return@composed this
    val key = remember { Any() }
    DisposableEffect(registry) {
        onDispose {
            registry.items.remove(key)
            registry.meta.remove(key)
        }
    }
    onGloballyPositioned {
        registry.items[key] = it
        registry.meta[key] = id to kind
    }
}

/** The pinned top bar: cards under it are out of the band leaves may lie in. */
fun Modifier.leafCeiling(): Modifier = composed {
    val registry = LocalLeafSurfaces.current ?: return@composed this
    DisposableEffect(registry) { onDispose { registry.ceiling = null } }
    onGloballyPositioned { registry.ceiling = it }
}

/**
 * Falling leaves over the page (doc "Delululand — Falling Leaves"): decoration only, drawn over the
 * content and bars, under every sheet, dialog and lightbox (those are their own windows); taps pass
 * through. Off when Seasonal effects is off or the phone removes animations; paused (state kept)
 * while [active] is false, i.e. outside Home and the three lists.
 */
@Composable
fun LeafLayer(registry: LeafSurfaces, enabled: Boolean, active: Boolean) {
    val context = LocalContext.current
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    if (!enabled || reduced) return
    val unit = LocalDensity.current.density
    val sim = remember { LeafSim(unit) }
    var frame by remember { mutableLongStateOf(0L) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var band by remember { mutableStateOf(0f to 0f) }
    val dark = DlTheme.isDark
    val sprites = remember(dark) { spritesFor(dark) }

    LaunchedEffect(active, size) {
        if (!active || size.width == 0) return@LaunchedEffect
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else min(0.05f, max(0f, (now - last) / 1e9f))
                last = now
                val (view, surfaces) = read(registry, origin, size)
                band = view.clipTop to view.clipBottom
                if (!sim.started) sim.start(view)
                sim.step(dt, view, surfaces)
                frame = now
            }
        }
    }

    Canvas(
        Modifier.fillMaxSize().onGloballyPositioned {
            origin = it.positionInWindow()
            size = it.size
        },
    ) {
        if (!active) return@Canvas
        frame.hashCode()
        val shadowAlpha = if (dark) 0.35f else 0.16f
        fun draw(l: Leaf, shadow: Boolean) {
            val fx = cos(l.flip)
            val sx = max(0.12f, abs(fx))
            val set = sprites[l.sp]
            val s = l.size
            val img = if (shadow) set.silhouette else set.faces[l.col][if (fx < 0) 1 else 0]
            val d = if (shadow) Offset(1.2f * unit, 1.6f * unit) else Offset.Zero
            translate(l.x + d.x, l.y + d.y) {
                rotateRad(l.rot, Offset.Zero) {
                    scale(sx, 1f, Offset.Zero) {
                        drawImage(
                            img,
                            dstOffset = IntOffset((-s / 2).roundToInt(), (-s / 2).roundToInt()),
                            dstSize = IntSize(s.roundToInt(), s.roundToInt()),
                            alpha = (max(0f, l.alpha) * if (shadow) shadowAlpha else 1f).coerceIn(0f, 1f),
                        )
                    }
                }
            }
        }

        /** A leaf lying on something carries a faint contact shadow. */
        fun one(l: Leaf, shadow: Boolean) {
            if (shadow) draw(l, true)
            draw(l, false)
        }
        // Leaves on cards are clipped to the band between the bars; on the bar and in the air, over everything.
        clipRect(0f, band.first, size.width.toFloat(), max(band.first, band.second)) {
            for (l in sim.leaves) if (l.state != LeafState.Fall && l.clipped) one(l, true)
        }
        for (l in sim.leaves) if (l.state != LeafState.Fall && !l.clipped) one(l, true)
        for (l in sim.leaves) if (l.state == LeafState.Fall) one(l, false)
    }
}

/** §5, read once per frame: the band between the bars and every registered surface, in layer px. */
private fun read(registry: LeafSurfaces, origin: Offset, size: IntSize): Pair<LeafView, List<Surface>> {
    val w = size.width.toFloat()
    val h = size.height.toFloat()
    var clipTop = 0f
    var clipBottom = h
    registry.ceiling?.takeIf { it.isAttached }?.let { clipTop = max(clipTop, it.boundsInWindow().bottom - origin.y) }
    val out = mutableListOf<Surface>()
    for ((key, c) in registry.items.entries.toList()) {
        if (!c.isAttached) continue
        val (id, kind) = registry.meta[key] ?: continue
        val r = c.boundsInWindow().translate(-origin.x, -origin.y)
        val full = c.size
        if (r.width < 8 || full.width < 8) continue
        when (kind) {
            SurfaceKind.Ground -> {
                clipBottom = min(clipBottom, r.top)
                out += Surface(key, "ground", SurfaceKind.Ground, 0f, w, r.top, r.bottom)
            }
            SurfaceKind.Round -> out += Surface(key, null, SurfaceKind.Round, r.left, r.right, r.top, r.bottom, r.center.x, r.center.y, r.width / 2)
            SurfaceKind.Flat -> {
                // Top edge as laid out (a card partly under the bar keeps its true top).
                val top = c.positionInWindow().y - origin.y
                if (top < clipTop - h || top > clipBottom + h) continue
                out += Surface(key, id, SurfaceKind.Flat, r.left, r.right, top, top + full.height)
            }
        }
    }
    return LeafView(w, h, clipTop, clipBottom) to out
}
