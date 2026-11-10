package com.delulutopia.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * Colours, one-to-one with the web tokens (delululand-web src/styles/tokens.css, materials.css,
 * board/board.css) and the Android canvas "Foundations › Colour roles" (Z-Tokens). Dynamic colour
 * (Material You) stays off (decision A1), so every value here is fixed.
 */

/** Brand colours that have no Material 3 role: letter paper, mail cards, board, notes, signatures. */
@Immutable
data class DlColors(
    // Ink beyond the M3 roles
    val inkDisabled: Color,
    val inkOnScrim: Color,
    // Signature colours (same in both themes)
    val userA: Color,
    val userB: Color,
    val userATint: Color,
    val userBTint: Color,
    val postmarkHue: Color,
    // Letters (envelope, sheet, slip)
    val letterSheet: Color,
    val letterEnvelope: Color,
    val letterFlap: Color,
    val letterEdge: Color,
    val letterSlip: Color,
    val letterInk: Color,
    val letterRule: Color,
    val letterMargin: Color,
    val letterPrint: Color,
    val letterTape: Color,
    // Letter status marks (lt-*)
    val markContainer: Color,
    val markLine: Color,
    val countdownChip: Color,
    val newBadge: Color,
    val onNewBadge: Color,
    val reactLine: Color,
    // Mail cards (Emails)
    val mailBackground: Color,
    val mailMine: Color,
    val mailLine: Color,
    val mailBlue: Color,
    val onMailBlue: Color,
    val mailUnread: Color,
    val mailInk: Color,
    // Board (whiteboard)
    val boardFrame: Color,
    val boardTray: Color,
    val boardSurface: Color,
    // Discussion sticky notes, indexed by [NoteColor]
    val notes: Map<NoteColor, NotePair>,
    // Lightbox and board viewer
    val lightbox: Color,
    val onLightbox: Color,
    val onLightboxQuiet: Color,
)

/** A sticky note's face and its deeper fold/shadow tone. */
@Immutable
data class NotePair(val face: Color, val deep: Color)

/** The six classic note colours a discussion author picks (doc "Delululand — Discussions"), plus blank. */
enum class NoteColor { Lemon, Peach, Pink, Lavender, Sky, Mint, Blank }

internal val LightDlColors = DlColors(
    inkDisabled = Color(0xFFA3A8B6),
    inkOnScrim = Color(0xFF7A8296),
    userA = Color(0xFFD99A82),
    userB = Color(0xFF9CBDB3),
    userATint = Color(0xFFF2E0D7),
    userBTint = Color(0xFFDCE8E3),
    postmarkHue = Color(0xFF6F978B),
    letterSheet = Color(0xFFFBF8F3),
    letterEnvelope = Color(0xFFE8E1D8),
    letterFlap = Color(0xFFDED5C9),
    letterEdge = Color(0xFFC9BFB2),
    letterSlip = Color(0xFFFFFDF9),
    letterInk = Color(0xFF33323B),
    letterRule = Color(0x2948536C), // rgb(72 83 108 / 0.16)
    letterMargin = Color(0x38B23A3A), // rgb(178 58 58 / 0.22)
    letterPrint = Color(0xFFFFFFFF),
    letterTape = Color(0xC7F0E8D2), // rgb(240 232 210 / 0.78)
    markContainer = Color(0xFFF2D9C8),
    markLine = Color(0xFFDFAE92),
    countdownChip = Color(0xFFE6C3AD),
    newBadge = Color(0xFFA9512F),
    onNewBadge = Color(0xFFFFF8F2),
    reactLine = Color(0xFFC98B6E),
    mailBackground = Color(0xFFFFFFFF),
    mailMine = Color(0xFFF3F5F9),
    mailLine = Color(0xFFDDE2EA),
    mailBlue = Color(0xFF2A62CC),
    onMailBlue = Color(0xFFFFFFFF),
    mailUnread = Color(0xFFEEF3FD),
    mailInk = Color(0xFF1F2430),
    boardFrame = Color(0xFFC7CBD1),
    boardTray = Color(0xFFAEB3BA),
    boardSurface = Color(0xFFFBFBF8),
    notes = mapOf(
        NoteColor.Lemon to NotePair(Color(0xFFF6EAB0), Color(0xFFECDC93)),
        NoteColor.Peach to NotePair(Color(0xFFF8D9C3), Color(0xFFEFC6A9)),
        NoteColor.Pink to NotePair(Color(0xFFF5D3DC), Color(0xFFECBFCB)),
        NoteColor.Lavender to NotePair(Color(0xFFE0DAF2), Color(0xFFD0C8EA)),
        NoteColor.Sky to NotePair(Color(0xFFCFE3F3), Color(0xFFBAD5EC)),
        NoteColor.Mint to NotePair(Color(0xFFD4E9DC), Color(0xFFC0DCCB)),
        NoteColor.Blank to NotePair(Color(0xFFECE7DE), Color(0xFFE2DBD0)),
    ),
    lightbox = Color(0xFF202B45),
    onLightbox = Color(0xFFF5F0E8),
    onLightboxQuiet = Color(0xFFD9D0C5),
)

