package com.delulutopia.feature.letters

import androidx.navigation.NavGraphBuilder
import androidx.navigation.toRoute
import com.delulutopia.core.ui.LetterRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.WriteRoute
import com.delulutopia.core.ui.page

/** S6, S7 and S4 (Write lives under Letters). */
fun NavGraphBuilder.lettersGraph() {
    page<LettersRoute> { LettersScreen() }
    page<LetterRoute> { entry -> LetterScreen(entry.toRoute<LetterRoute>().id) }
    page<WriteRoute> { WriteScreen() }
}
