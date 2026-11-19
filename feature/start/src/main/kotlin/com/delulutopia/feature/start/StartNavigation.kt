package com.delulutopia.feature.start

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import com.delulutopia.core.ui.page
import kotlinx.serialization.Serializable

/** The signed-out graph (S0, S1, Create, Join). A session takes the app to the shell. */
@Serializable data object EntryDestination

@Serializable data class PinDestination(val userId: String)

@Serializable data object CreateDestination

@Serializable data object JoinDestination

fun NavGraphBuilder.startGraph(nav: NavHostController) {
    // Back to the key and cards: the page on top slides off the stack (a fresh Entry if it is not there).
    val toEntry: () -> Unit = {
        if (!nav.popBackStack(EntryDestination, inclusive = false)) {
            nav.navigate(EntryDestination) {
                popUpTo(EntryDestination) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    page<EntryDestination> {
        EntryRoute(
            onOpenPin = { nav.navigate(PinDestination(it)) },
            onCreate = { nav.navigate(CreateDestination) },
            onJoin = { nav.navigate(JoinDestination) },
        )
    }
    page<PinDestination> { PinRoute(onBack = toEntry) }
    page<CreateDestination> { CreateUniverseRoute(onLeave = toEntry) }
    page<JoinDestination> { JoinRoute(onLeave = toEntry) }
}