internal val DarkDlColors = DlColors(
    inkDisabled = Color(0xFF5C6273),
    inkOnScrim = Color(0xFF7A8296),
    userA = Color(0xFFD99A82),
    userB = Color(0xFF9CBDB3),
    userATint = Color(0xFF3B2A24),
    userBTint = Color(0xFF1F2D2A),
    postmarkHue = Color(0xFF7FA89B),
    letterSheet = Color(0xFF3A3630),
    letterEnvelope = Color(0xFF332F2A),
    letterFlap = Color(0xFF2B2823),
    letterEdge = Color(0xFF4B463E),
    letterSlip = Color(0xFF444039),
    letterInk = Color(0xFFE8E0D2),
    letterRule = Color(0x17E8E0D2), // rgb(232 224 210 / 0.09)
    letterMargin = Color(0x4DEE8A80), // rgb(238 138 128 / 0.3)
    letterPrint = Color(0xFFD9D1C2),
    letterTape = Color(0x59D6CCB0), // rgb(214 204 176 / 0.35)
    markContainer = Color(0xFF55402F),
    markLine = Color(0xFF7D5C46),
    countdownChip = Color(0xFF6A503D),
    newBadge = Color(0xFFE3A184),
    onNewBadge = Color(0xFF2B1A12),
    reactLine = Color(0xFFC4785A),
    mailBackground = Color(0xFF23262E),
    mailMine = Color(0xFF2A2E37),
    mailLine = Color(0xFF353A46),
    mailBlue = Color(0xFF8AB4FF),
    onMailBlue = Color(0xFF0F1A2E),
    mailUnread = Color(0xFF262C3A),
    mailInk = Color(0xFFE6E9EF),
    boardFrame = Color(0xFF4B4F57),
    boardTray = Color(0xFF3B3E45),
    boardSurface = Color(0xFFECE8E0),
    notes = mapOf(
        NoteColor.Lemon to NotePair(Color(0xFF4F4A2C), Color(0xFF45402A)),
        NoteColor.Peach to NotePair(Color(0xFF5B4436), Color(0xFF4E3A2E)),
        NoteColor.Pink to NotePair(Color(0xFF563A44), Color(0xFF4A323B)),
        NoteColor.Lavender to NotePair(Color(0xFF433D5A), Color(0xFF3A354F)),
        NoteColor.Sky to NotePair(Color(0xFF2F4457), Color(0xFF293B4C)),
        NoteColor.Mint to NotePair(Color(0xFF34493E), Color(0xFF2D3F36)),
        NoteColor.Blank to NotePair(Color(0xFF34312C), Color(0xFF2D2A26)),
    ),
    lightbox = Color(0xFF0B0D12),
    onLightbox = Color(0xFFF5F0E8),
    onLightboxQuiet = Color(0xFFD9D0C5),
)

/** Material 3 roles from Z-Tokens (M3 role → web token → light / dark). */
internal val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF202B45),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4DFF0),
    onPrimaryContainer = Color(0xFF202B45),
    inversePrimary = Color(0xFFB4C2EA),
    secondary = Color(0xFF48536C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2D9C8),
    onSecondaryContainer = Color(0xFF33323B),
    tertiary = Color(0xFFA9512F),
    onTertiary = Color(0xFFFFF8F2),
    tertiaryContainer = Color(0xFFF2E0D7),
    onTertiaryContainer = Color(0xFF33323B),
    background = Color(0xFFF5F0E8),
    onBackground = Color(0xFF33323B),
    surface = Color(0xFFF5F0E8),
    onSurface = Color(0xFF33323B),
    surfaceVariant = Color(0xFFE8E1D8),
    onSurfaceVariant = Color(0xFF48536C),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF33323B),
    inverseOnSurface = Color(0xFFF5F0E8),
    error = Color(0xFFB23A3A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF3DADA),
    onErrorContainer = Color(0xFF33323B),
    outline = Color(0xFF8E93A0),
    outlineVariant = Color(0xFFD9D0C5),
    scrim = Color(0x73202B45), // rgba(32,43,69,0.45)
    surfaceBright = Color(0xFFF5F0E8),
    surfaceDim = Color(0xFFE1D9CE),
    surfaceContainerLowest = Color(0xFFF5F0E8),
    surfaceContainerLow = Color(0xFFEFE9E0),
    surfaceContainer = Color(0xFFECE6DD),
    surfaceContainerHigh = Color(0xFFE8E1D8),
    surfaceContainerHighest = Color(0xFFE1D9CE),
)

internal val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFB4C2EA),
    onPrimary = Color(0xFF141A2C),
    primaryContainer = Color(0xFF2C2A40),
    onPrimaryContainer = Color(0xFFB4C2EA),
    inversePrimary = Color(0xFF202B45),
    secondary = Color(0xFFA3ADC4),
    onSecondary = Color(0xFF141A2C),
    secondaryContainer = Color(0xFF55402F),
    onSecondaryContainer = Color(0xFFE8E0D2),
    tertiary = Color(0xFFE3A184),
    onTertiary = Color(0xFF2B1A12),
    tertiaryContainer = Color(0xFF3B2A24),
    onTertiaryContainer = Color(0xFFECE6DC),
    background = Color(0xFF16181F),
    onBackground = Color(0xFFECE6DC),
    surface = Color(0xFF16181F),
    onSurface = Color(0xFFECE6DC),
    surfaceVariant = Color(0xFF22252E),
    onSurfaceVariant = Color(0xFFA3ADC4),
    surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFECE6DC),
    inverseOnSurface = Color(0xFF16181F),
    error = Color(0xFFEE8A80),
    onError = Color(0xFF141A2C),
    errorContainer = Color(0xFF3A2224),
    onErrorContainer = Color(0xFFECE6DC),
    outline = Color(0xFF6E7487),
    outlineVariant = Color(0xFF3B3F4C),
    scrim = Color(0x99000000), // rgba(0,0,0,0.6)
    surfaceBright = Color(0xFF2A2E38),
    surfaceDim = Color(0xFF16181F),
    surfaceContainerLowest = Color(0xFF16181F),
    surfaceContainerLow = Color(0xFF1C1F27),
    surfaceContainer = Color(0xFF1D2029),
    surfaceContainerHigh = Color(0xFF22252E),
    surfaceContainerHighest = Color(0xFF2A2E38),
)
