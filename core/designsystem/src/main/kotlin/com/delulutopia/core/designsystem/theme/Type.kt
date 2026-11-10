package com.delulutopia.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.R

/*
 * Type (canvas "Foundations › Type", Z-Type): Manrope for the interface, Literata for letters and
 * notes, Caveat for the board note. Fonts are bundled (P10) and sized in sp. The bundled Manrope
 * carries capital schwa Ə (U+018F) merged from Noto Sans by tools/fonts/merge_schwa.py, as the web
 * does with its 'DL Schwa' face. Arabic script falls back to the system's Noto Arabic faces.
 */

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
    Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
)

val Literata = FontFamily(
    Font(R.font.literata_regular, FontWeight.Normal),
    Font(R.font.literata_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.literata_semibold, FontWeight.SemiBold),
    Font(R.font.literata_bold, FontWeight.Bold),
    Font(R.font.literata_bold_italic, FontWeight.Bold, FontStyle.Italic),
)

val Caveat = FontFamily(
    Font(R.font.caveat_medium, FontWeight.Medium),
    Font(R.font.caveat_semibold, FontWeight.SemiBold),
)

private val Trim = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None)

private fun ui(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
    lineHeightStyle = Trim,
)

/** The M3 scale filled from Z-Type. Roles the canvas does not name keep M3's sizes in Manrope. */
internal val DlTypography = Typography(
    displayLarge = ui(57, 64, FontWeight.ExtraBold),
    displayMedium = ui(45, 52, FontWeight.ExtraBold),
    // Display · universe name on cards
    displaySmall = ui(32, 40, FontWeight.ExtraBold),
    headlineLarge = ui(32, 40, FontWeight.ExtraBold),
    headlineMedium = ui(28, 36, FontWeight.ExtraBold),
    // Headline small · wizard titles
    headlineSmall = ui(28, 36, FontWeight.ExtraBold),
    // Title large · top app bar
    titleLarge = ui(22, 28, FontWeight.Bold, -0.01),
    // Title medium · row titles, names
    titleMedium = ui(16, 24, FontWeight.Bold),
    titleSmall = ui(14, 20, FontWeight.Bold, 0.01),
    // Body large · inputs
    bodyLarge = ui(16, 24, FontWeight.Normal),
    // Body · rows, sheets (brand 15)
    bodyMedium = ui(15, 22, FontWeight.Medium),
    // Body small · times, hints
    bodySmall = ui(13, 16, FontWeight.Medium),
    // Buttons
    labelLarge = ui(14, 20, FontWeight.Bold, 0.01),
    // Navigation bar labels: 12 sp, normal case (Y-A1)
    labelMedium = ui(12, 16, FontWeight.SemiBold, 0.01),
    // Label · section labels, caps
    labelSmall = ui(11, 16, FontWeight.ExtraBold, 0.09),
)

/** Brand styles with no M3 role. */
@Immutable
data class DlType(
    /** Countdown digits, tabular numerals (28:41). */
    val countdown: TextStyle,
    /** Letter body in Literata. */
    val letterBody: TextStyle,
    /** Discussion note title in Literata. */
    val noteTitle: TextStyle,
    /** Board note in Caveat. */
    val boardNote: TextStyle,
    /** NEW / "2 new" pills. */
    val pill: TextStyle,
    /** Entry keys, shown and typed as XXXX-XXXX-XXXX. */
    val key: TextStyle,
    /** Extended FAB label. */
    val fabLabel: TextStyle,
)

internal val DefaultDlType = DlType(
    countdown = ui(18, 24, FontWeight.Bold).copy(fontFeatureSettings = "tnum"),
    letterBody = TextStyle(
        fontFamily = Literata,
        fontSize = 17.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        lineHeightStyle = Trim,
    ),
    noteTitle = TextStyle(
        fontFamily = Literata,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        lineHeightStyle = Trim,
    ),
    boardNote = TextStyle(
        fontFamily = Caveat,
        fontSize = 22.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium,
        lineHeightStyle = Trim,
    ),
    pill = ui(11, 14, FontWeight.ExtraBold, 0.08),
    key = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.08.em,
    ),
    fabLabel = ui(15, 20, FontWeight.Bold),
)
