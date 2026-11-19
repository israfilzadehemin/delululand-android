package com.delulutopia.core.ui

import android.os.SystemClock
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.delulutopia.core.designsystem.motion.LocalPageLanding
import com.delulutopia.core.designsystem.motion.Motion
import kotlin.math.abs

/**
 * How the app frames a destination: a tab root gets its top bar and room for the navigation bar,
 * a deeper page is an opaque full-screen sheet on the stack (it covers the bars as it slides in).
 * The shell provides it; without one a page is drawn bare (tests, previews).
 */
val LocalPageFrame = staticCompositionLocalOf<@Composable (NavBackStackEntry, @Composable () -> Unit) -> Unit> { { _, content -> content() } }

/**
 * A destination drawn in [LocalPageFrame], used for every screen instead of `composable<T>`. While
 * it slides in, [LocalPageLanding] says when it will land, so its own enter animations wait for it.
 */
inline fun <reified T : Any> NavGraphBuilder.page(noinline content: @Composable (NavBackStackEntry) -> Unit) {
    composable<T> { entry ->
        val arriving = transition.targetState == EnterExitState.Visible && transition.currentState != EnterExitState.Visible
        // Not sliding itself: it may still be arriving with the whole app (the unlock zoom).
        val outer = LocalPageLanding.current
        val landing = remember { if (arriving) SystemClock.uptimeMillis() + Motion.STACK_MS else outer }
        CompositionLocalProvider(LocalPageLanding provides landing) {
            LocalPageFrame.current(entry) { content(entry) }
        }
    }
}

/**
 * Page motion (doc "Delululand — Motion", Android pages). A stack: a deeper page slides in over the
 * current one from the reading-direction end (right; left in Persian) and back out on Back, the page
 * under it staying put and darkened by the one on top ([stackShadow]); predictive back drags the same
 * slide with the finger. Tabs: the pages slide side by side toward the picked tab. [rtl] mirrors both.
 */
object PageMotion {
    private fun side(rtl: Boolean) = if (rtl) -1 else 1

    /** Pushed: from the far edge, decelerating onto the stack. */
    fun push(rtl: Boolean): EnterTransition = slideInHorizontally(tween(Motion.STACK_MS, easing = Motion.EmphasizedDecelerate)) { side(rtl) * it }

    /** Covered: stays where it is under the new page (kept alive for the slide; the cover darkens it). */
    fun cover(): ExitTransition = fadeOut(tween(Motion.STACK_MS, easing = LinearEasing), targetAlpha = 0.999f)

    /** Back: the top page slides off to the far edge. */
    fun pop(rtl: Boolean): ExitTransition = slideOutHorizontally(tween(Motion.STACK_MS, easing = Motion.Emphasized)) { side(rtl) * it }

    /** Uncovered: the page underneath, already in place. */
    fun uncover(): EnterTransition = fadeIn(tween(Motion.STACK_MS, easing = LinearEasing), initialAlpha = 0.999f)

    /** A tab to the end side of the current one ([forward]) comes in from that side; the current one leaves the other way. */
    fun tabIn(forward: Boolean, rtl: Boolean): EnterTransition =
        slideInHorizontally(tween(Motion.TAB_MS, easing = Motion.Emphasized)) { (if (forward) 1 else -1) * side(rtl) * it }

    fun tabOut(forward: Boolean, rtl: Boolean): ExitTransition =
        slideOutHorizontally(tween(Motion.TAB_MS, easing = Motion.Emphasized)) { -(if (forward) 1 else -1) * side(rtl) * it }
}

/**
 * The shadow a page on the stack casts while it slides: the uncovered part of the page below darkens
 * as this one covers it (up to 22 %), with a soft 16 dp edge. Nothing is drawn while it is at rest.
 */
fun Modifier.stackShadow(): Modifier = composed {
    var dx by remember { mutableFloatStateOf(0f) }
    onGloballyPositioned { dx = it.positionInRoot().x }
        .drawBehind {
            val w = size.width
            val gap = abs(dx)
            if (gap < 0.5f || w <= 0f) return@drawBehind
            val k = (1f - gap / w).coerceIn(0f, 1f)
            val edge = 16.dp.toPx()
            // The uncovered strip lies on the side the page came from: left of it when it moved right.
            val left = if (dx > 0) -gap else w
            drawRect(Color.Black.copy(alpha = 0.22f * k), topLeft = Offset(left, 0f), size = Size(gap, size.height))
            val (from, to) = if (dx > 0) -edge to 0f else w + edge to w
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.16f)), startX = from, endX = to),
                topLeft = Offset(minOf(from, to), 0f),
                size = Size(edge, size.height),
            )
        }
}

/** A page on the stack: opaque, edge to edge, casting [stackShadow]; [insets] pads it inside the system bars (the shell's pages). */
@Composable
fun StackPage(insets: Boolean = true, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .stackShadow()
            .background(MaterialTheme.colorScheme.surface)
            .then(if (insets) Modifier.windowInsetsPadding(ScaffoldDefaults.contentWindowInsets) else Modifier),
    ) { content() }
}
