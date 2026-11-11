package com.delulutopia.feature.letters

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LettersRoute

/** S6 and S7. */
fun NavGraphBuilder.lettersGraph() {
    composable<LettersRoute> { LettersScreen() }
    composable<LetterRoute> { entry -> LetterScreen(entry.toRoute<LetterRoute>().id) }
}
