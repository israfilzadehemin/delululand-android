package com.delulutopia.app.navigation

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.delulutopia.app.R
import com.delulutopia.core.designsystem.icon.DlIcons
import kotlinx.serialization.Serializable

/** Typed routes. The five destinations of the navigation bar (Y-A1) plus sub-screens. */
@Serializable data object HomeRoute

@Serializable data object LettersRoute

@Serializable data object DiscussionsRoute

@Serializable data object EmailsRoute

@Serializable data object YouRoute

@Serializable data object AppearanceRoute

enum class TopDestination(
    val route: Any,
    @StringRes val label: Int,
    val icon: ImageVector?,
    val activeIcon: ImageVector?,
) {
    Home(HomeRoute, R.string.nav_home, DlIcons.Home, DlIcons.HomeActive),
    Letters(LettersRoute, R.string.nav_letters, DlIcons.Letters, DlIcons.LettersActive),
    Discussions(DiscussionsRoute, R.string.nav_discussions, DlIcons.Discussions, DlIcons.DiscussionsActive),
    Emails(EmailsRoute, R.string.nav_emails, DlIcons.Emails, DlIcons.EmailsActive),
    You(YouRoute, R.string.nav_you, null, null),
}
