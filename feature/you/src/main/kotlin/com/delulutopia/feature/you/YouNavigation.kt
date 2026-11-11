package com.delulutopia.feature.you

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.ui.AppearanceRoute
import com.delulutopia.core.ui.FriendRoute
import com.delulutopia.core.ui.NotificationsRoute
import com.delulutopia.core.ui.ProfileRoute
import com.delulutopia.core.ui.UniverseRoute
import com.delulutopia.core.ui.YouRoute

/** The You tab and everything it opens (Profile, Universe, Appearance, Notifications) plus the friend's profile. */
fun NavGraphBuilder.youGraph(themeMode: () -> ThemeMode, onThemeMode: (ThemeMode) -> Unit, onLogout: () -> Unit) {
    composable<YouRoute> { YouScreen(themeMode(), onLogout) }
    composable<ProfileRoute> { ProfileScreen() }
    composable<FriendRoute> { FriendScreen() }
    composable<AppearanceRoute> { AppearanceScreen(themeMode(), onThemeMode) }
    composable<NotificationsRoute> { NotificationsScreen() }
    composable<UniverseRoute> { UniverseScreen() }
}
