package com.delulutopia.feature.start

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.network.model.Entered
import com.delulutopia.core.network.model.EntryView
import com.delulutopia.core.network.model.UserCard
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The start screens at the canvas phone frame, compared against S-Landing, S-Cards, S-PIN, S-Welcome. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi", application = Application::class)
class StartScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val moonbase = EntryView(
        name = "Moonbase",
        welcome = Rules.welcomeTemplate("Moonbase"),
        canJoin = true,
        users = listOf(UserCard(3, "Nigar", null, hasPin = true)),
    )

    private fun shot(name: String, dark: Boolean = false, content: @Composable () -> Unit) {
        compose.setContent {
            DelulutopiaTheme(darkTheme = dark) { Surface(color = MaterialTheme.colorScheme.surface) { content() } }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/start_$name.png")
    }

    private fun entry(state: EntryViewModel.UiState) = @Composable {
        EntryScreen(state, {}, {}, {}, {}, {}, {}, {}, {})
    }

    @Test fun landing_wrong() = shot("landing_wrong", content = entry(EntryViewModel.UiState(EntryViewModel.Content.Landing("K7QM-WX4P-2HRN", wrong = true))))

    @Test fun landing_dark() = shot("landing_dark", dark = true, content = entry(EntryViewModel.UiState(EntryViewModel.Content.Landing())))

    @Test fun cards_join() = shot("cards_join", content = entry(EntryViewModel.UiState(EntryViewModel.Content.Cards(moonbase))))

    @Test fun cards_join_dark() = shot("cards_join_dark", dark = true, content = entry(EntryViewModel.UiState(EntryViewModel.Content.Cards(moonbase))))

    @Test fun welcome() = shot(
        "welcome",
        content = entry(EntryViewModel.UiState(EntryViewModel.Content.Cards(moonbase), welcome = Entered("Moonbase", moonbase.welcome))),
    )

    @Test fun pin_wrong() = shot("pin_wrong") {
        PinScreen(
            PinViewModel.UiState(user = UserCard(1, "Emin", null, true), universe = "Delulutopia", problem = PinViewModel.Problem.Wrong),
            {},
            {},
            {},
        )
    }

    @Test fun pin_create_dark() = shot("pin_create_dark", dark = true) {
        PinScreen(
            PinViewModel.UiState(user = UserCard(2, "Leyla", null, false), index = 1, mode = PinViewModel.Mode.Create, digits = "12"),
            {},
            {},
            {},
        )
    }

    @Test fun key_reveal() = shot("key_reveal") { KeyReveal("Moonbase", "K7QMWX4P2HRN") {} }
}
