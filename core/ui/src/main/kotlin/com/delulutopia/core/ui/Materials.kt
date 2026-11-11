package com.delulutopia.core.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.delulutopia.core.designsystem.component.LocalServerBaseUrl
import com.delulutopia.core.designsystem.component.resolveMedia
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.Enter
import com.delulutopia.core.designsystem.motion.bob
import com.delulutopia.core.designsystem.motion.enter
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.network.model.StampMark

/*
 * The materials each kind of item is made of (web components/Materials.tsx, styles/materials.css):
 * letters are envelopes and sheets. Purely visual.
 */

enum class TileShape { Envelope, Sheet }

private val TileCorner = RoundedCornerShape(4.dp)

/**
 * A letter row on Home or in the list: a slim envelope with its flap and the writer's wax seal, or
 * an unfolded sheet with a margin line, one rule and a dog-eared corner. [reach] is the in-reach
 * tint (the letter can still be edited or cancelled).
 */
@Composable
fun LetterTile(
    shape: TileShape,
    who: Who,
    modifier: Modifier = Modifier,
    reach: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = DlTheme.colors
    val base = modifier
        .fillMaxWidth()
        .shadow(1.dp, TileCorner, clip = false)
        .clip(TileCorner)
        .then(if (reach) Modifier.border(1.dp, c.markLine, TileCorner) else Modifier.border(0.5.dp, c.letterInk.copy(alpha = 0.22f), TileCorner))
        .background(
            if (reach) {
                c.markContainer
            } else if (shape == TileShape.Envelope) {
                c.letterEnvelope
            } else {
                c.letterSheet
            },
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    val seal = who.signature()
    if (shape == TileShape.Envelope) {
        Box(base) {
            Flap(seal)
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 30.dp, bottom = 12.dp), content = content)
        }
    } else {
        val margin = c.letterMargin
        val rule = c.letterRule
        val flap = c.letterFlap
        Box(
            base.drawBehind {
                val x = 7.dp.toPx()
                drawLine(margin, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                val y = size.height - 8.5.dp.toPx()
                drawLine(rule, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            },
        ) {
            Column(Modifier.padding(start = 18.dp, end = 14.dp, top = 12.dp, bottom = 14.dp), content = content)
            Fold(flap, Modifier.align(Alignment.BottomEnd))
        }
    }
}

/** The flap across the top of an envelope row, and its seal. */
@Composable
private fun Flap(seal: Color) {
    val c = DlTheme.colors
    Box(Modifier.fillMaxWidth().height(22.dp).clearAndSetSemantics { }) {
        Spacer(
            Modifier.fillMaxWidth().height(22.dp).drawBehind {
                val w = size.width
                val h = size.height
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w * 0.52f, h * 0.9f)
                    quadraticTo(w * 0.5f, h * 0.96f, w * 0.48f, h * 0.9f)
                    close()
                }
                drawPath(path, c.letterFlap)
                drawPath(path, c.letterEdge, style = Stroke(1.dp.toPx()))
            },
        )
        WaxSeal(seal, Modifier.align(Alignment.TopCenter).offset(y = 11.dp))
    }
}

/** A small wax seal in the writer's colour. */
@Composable
fun WaxSeal(colour: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Box(
        modifier
            .size(size)
            .shadow(1.dp, CircleShape)
            .background(colour, CircleShape)
            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape),
    )
}

/** The dog-eared corner of a sheet: the page's ground where the corner folded away, the flap colour its back. */
@Composable
private fun Fold(flap: Color, modifier: Modifier) {
    val ground = MaterialTheme.colorScheme.surface
    Spacer(
        modifier.size(14.dp).drawBehind {
            val s = size.width
            val away = Path().apply {
                moveTo(s, 0f)
                lineTo(s, s)
                lineTo(0f, s)
                close()
            }
            val back = Path().apply {
                moveTo(0f, 0f)
                lineTo(s, 0f)
                lineTo(0f, s)
                close()
            }
            drawPath(away, ground)
            drawPath(back, flap)
        },
    )
}

/**
 * The incoming arc (UX S2): a dotted path, the travelled part in the seal colour, a plane on it.
 */
@Composable
fun Arc(target: Float, who: Who, modifier: Modifier = Modifier) {
    // The arc and plane move every second; a one-second linear glide makes it continuous (web .dl-arc-done).
    val progress by animateFloatAsState(target, tween(1000, easing = LinearEasing), label = "arc")
    val c = DlTheme.colors
    val seal = who.signature()
    val density = LocalDensity.current
    Box(modifier.fillMaxWidth().height(14.dp).clearAndSetSemantics { }) {
        Spacer(
            Modifier.fillMaxWidth().height(14.dp).drawBehind {
                val y = 7.dp.toPx()
                val stroke = 1.5.dp.toPx()
                drawLine(
                    c.letterEdge,
                    Offset(0f, y),
                    Offset(size.width, y),
                    stroke,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(stroke, stroke * 1.6f)),
                )
                drawLine(seal, Offset(0f, y), Offset(size.width * progress.coerceIn(0f, 1f), y), stroke)
            },
        )
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val x = with(density) { (constraints.maxWidth * progress.coerceIn(0f, 1f)).toDp() } - 7.dp
            Icon(WebIcons.PlaneSmall, null, Modifier.offset(x = x).bob().size(14.dp), tint = c.letterInk)
        }
    }
}

/** One stamp (FS-ST6): the trailing edge of a row (28 × 36), the paper (72 × 91) or the picker. */
@Composable
fun StampImage(stamp: StampMark, modifier: Modifier = Modifier, width: Dp = 28.dp) {
    val base = LocalServerBaseUrl.current
    AsyncImage(
        model = resolveMedia(base, stamp.imageUrl),
        contentDescription = "Stamp: ${stamp.name}",
        modifier = modifier.size(width, width * (380f / 300f)),
        contentScale = ContentScale.Fit,
    )
}

/** The seal-coloured NEW chip on letter tiles and notes. */
@Composable
fun NewChip(modifier: Modifier = Modifier, text: String = "NEW") {
    val c = DlTheme.colors
    Box(
        modifier.enter(Enter.Pop, 160).height(22.dp).background(c.newBadge, RoundedCornerShape(11.dp)).padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = DlTheme.type.pill, color = c.onNewBadge)
    }
}
