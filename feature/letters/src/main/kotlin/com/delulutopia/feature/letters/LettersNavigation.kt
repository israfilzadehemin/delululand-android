package com.delulutopia.feature.letters

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.WriteRoute

/** S6, S7 and S4 (Write lives under Letters). */
fun NavGraphBuilder.lettersGraph() {
    composable<LettersRoute> { LettersScreen() }
    composable<LetterRoute> { entry -> LetterScreen(entry.toRoute<LetterRoute>().id) }
    composable<WriteRoute> { WriteScreen() }
}
