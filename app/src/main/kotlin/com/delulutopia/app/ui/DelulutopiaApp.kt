package com.delulutopia.app.ui

import android.content.Context
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import com.delulutopia.core.i18n.t
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
import com.delulutopia.core.ui.LocalPageFrame
import com.delulutopia.core.ui.PageMotion
import com.delulutopia.core.ui.StackPage
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
import com.delulutopia.core.ui.page
import com.delulutopia.core.ui.reactions.LocalReactionsApi
import com.delulutopia.core.ui.tintOf
import com.delulutopia.feature.discussions.discussionsGraph
import com.delulutopia.feature.emails.emailsGraph
import com.delulutopia.feature.home.HomeScreen
import com.delulutopia.feature.letters.lettersGraph
import com.delulutopia.feature.you.youGraph
import kotlin.reflect.KClass
import kotlinx.coroutines.launch

/** The five destinations of the navigation bar (Y-A1). */
enum class TopDestination(val route: Any, val label: String, val icon: ImageVector?, val activeIcon: ImageVector?) {
    Home(HomeRoute, "tabs.home", DlIcons.Home, DlIcons.HomeActive),
    Letters(LettersRoute, "tabs.letters", DlIcons.Letters, DlIcons.LettersActive),
    Discussions(DiscussionsRoute, "tabs.discussions", DlIcons.Discussions, DlIcons.DiscussionsActive),
    Emails(EmailsRoute, "tabs.emails", DlIcons.Emails, DlIcons.EmailsActive),
    You(YouRoute, "tabs.you", null, null),
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
    val activity = LocalActivity.current
    DisposableEffect(session.me.id) {
        shell.enter()
        // A rotation recomposes the shell; only a real sign-out (or another citizen) forgets the session.
        onDispose { if (activity?.isChangingConfigurations != true) shell.leave() }
    }
    LifecycleStartEffect(Unit) {
        shell.foreground()
        onStopOrDispose { shell.background() }
    }
    CompositionLocalProvider(LocalReactionsApi provides shell.reactions) {
        DelulutopiaApp(session, state, leaves = leaves, overlay = { AfterPin(state.home?.friend?.nickname) }) {
            page<HomeRoute> { HomeScreen() }
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
            notice(t("universe.photoNotAdded"))
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
    val yes = t("settings.fingerprintOfferYes")
    DlDialog(
        title = t("settings.fingerprintOfferTitle"),
        text = t("settings.fingerprintOfferText"),
        confirmLabel = yes,
        onConfirm = {
            close()
            if (activity != null) Fingerprint.turnOn(activity, pending.first, pending.second, yes) { }
        },
        dismissLabel = t("notify.notNow"),
        onDismiss = close,
    )
}

/** S9 with the Android copy (design §7): Allow asks the system; Not now is remembered on this phone. */
@Composable
private fun NotifyAsk(friend: String?, onAllow: () -> Unit, onNotNow: () -> Unit) {
    DlBottomSheet(onDismissRequest = onNotNow) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(DlIcons.Bell, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Text(t("notify.knowTheMomentALetter"), style = MaterialTheme.typography.titleLarge)
            Text(t("notify.askTextPhone", "friend" to (friend ?: t("settings.yourFriend"))), style = MaterialTheme.typography.bodyMedium)
            Text(t("notify.askQuiet"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)) {
                DlOutlinedButton(t("notify.notNow"), onNotNow)
                DlFilledButton(t("notify.allow"), onAllow)
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

            /**
             * After an action (sent, posted): that list itself, fresh. Not the tab bar's restore,
             * which brought back the tab's saved stack — the Write or Compose screen just left. When
             * the list is under it on the stack the finished page slides away to reveal it.
             */
            override fun top(route: Any) {
                if (navController.popBackStack(route, inclusive = false)) return
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id)
                    launchSingleTop = true
                }
            }
        }
    }

    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val top = topOf(destination)
    // The pages on screen: one at rest, two while one slides over, off or beside the other.
    val visible by navController.visibleEntries.collectAsStateWithLifecycle()
    val rootsOnly = visible.all { topOf(it.destination) != null }
    // The bar belongs to the tab pages: shown while one is on screen, lit for the tab under the stack.
    val barTab = top ?: visible.firstNotNullOfOrNull { topOf(it.destination) }
    val brand = home?.universe?.name ?: session.universe.name
    // The creator is the first citizen (whoOf); public ids carry no order.
    val firstCitizen = home?.let { h -> creatorOf(h.me.id, h.friend?.id, h.universe.isCreator) }
        ?: creatorOf(session.me.id, session.friend?.id, session.universe.isCreator)
    val notify = rememberNotifyAccess()
    val leafSurfaces = remember { LeafSurfaces() }
    val pendingOpen by PendingOpen.path.collectAsStateWithLifecycle()
    LaunchedEffect(pendingOpen) {
        val path = pendingOpen ?: return@LaunchedEffect
        PendingOpen.path.value = null
        routeOf(path)?.let { navController.navigate(it) }
    }
    val density = LocalDensity.current
    var barPx by remember { mutableIntStateOf(0) }
    val barHeight = with(density) { barPx.toDp() }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val frame: @Composable (NavBackStackEntry, @Composable () -> Unit) -> Unit = { entry, content ->
        when (val tab = topOf(entry.destination)) {
            // Home's title is the universe's name (Universe §4).
            null -> StackPage(content = content)
            else -> TabPage(if (tab == TopDestination.Home) brand else t(tab.label), barHeight, content)
        }
    }

