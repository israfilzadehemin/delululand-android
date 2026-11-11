package com.delulutopia.app.ui

import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.delulutopia.app.R
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlNavigationBar
import com.delulutopia.core.designsystem.component.DlNavigationItem
import com.delulutopia.core.designsystem.component.DlNavigationYouItem
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSnackbarHost
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.ui.AfterStart
import com.delulutopia.core.ui.AppNav
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EmailsRoute
import com.delulutopia.core.ui.HomeRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalFirstCitizen
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.LocalNoticeAction
import com.delulutopia.core.ui.YouRoute
import com.delulutopia.core.ui.fingerprint.Fingerprint
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.leaves.LeafLayer
import com.delulutopia.core.ui.leaves.LeafSurfaces
import com.delulutopia.core.ui.leaves.LocalLeafSurfaces
import com.delulutopia.core.ui.leaves.SurfaceKind
import com.delulutopia.core.ui.leaves.leafCeiling
import com.delulutopia.core.ui.leaves.leafSurface
import com.delulutopia.core.ui.notify.NotifyPermission
import com.delulutopia.core.ui.notify.rememberNotifyAccess
import com.delulutopia.core.ui.reactions.LocalReactionsApi
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.feature.discussions.discussionsGraph
import com.delulutopia.feature.emails.emailsGraph
import com.delulutopia.feature.home.HomeScreen
import com.delulutopia.feature.letters.lettersGraph
import com.delulutopia.feature.you.youGraph
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

/** The five destinations of the navigation bar (Y-A1). */
enum class TopDestination(val route: Any, @StringRes val label: Int, val icon: ImageVector?, val activeIcon: ImageVector?) {
    Home(HomeRoute, R.string.nav_home, DlIcons.Home, DlIcons.HomeActive),
    Letters(LettersRoute, R.string.nav_letters, DlIcons.Letters, DlIcons.LettersActive),
    Discussions(DiscussionsRoute, R.string.nav_discussions, DlIcons.Discussions, DlIcons.DiscussionsActive),
    Emails(EmailsRoute, R.string.nav_emails, DlIcons.Emails, DlIcons.EmailsActive),
    You(YouRoute, R.string.nav_you, null, null),
}

/**
 * The app shell (web AuthedLayout, TabBar): top app bar, navigation bar with five destinations and
 * their dots, the notice line, one live stream while in the foreground. Sub-screens hide the bar.
 */
@Composable
fun SignedInApp(
    session: HomeSession,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLogout: () -> Unit,
    shell: ShellViewModel = hiltViewModel(),
) {
    val state by shell.state.collectAsStateWithLifecycle()
    val leaves by shell.leaves.collectAsStateWithLifecycle()
    val currentTheme by rememberUpdatedState(themeMode)
    LifecycleStartEffect(Unit) {
        shell.foreground()
        onStopOrDispose { shell.background() }
    }
    CompositionLocalProvider(LocalReactionsApi provides shell.reactions) {
        DelulutopiaApp(session, state, leaves = leaves, overlay = { AfterPin() }) {
            composable<HomeRoute> { HomeScreen() }
            lettersGraph()
            discussionsGraph()
            emailsGraph()
            youGraph({ currentTheme }, onThemeModeChange, onLogout)
        }
    }
}

/**
 * What follows a PIN entry once Home is up: X1's one-time fingerprint offer, and U9's word when the
 * citizen photo could not be uploaded.
 */
@Composable
private fun AfterPin() {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val notice = LocalNotice.current
    var offer by remember {
        val entered = Fingerprint.justEntered
        Fingerprint.justEntered = null
        mutableStateOf(
            entered?.takeIf { (id, _) ->
                activity != null && Fingerprint.available(context) && !Fingerprint.offered(context, id) && !Fingerprint.isOn(context, id)
            },
        )
    }
    LaunchedEffect(Unit) {
        if (AfterStart.photoFailed) {
            AfterStart.photoFailed = false
            notice("Couldn't add your photo. You can add it in Profile.")
        }
    }
    val pending = offer ?: return
    val close = {
        Fingerprint.markOffered(context, pending.first)
        offer = null
    }
    DlDialog(
        title = stringResource(R.string.fingerprint_offer_title),
        text = stringResource(R.string.fingerprint_offer_text),
        confirmLabel = stringResource(R.string.fingerprint_offer_yes),
        onConfirm = {
            close()
            if (activity != null) Fingerprint.turnOn(activity, pending.first, pending.second, context.getString(R.string.fingerprint_offer_yes)) { }
        },
        dismissLabel = stringResource(R.string.not_now),
        onDismiss = close,
    )
}

