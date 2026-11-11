package com.delulutopia.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/*
 * Board reactions (web board/reactions.tsx, doc "Delululand — Board" §R): ten marker-drawn marks in
 * the board's ink colours on a 100-unit grid. A new mark draws itself in, stroke by stroke (R5).
 */

private val INK = Color(0xFF33323B)
private val RED = Color(0xFFB23A3A)
private val MUSTARD = Color(0xFFD4A24C)
private val SKY = Color(0xFF5B8FC7)
private val TERRACOTTA = Color(0xFFC96F52)
private val LAVENDER = Color(0xFF8A7BC0)

private const val FACE = "M50 13 C73 12 88 30 87 51 C86 72 70 87 49 86 C27 86 13 70 14 49 C15 28 30 14 50 13"

internal class Mark(val d: String, val colour: Color, val width: Float, val dot: Boolean = false)

private fun s(d: String, c: Color, w: Float) = Mark(d, c, w)

private fun dot(x: Int, y: Int) = Mark("M${x - 3.5f} $y a3.5 3.5 0 1 0 7 0 a3.5 3.5 0 1 0 -7 0", INK, 3f, dot = true)

class BoardReaction internal constructor(val key: String, val name: String, internal val marks: List<Mark>)

val REACTIONS: List<BoardReaction> = listOf(
    BoardReaction(
        "love",
        "Love it",
        listOf(s("M50 82 C22 63 12 46 19 31 C26 17 43 19 50 34 C57 19 74 16 81 30 C89 45 79 63 50 82 Z", RED, 6f), s("M29 33 q5 -8 13 -5", RED, 3.5f)),
    ),
    BoardReaction(
        "haha",
        "Haha",
        listOf(
            s(FACE, MUSTARD, 6f),
            s("M31 41 q6 -8 13 0 M56 41 q6 -8 13 0", INK, 5f),
            s("M31 55 Q50 82 69 55 Z", INK, 5f),
            s("M20 46 q-6 8 -2 13 M80 46 q6 8 2 13", SKY, 4f),
        ),
    ),
    BoardReaction(
        "wow",
        "Wow",
        listOf(
            s(FACE, MUSTARD, 6f),
            dot(37, 40),
            dot(63, 40),
            s("M50 56 C58 56 59 72 50 72 C41 72 42 56 50 56", INK, 5f),
            s("M30 28 q6 -5 12 -3 M58 25 q6 -2 12 3", INK, 3.5f),
        ),
    ),
    BoardReaction(
        "star",
        "Star",
        listOf(s("M50 11 L60 38 L89 39 L66 56 L75 85 L50 68 L26 85 L34 56 L11 39 L40 37 Z", MUSTARD, 6f), s("M82 14 v10 M77 19 h10", MUSTARD, 3.5f)),
    ),
    BoardReaction(
        "fire",
        "Fire",
        listOf(
            s("M50 89 C27 89 19 70 26 54 C31 44 39 39 37 24 C49 31 55 42 53 54 C59 48 61 39 59 30 C73 41 82 58 75 73 C71 83 62 89 50 89 Z", TERRACOTTA, 6f),
            s("M50 83 C41 83 39 74 43 68 C47 62 48 58 47 52 C55 58 60 66 58 74 C56 80 54 83 50 83", MUSTARD, 4.5f),
        ),
    ),
    BoardReaction(
        "yes",
        "Thumbs up",
        listOf(
            s("M22 48 H38 V86 H22 Z", INK, 5.5f),
            s("M38 51 L52 29 C55 20 66 22 64 33 L61 45 H79 C86 45 88 53 83 57 C88 61 86 67 81 69 C84 73 82 79 77 80 C78 86 73 88 69 88 H38", INK, 5.5f),
        ),
    ),
    BoardReaction(
        "aww",
        "Aww",
        listOf(s(FACE, SKY, 6f), dot(37, 42), dot(63, 42), s("M35 70 q15 -13 30 0", INK, 5f), s("M66 50 c-5 8 -4 14 0 14 c4 0 5 -6 0 -14", SKY, 3.5f)),
    ),
    BoardReaction(
        "kiss",
        "Kiss",
        listOf(
            s("M14 50 C28 34 41 37 50 44 C59 37 72 34 86 50 C73 67 61 73 50 73 C39 73 27 67 14 50 Z", RED, 6f),
            s("M16 50 C36 55 64 55 84 50", RED, 4f),
            s("M74 16 c-3 -6 -11 -3 -8 3 l8 9 l8 -9 c3 -6 -5 -9 -8 -3", RED, 3f),
        ),
    ),
    BoardReaction(
        "sparkle",
        "Sparkle",
        listOf(
            s("M40 18 C42 37 46 41 65 43 C46 45 42 49 40 68 C38 49 34 45 15 43 C34 41 38 37 40 18 Z", LAVENDER, 5.5f),
            s("M72 56 C73 65 75 67 84 68 C75 69 73 71 72 80 C71 71 69 69 60 68 C69 67 71 65 72 56 Z", MUSTARD, 4.5f),
            s("M74 14 v14 M67 21 h14", MUSTARD, 4f),
        ),
    ),
    BoardReaction(
        "hundred",
        "100",
        listOf(
            s("M14 36 L23 29 V70", RED, 6f),
            s("M45 30 C34 30 33 70 45 70 C57 70 56 30 45 30", RED, 6f),
            s("M73 30 C62 30 61 70 73 70 C85 70 84 30 73 30", RED, 6f),
            s("M12 80 Q50 74 89 80 M17 89 Q50 84 85 88", RED, 4f),
        ),
    ),
)