    CompositionLocalProvider(
        LocalAppNav provides nav,
        LocalNotice provides notice,
        LocalNoticeAction provides noticeAction,
        LocalBrand provides brand,
        LocalFirstCitizen provides firstCitizen,
        LocalLeafSurfaces provides leafSurfaces,
        LocalPageFrame provides frame,
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
            if (barTab != null) {
                // Under the pages while one slides over them (it covers the bar too); on top at rest and between tabs.
                DlNavigationBar(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .zIndex(if (rootsOnly) 1f else 0f)
                        .onSizeChanged { barPx = it.height }
                        .leafSurface(kind = SurfaceKind.Ground),
                ) {
                    TopDestination.entries.forEach { item ->
                        val selected = item == barTab
                        val open = { navController.navigateToTop(item.route) }
                        if (item == TopDestination.You) {
                            // T5: a dot while this device shows no notifications but some are switched on.
                            val wanted = home?.notifyPrefs?.let { it.enabled.isNotEmpty() } ?: true
                            val alert = wanted && notify.permission != NotifyPermission.Granted
                            DlNavigationYouItem(
                                selected = selected,
                                onClick = open,
                                label = t(item.label),
                                alert = alert,
                                alertDescription = t("notify.hintOff"),
                            ) {
                                val me = home?.me
                                DlRemoteAvatar(
                                    avatarUrl = me?.avatarUrl ?: session.me.avatarUrl,
                                    name = me?.nickname ?: session.me.nickname,
                                    initials = me?.initials() ?: session.me.nickname.take(1).uppercase(),
                                    signature = tintOf(session.me.id),
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
                                label = t(item.label),
                                icon = item.icon!!,
                                activeIcon = item.activeIcon!!,
                                hasNew = hasNew,
                            )
                        }
                    }
                }
            }
            // Motion (PageMotion): tab to tab the pages slide side by side toward the picked tab;
            // deeper pages slide over the stack and back off it (predictive back drags the same slide).
            NavHost(
                navController,
                startDestination = HomeRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabIn(tabs, rtl) else PageMotion.push(rtl)
                },
                exitTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabOut(tabs, rtl) else PageMotion.cover()
                },
                popEnterTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabIn(tabs, rtl) else PageMotion.uncover()
                },
                popExitTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabOut(tabs, rtl) else PageMotion.pop(rtl)
                },
                // Email Bubbles BK1: the edge swipe drags the same slide as ← (without these, Navigation's
                // default predictive pop shrinks the page toward the centre and drops it on release).
                predictivePopEnterTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabIn(tabs, rtl) else PageMotion.uncover()
                },
                predictivePopExitTransition = {
                    val tabs = tabStep(initialState, targetState)
                    if (tabs != null) PageMotion.tabOut(tabs, rtl) else PageMotion.pop(rtl)
                },
            ) {
                graph(navController)
            }
            DlSnackbarHost(
                snackbarHostState,
                Modifier
                    .align(Alignment.BottomCenter)
                    .zIndex(2f)
                    .then(if (top != null) Modifier.padding(bottom = barHeight) else Modifier.navigationBarsPadding()),
            )
        }
        // L1: Home and the three lists; the You tab and every opened item have none.
        LeafLayer(leafSurfaces, enabled = leaves, active = top != null && top != TopDestination.You)
        overlay()
    }
}

/**
 * A tab page: its top bar (it moves with the page) over the content, and a clear strip at the
 * bottom where the shell's navigation bar shows through. The content treats the bars' space as
 * consumed, so a screen's imePadding lifts only by the part of the keyboard the bar does not cover.
 */
@Composable
private fun TabPage(title: String, barHeight: Dp, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = title, modifier = Modifier.leafCeiling())
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .consumeWindowInsets(WindowInsets.statusBars)
                .consumeWindowInsets(PaddingValues(bottom = barHeight))
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
        ) { content() }
        Spacer(Modifier.fillMaxWidth().height(barHeight))
    }
}

private fun topOf(destination: NavDestination?): TopDestination? = TopDestination.entries.firstOrNull { destination.isOn(it.route::class) }

/** Between two tabs: whether the target lies toward the end of the bar; null when a page is pushed or popped. */
private fun tabStep(from: NavBackStackEntry, to: NavBackStackEntry): Boolean? {
    val a = topOf(from.destination) ?: return null
    val b = topOf(to.destination) ?: return null
    return b.ordinal > a.ordinal
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

/** "/letters/<id>", "/discussions/<id>", "/emails/<id>" (the push's click targets) as routes. */

/** The first citizen: me when I made the universe (or live alone in it), else the friend. */
internal fun creatorOf(me: String, friend: String?, iAmCreator: Boolean?): String = if (friend == null || iAmCreator != false) me else friend

/** A push or widget link to its screen; the id is a public id, used as given (doc "Delululand — Public IDs"). */
internal fun routeOf(path: String): Any? {
    val parts = path.trim('/').split('/')
    val id = parts.getOrNull(1)?.takeIf { it.isNotEmpty() } ?: return null
    return when (parts[0]) {
        "letters" -> LetterRoute(id)
        "discussions" -> DiscussionRoute(id)
        "emails" -> ThreadRoute(id)
        else -> null
    }
}
