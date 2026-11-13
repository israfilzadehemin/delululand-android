package com.delulutopia.feature.start

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** The signed-out graph (S0, S1, Create, Join). A session takes the app to the shell. */
@Serializable data object EntryDestination

@Serializable data class PinDestination(val userId: Int)

@Serializable data object CreateDestination

@Serializable data object JoinDestination

fun NavGraphBuilder.startGraph(nav: NavHostController) {
    val toEntry: () -> Unit = {
        nav.navigate(EntryDestination) {
            popUpTo(EntryDestination) { inclusive = true }
            launchSingleTop = true
        }
    }
    composable<EntryDestination> {
        EntryRoute(
            onOpenPin = { nav.navigate(PinDestination(it)) },
            onCreate = { nav.navigate(CreateDestination) },
            onJoin = { nav.navigate(JoinDestination) },
        )
    }
    composable<PinDestination> { PinRoute(onBack = toEntry) }
    composable<CreateDestination> { CreateUniverseRoute(onLeave = toEntry) }
    composable<JoinDestination> { JoinRoute(onLeave = toEntry) }
}
