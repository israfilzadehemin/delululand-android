package com.delulutopia.core.designsystem.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay

/**
 * Motion (web src/styles/motion.css; UX §7.4 "the whole interface moves"). Enter animations play
 * once when an element first appears, presses scale, a few marks loop. Only alpha and transform
 * move. With the phone's animations removed (Settings › Accessibility › Remove animations, i.e.
 * an animator duration scale of 0) every element is simply shown: the static design.
 */
object Motion {
    val EaseOut: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val EaseIn: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val Spring: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
    const val PRESS_MS = 120
    const val QUICK_MS = 160
    const val ENTER_MS = 280
    const val PAGE_MS = 300

    /** The web's list stagger: 40 ms a row, the ninth and later all at 320 ms. */
    fun stagger(index: Int, stepMs: Int = 40, maxMs: Int = 320): Int = (index * stepMs).coerceAtMost(maxMs)
}

/** False when the phone removes animations; tests can provide false for static frames. */
val LocalMotion = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun motionOn(): Boolean {
    LocalMotion.current?.let { return it }
    // The JVM parity screenshots ask for static frames.
    if (System.getProperty("dl.motion") == "off") return false
    val context = LocalContext.current
    return remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
}

/** The web's enter keyframes. */
enum class Enter {
    /** dl-fade-in: opacity only. */
    Fade,

    /** dl-rise: from 10 dp below. */
    Rise,

    /** dl-drop-in: from 8 dp above. */
    Drop,

    /** dl-pop-in: from 8 dp below at 96 %. */
    PopIn,

    /** dl-pop: 50 % → 112 % → 100 %, for chips and badges. */
    Pop,

    /** dl-zoom-in: from 94 %. */
    Zoom,

    /** dl-menu-in: a menu from its control, 92 %. */
    Menu,

    /** dl-paper-in: a letter sheet settling, from 18 dp below, tilted. */
    Paper,

    /** dl-stamp: a postmark struck at 170 %, rotated. */
    Stamp,
}

/**
 * Plays [kind] once, [delayMs] after this element first appears. [key] replays it when it changes
 * (the web remounts an element whose kind changed: an arrival "lands" again).
 */
fun Modifier.enter(
    kind: Enter = Enter.Rise,
    delayMs: Int = 0,
    durationMs: Int = when (kind) {
        Enter.Fade -> 240
        Enter.Pop -> 460
        Enter.Paper -> 480
        Enter.Stamp -> 560
        Enter.Menu -> Motion.QUICK_MS
        Enter.Zoom -> 300
        else -> 320
    },
    easing: Easing = Motion.EaseOut,
    key: Any? = Unit,
): Modifier = composed {
    if (!motionOn()) return@composed this
    val t = remember(key) { Animatable(0f) }
    LaunchedEffect(key) {
        if (delayMs > 0) delay(delayMs.toLong())
        t.animateTo(1f, tween(durationMs, easing = if (kind == Enter.Fade) LinearEasing else easing))
    }
    val dp = LocalDensity.current.density
    graphicsLayer {
        val p = t.value
        val q = 1f - p
        when (kind) {
            Enter.Fade -> alpha = p
            Enter.Rise -> {
                alpha = p
                translationY = 10 * dp * q
            }
            Enter.Drop -> {
                alpha = p
                translationY = -8 * dp * q
            }
            Enter.PopIn -> {
                alpha = p
                translationY = 8 * dp * q
                scaleX = 1f - 0.04f * q
                scaleY = scaleX
            }
            Enter.Pop -> {
                // 0 → 60 %: 0.5 → 1.12 and fading in; 60 → 100 %: back to 1.
                val s = if (p < 0.6f) 0.5f + (1.12f - 0.5f) * (p / 0.6f) else 1.12f - 0.12f * ((p - 0.6f) / 0.4f)
                alpha = (p / 0.6f).coerceAtMost(1f)
                scaleX = s
                scaleY = s
            }
            Enter.Zoom -> {
                alpha = p
                scaleX = 1f - 0.06f * q
                scaleY = scaleX
            }
            Enter.Menu -> {
                alpha = p
                scaleX = 1f - 0.08f * q
                scaleY = scaleX
            }
            Enter.Paper -> {
                alpha = p
                translationY = 18 * dp * q
                rotationZ = -0.6f * q
                scaleX = 1f - 0.015f * q
                scaleY = scaleX
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            Enter.Stamp -> {
                val s = if (p < 0.55f) 1.7f - (1.7f - 0.94f) * (p / 0.55f) else 0.94f + 0.06f * ((p - 0.55f) / 0.45f)
                val r = if (p < 0.55f) -16f + 18f * (p / 0.55f) else 2f - 2f * ((p - 0.55f) / 0.45f)
                alpha = (p / 0.55f).coerceAtMost(1f)
                scaleX = s
                scaleY = s
                rotationZ = r
            }
        }
    }
}

/**
 * The rows a list has already played in: a lazy list recomposes a row that scrolls back into view,
 * and the web's rows only rise on the screen's first paint.
 */
class Played internal constructor() {
    internal val keys = HashSet<Any>()
}

@Composable
fun rememberPlayed(): Played = remember { Played() }

/**
 * A row in a list rises in its turn on the screen's first paint (web `.dl-list > .dl-row`: 40 ms
 * apart, capped at 320 ms). [key] identifies the row in [played]; [kind] changing (a letter that
 * arrived) plays it again, as the web remounts it.
 */
fun Modifier.rowEnter(index: Int, played: Played, key: Any, kind: Any? = Unit): Modifier = composed {
    val id = key to kind
    val first = remember(id) { played.keys.add(id) }
    if (first) enter(Enter.Rise, Motion.stagger(index), key = id) else this
}

/** Pressed controls shrink a little (web `:active` transforms: buttons 0.97, keys 0.9, rows 0.985). */
fun Modifier.pressScale(source: InteractionSource, pressed: Float = 0.95f): Modifier = composed {
    if (!motionOn()) return@composed this
    val down by source.collectIsPressedAsState()
    val s by animateFloatAsState(if (down) pressed else 1f, tween(Motion.PRESS_MS, easing = Motion.EaseOut), label = "press")
    graphicsLayer {
        scaleX = s
        scaleY = s
    }
}

/** dl-shake: a wrong PIN shakes its dots; [trigger] changes on each error. */
fun Modifier.shake(trigger: Any?): Modifier = composed {
    if (!motionOn() || trigger == null) return@composed this
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        x.snapTo(0f)
        x.animateTo(
            0f,
            keyframes {
                durationMillis = 420
                -8f at 84
                7f at 168
                -5f at 252
                3f at 336
                0f at 420
            },
        )
    }
    val dp = LocalDensity.current.density
    graphicsLayer { translationX = x.value * dp }
}

