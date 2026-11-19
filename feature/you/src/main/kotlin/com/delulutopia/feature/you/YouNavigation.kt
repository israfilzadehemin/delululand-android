package com.delulutopia.feature.you

import androidx.navigation.NavGraphBuilder
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.ui.AppearanceRoute
import com.delulutopia.core.ui.FriendRoute
import com.delulutopia.core.ui.LanguageRoute
import com.delulutopia.core.ui.NotificationsRoute
import com.delulutopia.core.ui.ProfileRoute
import com.delulutopia.core.ui.UniverseRoute
import com.delulutopia.core.ui.YouRoute
import com.delulutopia.core.ui.page

/** The You tab and everything it opens (Profile, Universe, Appearance, Language, Notifications) plus the friend's profile. */
fun NavGraphBuilder.youGraph(themeMode: () -> ThemeMode, onThemeMode: (ThemeMode) -> Unit, onLogout: () -> Unit) {
    page<YouRoute> { YouScreen(themeMode(), onLogout) }
    page<ProfileRoute> { ProfileScreen() }
    page<FriendRoute> { FriendScreen() }
    page<AppearanceRoute> { AppearanceScreen(themeMode(), onThemeMode) }
    page<LanguageRoute> { LanguageScreen() }
    page<NotificationsRoute> { NotificationsScreen() }
    page<UniverseRoute> { UniverseScreen() }
}
