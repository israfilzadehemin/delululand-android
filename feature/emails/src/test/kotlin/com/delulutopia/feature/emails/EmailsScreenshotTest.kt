package com.delulutopia.feature.emails

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.ThreadMedia
import com.delulutopia.core.network.model.ThreadRow
import com.delulutopia.core.network.model.UniverseInfo
import com.delulutopia.core.ui.LocalFirstCitizen
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** S13 at the canvas phone frame (E-Emails, E-Emails-empty, E-No-credits). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi", application = Application::class)
class EmailsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val now = System.currentTimeMillis()

    private fun at(minutes: Long) = Instant.ofEpochMilli(now + minutes * 60_000).toString()

    private fun home(left: Int) = Home(
        serverNow = at(0),
        me = Citizen(1, "Emin"),
        friend = Citizen(2, "Leyla"),
        emailCredits = EmailCredits(10, 10 - left, left, at(60 * 24 * 3)),
        universe = UniverseInfo(1, "Delulutopia", isCreator = true),
    )

    private val rows = listOf(
        ThreadRow(1, "Weekend plans", 1, at(-300), at(-5), lastSenderId = 2, lastText = "Saturday works, let's do the market", emailCount = 4, unread = 1),
        ThreadRow(2, "Photos from the trip", 2, at(-3000), at(-900), lastSenderId = 1, lastMedia = ThreadMedia(photos = 3), emailCount = 2),
        ThreadRow(3, "Recipe", 1, at(-9000), at(-8000), lastSenderId = 2, lastText = "The secret is the cardamom", emailCount = 7),
    )

    private fun shot(name: String, rows: List<ThreadRow>, left: Int = 7, dark: Boolean = false) {
        compose.setContent {
            DelulutopiaTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalFirstCitizen provides 1) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        EmailsList(rows, hasMore = false, draftEmpty = true, home = home(left), onMore = {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/emails_$name.png")
    }

    @Test fun list() = shot("list", rows)

    @Test fun list_dark() = shot("list_dark", rows, dark = true)

    @Test fun empty() = shot("empty", emptyList())

    @Test fun no_credits() = shot("no_credits", rows, left = 0)
}