/** dl-float: an idle drift of 6 dp, every 6 s (the entry logo, empty-state art). */
fun Modifier.float(delayMs: Int = 600): Modifier = composed {
    if (!motionOn()) return@composed this
    val loop = rememberInfiniteTransition(label = "float")
    val y by loop.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(3000, delayMs, FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float",
    )
    val dp = LocalDensity.current.density
    graphicsLayer { translationY = -6 * dp * y }
}

/** dl-bob: the paper plane on an arc rocks gently. */
fun Modifier.bob(): Modifier = composed {
    if (!motionOn()) return@composed this
    val loop = rememberInfiniteTransition(label = "bob")
    val p by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob")
    val dp = LocalDensity.current.density
    graphicsLayer {
        translationY = -1.5f * dp * p
        rotationZ = -3f * p
    }
}

/** dl-tab-pop: the selected destination's icon hops when it becomes selected. */
fun Modifier.tabPop(selected: Boolean): Modifier = composed {
    if (!motionOn()) return@composed this
    val t = remember { Animatable(1f) }
    LaunchedEffect(selected) {
        if (selected) {
            t.snapTo(0f)
            t.animateTo(1f, tween(380, easing = Motion.EaseOut))
        }
    }
    val dp = LocalDensity.current.density
    graphicsLayer {
        // 0 → 45 % up to 114 % and 2 dp higher, then back.
        val p = t.value
        val k = if (p < 0.45f) p / 0.45f else 1f - (p - 0.45f) / 0.55f
        scaleX = 1f + 0.14f * k
        scaleY = scaleX
        translationY = -2 * dp * k
    }
}

/** dl-dot-fill: a PIN dot that has just filled pops. */
fun Modifier.dotFill(filled: Boolean): Modifier = composed {
    if (!motionOn()) return@composed this
    val t = remember { Animatable(1f) }
    LaunchedEffect(filled) {
        if (filled) {
            t.snapTo(0f)
            t.animateTo(1f, tween(240, easing = Motion.EaseOut))
        }
    }
    graphicsLayer {
        val p = t.value
        val s = if (p < 0.6f) 0.55f + (1.22f - 0.55f) * (p / 0.6f) else 1.22f - 0.22f * ((p - 0.6f) / 0.4f)
        scaleX = s
        scaleY = s
    }
}