/** The shell itself, stateless: [state] is Home's, [graph] the screens (tests pass their own). */
@Composable
fun DelulutopiaApp(
    session: HomeSession,
    state: HomeRepository.State,
    navController: NavHostController = rememberNavController(),
    leaves: Boolean = false,
    overlay: @Composable () -> Unit = {},
    graph: NavGraphBuilder.(NavHostController) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val home = state.home

    val notice: (String) -> Unit = { text ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(text)
        }
    }
    val noticeAction: (String, String, () -> Unit) -> Unit = { text, action, onAction ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            if (snackbarHostState.showSnackbar(text, actionLabel = action) == SnackbarResult.ActionPerformed) onAction()
        }
    }
    val nav = remember(navController) {
        object : AppNav {
            override fun open(route: Any) = navController.navigate(route)

            override fun replace(route: Any) {
                val current = navController.currentBackStackEntry?.destination?.id
                navController.navigate(route) { if (current != null) popUpTo(current) { inclusive = true } }
            }

            override fun back() {
                navController.popBackStack()
            }

            override fun top(route: Any) = navController.navigateToTop(route)
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val top = TopDestination.entries.firstOrNull { destination.isOn(it.route::class) }
    val brand = home?.universe?.name ?: session.universe.name
    val firstCitizen = home?.let { h -> listOfNotNull(h.me.id, h.friend?.id).min() } ?: listOfNotNull(session.me.id, session.friend?.id).min()
    val notify = rememberNotifyAccess()
    val leafSurfaces = remember { LeafSurfaces() }

    CompositionLocalProvider(
        LocalAppNav provides nav,
        LocalNotice provides notice,
        LocalNoticeAction provides noticeAction,
        LocalBrand provides brand,
        LocalFirstCitizen provides firstCitizen,
        LocalLeafSurfaces provides leafSurfaces,
    ) {
        Scaffold(
            topBar = {
                when {
                    // Home's title is the universe's name (Universe §4).
                    top == TopDestination.Home -> DlTopAppBar(title = brand, modifier = Modifier.leafCeiling())
                    top != null -> DlTopAppBar(title = stringResource(top.label), modifier = Modifier.leafCeiling())
                }
            },
            bottomBar = {
                if (top != null) {
                    DlNavigationBar(Modifier.leafSurface(kind = SurfaceKind.Ground)) {
                        TopDestination.entries.forEach { item ->
                            val selected = item == top
                            val open = { navController.navigateToTop(item.route) }
                            if (item == TopDestination.You) {
                                // T5: a dot while this device shows no notifications but some are switched on.
                                val wanted = home?.notifyPrefs?.let { it.enabled.isNotEmpty() } ?: true
                                val alert = wanted && notify.permission != NotifyPermission.Granted
                                DlNavigationYouItem(
                                    selected = selected,
                                    onClick = open,
                                    label = stringResource(item.label),
                                    alert = alert,
                                    alertDescription = stringResource(R.string.nav_notifications_off),
                                ) {
                                    val me = home?.me
                                    DlRemoteAvatar(
                                        avatarUrl = me?.avatarUrl ?: session.me.avatarUrl,
                                        name = me?.nickname ?: session.me.nickname,
                                        initials = me?.initials() ?: session.me.nickname.take(1).uppercase(),
                                        signature = signatureOf(session.me.id),
                                        size = 24.dp,
                                    )
                                }
                            } else {
                                val hasNew = when (item) {
                                    TopDestination.Letters -> (home?.unreadCount ?: 0) > 0
                                    TopDestination.Discussions -> (home?.discussionsUnseen ?: 0) > 0
                                    TopDestination.Emails -> (home?.emailsUnread ?: 0) + (home?.emailsReacted ?: 0) > 0
                                    else -> false
                                }
                                DlNavigationItem(
                                    selected = selected,
                                    onClick = open,
                                    label = stringResource(item.label),
                                    icon = item.icon!!,
                                    activeIcon = item.activeIcon!!,
                                    hasNew = hasNew,
                                )
                            }
                        }
                    }
                }
            },
            snackbarHost = { DlSnackbarHost(snackbarHostState) },
            containerColor = MaterialTheme.colorScheme.surface,
        ) { padding ->
            NavHost(navController, startDestination = HomeRoute, modifier = Modifier.padding(padding)) {
                graph(navController)
            }
        }
        // L1: Home and the three lists; the You tab and every opened item have none.
        LeafLayer(leafSurfaces, enabled = leaves, active = top != null && top != TopDestination.You)
        overlay()
    }
}

private fun NavDestination?.isOn(route: KClass<*>): Boolean = this?.hierarchy?.any { it.hasRoute(route) } == true

/** Switch destinations like the M3 bar: single top, back returns to Home, state kept per destination. */
private fun NavHostController.navigateToTop(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
