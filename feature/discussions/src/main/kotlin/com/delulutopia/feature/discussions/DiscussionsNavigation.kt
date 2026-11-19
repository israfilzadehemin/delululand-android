package com.delulutopia.feature.discussions

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.delulutopia.core.ui.ArchivedRoute
import com.delulutopia.core.ui.ComposeDiscussionRoute
import com.delulutopia.core.ui.DiscussionRoute
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EditDiscussionRoute
import com.delulutopia.core.ui.page

/** S10, Archived, S11, S12 and editing a discussion. */
fun NavGraphBuilder.discussionsGraph() {
    page<DiscussionsRoute> { DiscussionsScreen(archived = false) }
    page<ArchivedRoute> { DiscussionsScreen(archived = true) }
    page<DiscussionRoute> { entry -> DiscussionScreen(entry.toRoute<DiscussionRoute>().id) }
    page<ComposeDiscussionRoute> { ComposeScreen(editId = null) }
    page<EditDiscussionRoute> { entry -> ComposeScreen(editId = entry.toRoute<EditDiscussionRoute>().id) }
}
