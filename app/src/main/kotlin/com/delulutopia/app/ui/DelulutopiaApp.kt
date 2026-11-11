package com.delulutopia.app.ui

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
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
import com.delulutopia.app.notify.PendingOpen
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlNavigationBar
import com.delulutopia.core.designsystem.component.DlNavigationItem
import com.delulutopia.core.designsystem.component.DlNavigationYouItem
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSnackbarHost
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.ui.AfterStart
import com.delulutopia.core.ui.AppNav
import com.delulutopia.core.ui.DiscussionRoute
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EmailsRoute
import com.delulutopia.core.ui.HomeRoute
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalFirstCitizen
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.LocalNoticeAction
import com.delulutopia.core.ui.ThreadRoute
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
        DelulutopiaApp(session, state, leaves = leaves, overlay = { AfterPin(state.home?.friend?.nickname) }) {
            composable<HomeRoute> { HomeScreen() }
            lettersGraph()
            discussionsGraph()
            emailsGraph()
            youGraph({ currentTheme }, onThemeModeChange, onLogout)
        }
    }
}

/**
 * What follows a PIN entry once Home is up: S9's notification ask (FS-A14, once per phone), then
 * X1's one-time fingerprint offer; and U9's word when the citizen photo could not be uploaded.
 */
@Composable
private fun AfterPin(friend: String?) {
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val notice = LocalNotice.current
    val access = rememberNotifyAccess()
    val entered = remember {
        val e = Fingerprint.justEntered
        Fingerprint.justEntered = null
        e
    }
    val device = remember { context.getSharedPreferences("device", Context.MODE_PRIVATE) }
    var ask by remember { mutableStateOf(entered != null && access.permission == NotifyPermission.Default && !device.getBoolean("notifyAsked", false)) }
    var offer by remember {
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
    if (ask) {
        NotifyAsk(
            friend = friend,
            onAllow = {
                ask = false
                access.allow()
            },
            onNotNow = {
                ask = false
                device.edit().putBoolean("notifyAsked", true).apply()
            },
        )
        return
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

/** S9 with the Android copy (design §7): Allow asks the system; Not now is remembered on this phone. */
@Composable
private fun NotifyAsk(friend: String?, onAllow: () -> Unit, onNotNow: () -> Unit) {
    DlBottomSheet(onDismissRequest = onNotNow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(DlIcons.Bell, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(stringResource(R.string.notify_ask_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.notify_ask_text, friend ?: stringResource(R.string.your_friend)), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.notify_ask_quiet), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                DlOutlinedButton(stringResource(R.string.not_now), onNotNow)
                DlFilledButton(stringResource(R.string.allow), onAllow)
            }
        }
    }
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
    val pendingOpen by PendingOpen.path.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOpen) {
        val path = pendingOpen ?: return@LaunchedEffect
        PendingOpen.path.value = null
        routeOf(path)?.let { navController.navigate(it) }
    }

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
            // Motion: forward slides in from the right, back from the left, a tab switch rises a little.
            val isTop: (NavDestination) -> Boolean = { d -> TopDestination.entries.any { d.isOn(it.route::class) } }
            NavHost(
                navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(padding),
                enterTransition = {
                    pageIn(
                        if (isTop(initialState.destination) &&
                            isTop(targetState.destination)
                        ) {
                            0
                        } else {
                            32
                        },
                        if (isTop(targetState.destination) && isTop(initialState.destination)) 8 else 0,
                    )
                },
                exitTransition = { fadeOut(tween(PAGE_MS / 2)) },
                popEnterTransition = { pageIn(-32, 0) },
                popExitTransition = { fadeOut(tween(PAGE_MS / 2)) },
            ) {
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

/** "/letters/12", "/discussions/3", "/emails/7" (the push's click targets) as routes. */
internal fun routeOf(path: String): Any? {
    val parts = path.trim('/').split('/')
    val id = parts.getOrNull(1)?.toLongOrNull() ?: return null
    return when (parts[0]) {
        "letters" -> LetterRoute(id)
        "discussions" -> DiscussionRoute(id)
        "emails" -> ThreadRoute(id)
        else -> null
    }
}

private const val PAGE_MS = 300
private val EASE_OUT = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

/** dl-page-in: fade in while moving from [dx], [dy] (dp) to rest. */
private fun pageIn(dx: Int, dy: Int): EnterTransition = fadeIn(tween(PAGE_MS, easing = EASE_OUT)) +
    slide(dx, dy)

private fun slide(dx: Int, dy: Int): EnterTransition = slideIn(tween(PAGE_MS, easing = EASE_OUT)) { full ->
    // dp of the 412 dp canvas phone, scaled to this window.
    val k = full.width / 412f
    IntOffset((dx * k).toInt(), (dy * k).toInt())
}
