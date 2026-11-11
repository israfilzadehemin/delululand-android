package com.delulutopia.core.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.NoteColor

/*
 * Discussions are sticky notes (Materials story; web styles/materials.css): a square note in the
 * colour its author picked, a sheen down to its foot, a red barrel pin pushed in at the top.
 */

/** The API's colour (`lemon` … `mint`), lemon when unset. */
fun noteColorOf(wire: String?): NoteColor =
    NoteColor.entries.firstOrNull { it.name.equals(wire, ignoreCase = true) && it != NoteColor.Blank } ?: NoteColor.Lemon

val NoteColor.wire: String get() = name.lowercase()

/** The six colours an author picks, in order. */
val PICKABLE_NOTES = listOf(NoteColor.Lemon, NoteColor.Peach, NoteColor.Pink, NoteColor.Lavender, NoteColor.Sky, NoteColor.Mint)

/** Ink on a note (contrast-checked in both themes). */
val noteInk: Color
    @Composable @ReadOnlyComposable
    get() = if (DlTheme.isDark) Color(0xFFECE4D6) else Color(0xFF33323B)

val noteInkQuiet: Color
    @Composable @ReadOnlyComposable
    get() = if (DlTheme.isDark) Color(0xFFC4B9A8) else Color(0xFF424A5C)

/** A note's paper; [unseen] rings it in terracotta, [selected] in the seal. */
@Composable
fun Note(
    colour: NoteColor,
    modifier: Modifier = Modifier,
    unseen: Boolean = false,
    pin: Dp = 20.dp,
    tilt: Float = 0f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val pair = DlTheme.colors.notes.getValue(colour)
    val shape = RoundedCornerShape(2.dp)
    Box(modifier.fillMaxWidth().rotate(tilt)) {
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(3.dp, shape)
                .background(Brush.verticalGradient(0.62f to pair.face, 1f to pair.deep), shape)
                .then(if (unseen) Modifier.border(1.5.dp, DlTheme.colors.markLine, shape) else Modifier)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            content = content,
        )
        if (pin > 0.dp) Pin(pin, Modifier.align(Alignment.TopCenter).offset(y = -pin * 0.85f))
    }
}

/** The red barrel map pin, leaning 28° about its point. */
@Composable
fun Pin(width: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(width, width * 5f / 3f).rotate(28f)) {
        scale(size.width / 24f, size.height / 40f, pivot = Offset.Zero) {
            // The needle.
            drawPath(
                Path().apply {
                    moveTo(10.6f, 27.5f)
                    lineTo(13.4f, 27.5f)
                    lineTo(12.8f, 38.2f)
                    lineTo(12f, 39.8f)
                    lineTo(11.2f, 38.2f)
                    close()
                },
                Brush.horizontalGradient(listOf(Color(0xFF3F434A), Color(0xFFD6DADE), Color(0xFFF4F6F8), Color(0xFF4A4E55)), 10.6f, 13.4f),
            )
            drawOval(Color(0xFF4A080C), Offset(3.4f, 25.3f), Size(17.2f, 5.8f))
            val barrel = Brush.horizontalGradient(
                0f to Color(0xFF6E0F14),
                0.22f to Color(0xFFD33A37),
                0.38f to Color(0xFFF27A6E),
                0.55f to Color(0xFFC8282C),
                1f to Color(0xFF6A0D12),
                startX = 4f,
                endX = 20f,
            )
            drawPath(
                Path().apply {
                    moveTo(3.6f, 27.6f)
                    cubicTo(3.6f, 25.6f, 7.6f, 24.6f, 9f, 22.6f)
                    lineTo(15f, 22.6f)
                    cubicTo(16.4f, 24.6f, 20.4f, 25.6f, 20.4f, 27.6f)
                    cubicTo(20.4f, 29.4f, 3.6f, 29.4f, 3.6f, 27.6f)
                    close()
                },
                barrel,
            )
            drawPath(
                Path().apply {
                    moveTo(7.2f, 9.4f)
                    cubicTo(8.8f, 12.6f, 9.2f, 18.6f, 8.6f, 23f)
                    cubicTo(10.8f, 24.1f, 13.2f, 24.1f, 15.4f, 23f)
                    cubicTo(14.8f, 18.6f, 15.2f, 12.6f, 16.8f, 9.4f)
                    close()
                },
                barrel,
            )
            drawPath(
                Path().apply {
                    moveTo(4.6f, 9.2f)
                    lineTo(4.6f, 6.6f)
                    cubicTo(4.6f, 3f, 19.4f, 3f, 19.4f, 6.6f)
                    lineTo(19.4f, 9.2f)
                    cubicTo(19.4f, 12.2f, 4.6f, 12.2f, 4.6f, 9.2f)
                    close()
                },
                barrel,
            )
            drawOval(
                Brush.linearGradient(listOf(Color(0xFFF58A7E), Color(0xFFD83A38), Color(0xFFA51B21)), Offset(4.6f, 3.5f), Offset(19.4f, 9.3f)),
                Offset(4.6f, 3.5f),
                Size(14.8f, 5.8f),
            )
            drawOval(Color.White.copy(alpha = 0.55f), Offset(6.6f, 4.6f), Size(6f, 2f))
        }
    }
}
