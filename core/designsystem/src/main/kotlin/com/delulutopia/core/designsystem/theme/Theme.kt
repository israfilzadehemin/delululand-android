package com.delulutopia.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

/** Settings › Appearance (A4): Light, Dark or Match device. Saved per device. */
enum class ThemeMode { Light, Dark, System }

@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
    ThemeMode.System -> isSystemInDarkTheme()
}

private val LocalDlColors = staticCompositionLocalOf { LightDlColors }
private val LocalDlType = staticCompositionLocalOf { DefaultDlType }
private val LocalDlDark = staticCompositionLocalOf { false }

/**
 * Brand on Material 3 (decision A1): the Delulutopia palette and type on M3 structure. Dynamic
 * colour is deliberately not used.
 */
@Composable
fun DelulutopiaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDlColors provides if (darkTheme) DarkDlColors else LightDlColors,
        LocalDlType provides DefaultDlType,
        LocalDlDark provides darkTheme,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = DlTypography,
            shapes = DlShapes,
            content = content,
        )
    }
}

/** Brand tokens alongside [MaterialTheme]: `DlTheme.colors.letterSheet`, `DlTheme.type.countdown`. */
object DlTheme {
    val colors: DlColors
        @Composable @ReadOnlyComposable
        get() = LocalDlColors.current

    val type: DlType
        @Composable @ReadOnlyComposable
        get() = LocalDlType.current

    /** Whether the dark theme is showing (a few materials keep their own dark tones). */
    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalDlDark.current
}
