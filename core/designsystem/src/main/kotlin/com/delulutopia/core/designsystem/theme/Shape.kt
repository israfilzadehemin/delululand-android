package com.delulutopia.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Web shape tokens: rows 12, buttons 20, sheets 24; paper cards keep 4 dp corners. */
internal val DlShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object DlShape {
    /** Letter, note and mail cards (doc "Delululand — Discussions", materials). */
    val card = RoundedCornerShape(4.dp)
    val row = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(20.dp)
    val sheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val snackbar = RoundedCornerShape(8.dp)
    val textField = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
}

/** 8 dp grid (web: gutter 16, row gap 8; Android: 48 dp touch targets, P11). */
object DlSpacing {
    val gutter = 16.dp
    val rowGap = 8.dp
    val touch = 48.dp

    /** Empty space at the end of a list with a FAB, so the last row scrolls clear of it (P12). */
    val fabListEnd = 88.dp
}
