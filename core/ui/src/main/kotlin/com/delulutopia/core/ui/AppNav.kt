package com.delulutopia.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * How a screen moves elsewhere without knowing the app's NavController: open a route, replace the
 * current screen (Cancel from a letter opens Write in its place), or go back.
 */
interface AppNav {
    fun open(route: Any)

    fun replace(route: Any)

    fun back()

    /** Switch to a navigation-bar destination as the bar would (Home, Letters, …). */
    fun top(route: Any)
}

val LocalAppNav = staticCompositionLocalOf<AppNav> {
    object : AppNav {
        override fun open(route: Any) = Unit

        override fun replace(route: Any) = Unit

        override fun back() = Unit

        override fun top(route: Any) = Unit
    }
}

/** The notice line (web `useNotice`): one short sentence in the snackbar. */
val LocalNotice = staticCompositionLocalOf<(String) -> Unit> { {} }

/** The notice line with one action ("Microphone is off for Delulutopia · Settings"). */
val LocalNoticeAction = staticCompositionLocalOf<(text: String, action: String, onAction: () -> Unit) -> Unit> { { _, _, _ -> } }
