package com.delulutopia.feature.letters

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.delulutopia.core.data.LettersRepository
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.LetterMedia
import com.delulutopia.core.network.model.Row
import com.delulutopia.core.ui.LocalFirstCitizen
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** S6 at the canvas phone frame (C-Letters, C-Letters-list, C-Letters-empty). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi", application = Application::class)
class LettersScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val now = System.currentTimeMillis()

    private fun at(minutes: Long) = Instant.ofEpochMilli(now + minutes * 60_000).toString()

    private val me = Citizen(1, "Emin", draftEmpty = false)
    private val leyla = Citizen(2, "Leyla")

    private val rows = listOf(
        Row(1, "to", "", at(-1), at(4), at(59), firstLine = "I found the kind of notebook you described"),
        Row(2, "from", "", at(-80), at(-75), at(-20), firstLine = "Dear Emin, the week went by slower than I expected", media = LetterMedia(photos = 2)),
        Row(3, "from", "", at(-10), at(-5), at(50), firstLine = "Secret"),
        Row(4, "to", "", at(-300), at(-295), at(-240), readAt = at(-200), firstLine = "About the parking spot", replyToId = 2),
        Row(5, "from", "", at(-900), at(-895), at(-840), readAt = at(-800), firstLine = "", media = LetterMedia(audios = 1)),
        Row(6, "to", "", at(-30), at(-25), at(-1), firstLine = "Delivered but not read"),
    )

    private fun shot(name: String, state: LettersRepository.State, list: Boolean, dark: Boolean = false) {
        compose.setContent {
            DelulutopiaTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalFirstCitizen provides 1) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        LettersList(state, me, leyla, list, {}, {})
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/letters_$name.png")
    }

    @Test fun grid() = shot("grid", LettersRepository.State(head = rows), list = false)

    @Test fun grid_dark() = shot("grid_dark", LettersRepository.State(head = rows), list = false, dark = true)

    @Test fun list() = shot("list", LettersRepository.State(head = rows), list = true)

    @Test fun empty() = shot("empty", LettersRepository.State(head = emptyList()), list = false)
}
