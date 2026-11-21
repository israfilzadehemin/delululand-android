package com.delulutopia.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.delulutopia.core.designsystem.motion.Motion

/** Board §9 / Selfies DS1 fold: how long the tile takes to roll up into its strip or out of it. */
internal const val FOLD_MS = 360

/**
 * The Board's and the selfie board's fold. Folding, the open board stays put while the tile's
 * height runs down to the strip's, clipped from the bottom, so the board rolls up into the strip;
 * the strip fades in as it lands. Unfolding, the strip fades out at once and the board unrolls
 * downward. Home below follows the height on both ways.
 */
@Composable
internal fun Fold(mini: Boolean, modifier: Modifier = Modifier, strip: @Composable () -> Unit, open: @Composable () -> Unit) {
    val fold = updateTransition(mini, label = "fold")
    // Clipped only while it moves: at rest the board's shadow and the sticky note's tape spill out.
    fold.AnimatedContent(
        modifier = modifier.then(if (fold.isRunning) Modifier.clipToBounds() else Modifier),
        contentAlignment = Alignment.TopStart,
        transitionSpec = {
            val size = SizeTransform(clip = false) { _, _ -> tween(FOLD_MS, easing = Motion.Emphasized) }
            if (targetState) {
                val stripIn = fadeIn(tween(200, delayMillis = FOLD_MS - 160, easing = Motion.EaseOut))
                val boardOut = fadeOut(tween(120, delayMillis = FOLD_MS - 120))
                (stripIn togetherWith boardOut) using size
            } else {
                val stripOut = fadeOut(tween(Motion.QUICK_MS - 40, easing = Motion.EaseOut))
                (EnterTransition.None togetherWith stripOut) using size
            }
        },
    ) { folded ->
        if (folded) strip() else open()
    }
}
