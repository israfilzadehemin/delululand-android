package com.delulutopia.feature.discussions

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.delulutopia.core.ui.ArchivedRoute
import com.delulutopia.core.ui.ComposeDiscussionRoute
import com.delulutopia.core.ui.DiscussionRoute
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EditDiscussionRoute

/** S10, Archived, S11, S12 and editing a discussion. */
fun NavGraphBuilder.discussionsGraph() {
    composable<DiscussionsRoute> { DiscussionsScreen(archived = false) }
    composable<ArchivedRoute> { DiscussionsScreen(archived = true) }
    composable<DiscussionRoute> { entry -> DiscussionScreen(entry.toRoute<DiscussionRoute>().id) }
    composable<ComposeDiscussionRoute> { ComposeScreen(editId = null) }
    composable<EditDiscussionRoute> { entry -> ComposeScreen(editId = entry.toRoute<EditDiscussionRoute>().id) }
}
