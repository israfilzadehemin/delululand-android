package com.delulutopia.app.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.delulutopia.app.R
import com.delulutopia.app.navigation.AppearanceRoute
import com.delulutopia.app.navigation.DiscussionsRoute
import com.delulutopia.app.navigation.EmailsRoute
import com.delulutopia.app.navigation.HomeRoute
import com.delulutopia.app.navigation.LettersRoute
import com.delulutopia.app.navigation.TopDestination
import com.delulutopia.app.navigation.YouRoute
import com.delulutopia.core.designsystem.component.DlAvatar
import com.delulutopia.core.designsystem.component.DlFab
import com.delulutopia.core.designsystem.component.DlNavigationBar
import com.delulutopia.core.designsystem.component.DlNavigationItem
import com.delulutopia.core.designsystem.component.DlNavigationYouItem
import com.delulutopia.core.designsystem.component.DlSnackbarHost
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.ThemeMode
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

/**
 * The app shell: top app bar (title only, Y-A3), navigation bar with five destinations (Y-A1) and a
 * FAB on Letters, Discussions and Emails only (Y-A2). Sub-screens hide the bar and show ←.
 */
@Composable
fun DelulutopiaApp(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notBuilt = stringResource(R.string.placeholder_snackbar)
    val showNotBuilt: () -> Unit = { scope.launch { snackbarHostState.showSnackbar(notBuilt) } }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val top = TopDestination.entries.firstOrNull { destination.isOn(it.route::class) }

    Scaffold(
        topBar = {
            when {
                // Home's title is the universe's name; the session arrives in P1.
                top == TopDestination.Home -> DlTopAppBar(title = "Delulutopia")
                top != null -> DlTopAppBar(title = stringResource(top.label))
                destination.isOn(AppearanceRoute::class) -> DlTopAppBar(
                    title = stringResource(R.string.appearance_title),
                    onBack = { navController.popBackStack() },
                    backDescription = stringResource(R.string.back),
                )
            }
        },
        bottomBar = {
            if (top != null) {
                DlNavigationBar {
                    TopDestination.entries.forEach { item ->
                        val selected = item == top
                        val open = { navController.navigateToTop(item) }
                        if (item == TopDestination.You) {
                            DlNavigationYouItem(selected = selected, onClick = open, label = stringResource(item.label)) {
                                DlAvatar(initials = "Y", signature = DlTheme.colors.userA, size = 24.dp)
                            }
                        } else {
                            DlNavigationItem(
                                selected = selected,
                                onClick = open,
                                label = stringResource(item.label),
                                icon = item.icon!!,
                                activeIcon = item.activeIcon!!,
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            when (top) {
                TopDestination.Letters -> DlFab(DlIcons.Pen, stringResource(R.string.fab_new_letter), showNotBuilt)
                TopDestination.Discussions -> DlFab(DlIcons.NotePlus, stringResource(R.string.fab_new_discussion), showNotBuilt)
                TopDestination.Emails -> DlFab(DlIcons.EnvelopePlus, stringResource(R.string.fab_new_email), showNotBuilt)
                else -> Unit
            }
        },
        snackbarHost = { DlSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface,
    ) { padding ->
        NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
            composable<HomeRoute> { PlaceholderScreen() }
            composable<LettersRoute> { PlaceholderScreen() }
            composable<DiscussionsRoute> { PlaceholderScreen() }
            composable<EmailsRoute> { PlaceholderScreen() }
            composable<YouRoute> { YouScreen(onOpenAppearance = { navController.navigate(AppearanceRoute) }) }
            composable<AppearanceRoute> { AppearanceScreen(themeMode, onThemeModeChange) }
        }
    }
}

private fun NavDestination?.isOn(route: KClass<*>): Boolean = this?.hierarchy?.any { it.hasRoute(route) } == true

/** Switch destinations like the M3 bar: single top, back returns to Home, state kept per destination. */
private fun NavHostController.navigateToTop(destination: TopDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
