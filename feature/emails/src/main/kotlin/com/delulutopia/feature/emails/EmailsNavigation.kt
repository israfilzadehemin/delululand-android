package com.delulutopia.feature.emails

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.delulutopia.core.ui.EmailsRoute
import com.delulutopia.core.ui.NewEmailRoute
import com.delulutopia.core.ui.ThreadRoute

/** S13, S14, S15. */
fun NavGraphBuilder.emailsGraph() {
    composable<EmailsRoute> { EmailsScreen() }
    composable<ThreadRoute> { entry -> ThreadScreen(entry.toRoute<ThreadRoute>().id) }
    composable<NewEmailRoute> { NewEmailScreen() }
}
