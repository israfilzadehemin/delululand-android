package com.delulutopia.feature.emails

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.delulutopia.core.ui.EmailsRoute
import com.delulutopia.core.ui.NewEmailRoute
import com.delulutopia.core.ui.ThreadRoute
import com.delulutopia.core.ui.page

/** S13, S14, S15. */
fun NavGraphBuilder.emailsGraph() {
    page<EmailsRoute> { EmailsScreen() }
    page<ThreadRoute> { entry -> ThreadScreen(entry.toRoute<ThreadRoute>().id) }
    page<NewEmailRoute> { NewEmailScreen() }
}
