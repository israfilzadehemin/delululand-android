package com.delulutopia.app

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.delulutopia.app.ui.DelulutopiaApp
import com.delulutopia.app.ui.PlaceholderScreen
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.network.model.Person
import com.delulutopia.core.network.model.UniverseInfo
import com.delulutopia.core.ui.DiscussionsRoute
import com.delulutopia.core.ui.EmailsRoute
import com.delulutopia.core.ui.HomeRoute
import com.delulutopia.core.ui.LettersRoute
import com.delulutopia.core.ui.YouRoute
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The shell at the canvas phone frame (412 × 917 dp), both themes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi", application = Application::class)
class AppShellScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var nav: NavHostController

    @Composable
    private fun Shell(dark: Boolean) {
        nav = rememberNavController()
        DelulutopiaTheme(darkTheme = dark) {
            DelulutopiaApp(session = SAMPLE, state = HomeRepository.State(), navController = nav) {
                composable<HomeRoute> { PlaceholderScreen() }
                composable<LettersRoute> { PlaceholderScreen() }
                composable<DiscussionsRoute> { PlaceholderScreen() }
                composable<EmailsRoute> { PlaceholderScreen() }
                composable<YouRoute> { PlaceholderScreen() }
            }
        }
    }

    private fun capture(name: String, dark: Boolean, start: Any? = null) {
        compose.setContent { Shell(dark) }
        compose.waitForIdle()
        if (start != null) {
            compose.runOnIdle { nav.navigate(start) }
            compose.waitForIdle()
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/app_$name.png")
    }

    @Test fun home_light() = capture("home_light", dark = false)

    @Test fun home_dark() = capture("home_dark", dark = true)

    @Test fun letters_light() = capture("letters_light", dark = false, start = LettersRoute)

    @Test fun letters_dark() = capture("letters_dark", dark = true, start = LettersRoute)

    private companion object {
        val SAMPLE = HomeSession(
            me = Person(1, "Emin"),
            friend = Person(2, "Leyla"),
            universe = UniverseInfo(1, "Delulutopia", isCreator = true),
        )
    }
}