fun reactionOf(key: String?): BoardReaction? = REACTIONS.firstOrNull { it.key == key }

private val pathCache = HashMap<String, Path>()

private fun pathOf(d: String): Path = pathCache.getOrPut(d) { PathParser().parsePathString(d).toPath() }

/** One mark. [draw] plays the stroke-by-stroke draw-in (R5). */
@Composable
fun HandReaction(reaction: BoardReaction, size: Dp, modifier: Modifier = Modifier, tilt: Float = 0f, draw: Boolean = false, label: String? = null) {
    val progress = remember(reaction.key, draw) { Animatable(if (draw) 0f else 1f) }
    LaunchedEffect(reaction.key, draw) {
        if (draw) progress.animateTo(1f, tween(durationMillis = 700))
    }
    val semantics = if (label != null) Modifier.semantics { contentDescription = label } else Modifier
    Canvas(modifier.size(size).rotate(tilt).then(semantics)) {
        val marks = reaction.marks
        val p = progress.value
        scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) {
            marks.forEachIndexed { i, m ->
                // Each stroke draws in its own slice of the time, in order.
                val local = ((p * marks.size) - i).coerceIn(0f, 1f)
                if (local <= 0f) return@forEachIndexed
                val path = pathOf(m.d)
                if (m.dot) {
                    if (local >= 1f) drawPath(path, m.colour, style = Fill)
                    drawPath(path, m.colour, style = Stroke(m.width))
                    return@forEachIndexed
                }
                val stroke = if (local >= 1f) {
                    Stroke(m.width, cap = StrokeCap.Round, join = StrokeJoin.Round)
                } else {
                    val length = PathMeasure().apply { setPath(path, false) }.length * 2
                    Stroke(
                        m.width,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(length * local, length)),
                    )
                }
                drawPath(path, m.colour, style = stroke)
            }
        }
    }
}

/** The empty slot on the friend's doodle: a dashed smiley with a plus. */
@Composable
fun ReactSlot(colour: Color, plus: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier.size(size)) {
        scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) {
            drawPath(
                pathOf("M50 8 C74 7 92 27 91 51 C90 75 72 92 49 91 C25 91 8 73 9 49 C10 26 27 9 50 8"),
                colour,
                style = Stroke(4f, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f))),
            )
            drawPath(pathOf("M36 42 v2 M62 42 v2"), colour, style = Stroke(6f, cap = StrokeCap.Round))
            drawPath(pathOf("M35 60 q15 12 30 0"), colour, style = Stroke(4.5f, cap = StrokeCap.Round))
            drawPath(pathOf("M80 14 v18 M71 23 h18"), plus, style = Stroke(5f, cap = StrokeCap.Round))
        }
    }
}

/** The marker ring around the chosen mark in the picker. */
@Composable
fun ChosenRing(colour: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        scale(this.size.width / 100f, this.size.height / 100f, pivot = Offset.Zero) {
            drawPath(pathOf("M52 6 C78 6 95 26 93 52 C91 78 70 95 46 93 C22 91 6 72 8 48 C10 24 30 7 58 9"), colour, style = Stroke(4f, cap = StrokeCap.Round))
        }
    }
}
