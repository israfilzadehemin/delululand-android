package com.delulutopia.feature.home

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.designsystem.theme.DelulutopiaTheme
import com.delulutopia.core.network.model.Board
import com.delulutopia.core.network.model.BoardComment
import com.delulutopia.core.network.model.Citizen
import com.delulutopia.core.network.model.Home
import com.delulutopia.core.network.model.Mood
import com.delulutopia.core.network.model.Row
import com.delulutopia.core.network.model.UniverseInfo
import com.delulutopia.core.ui.LocalFirstCitizen
import com.github.takahirom.roborazzi.captureRoboImage
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Home at the canvas phone frame (A-Home, A-Home-waiting, A-Home-empty), both themes. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h917dp-xxhdpi", application = Application::class)
class HomeScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private object Fake : HomeActions {
        override val boardMini: StateFlow<Boolean> = MutableStateFlow(false)

        override fun flipped() = Unit

        override fun setBoardMini(mini: Boolean) = Unit

        override suspend fun setMood(key: String, catalogue: List<Mood>) = true

        override suspend fun saveMood(editing: Mood?, emoji: String, label: String) = Result.success(Mood("X", emoji, label, true))

        override suspend fun deleteMood(key: String): String? = null

        override suspend fun cancel(id: Long) = LetterActions.CancelResult.Done

        override fun boardSeen(version: Int) = Unit

        override suspend fun postBoard(png: ByteArray, baseVersion: Int, edit: Boolean) = HomeActions.PostResult.Posted

        override suspend fun react(board: Board, key: String?, friend: String): String? = null

        override suspend fun comment(board: Board, text: String, friend: String): String? = null

        override suspend fun uncomment(board: Board, friend: String): String? = null
    }

    private val now = System.currentTimeMillis()

    private fun at(minutes: Long) = Instant.ofEpochMilli(now + minutes * 60_000).toString()

    private val me = Citizen(1, "Emin", mood = "HAPPY", moodSetAt = at(-120), timezone = "Europe/Warsaw")
    private val leyla = Citizen(2, "Leyla", mood = "CALM", moodSetAt = at(-25), timezone = "Asia/Tehran")

    private fun row(id: Long, direction: String, sent: Long, window: Long, arrives: Long, read: Long? = null, line: String) = Row(
        id = id,
        direction = direction,
        state = "",
        sentAt = at(sent),
        windowEndsAt = at(window),
        arrivesAt = at(arrives),
        readAt = read?.let { at(it) },
        firstLine = line,
    )

    private val full = Home(
        serverNow = at(0),
        me = me,
        friend = leyla,
        from = listOf(
            row(11, "from", -70, -65, -10, line = "Dear Emin, the week went by slower than I expected"),
            row(12, "from", -30, -25, 28, line = "Secret"),
        ),
        to = listOf(
            row(21, "to", -1, 4, 59, line = "I found the kind of notebook you described, the grey one"),
            row(22, "to", -20, -15, 43, line = "On its way"),
            row(23, "to", -90, -85, -30, line = "About the parking spot, and a small confession"),
        ),
        board = Board(3, 2, at(-120), "/api/board/3.png", reaction = "love", comment = BoardComment("Lovely sun!", at(-60))),
        universe = UniverseInfo(1, "Delulutopia", isCreator = true),
    )

    private fun shot(name: String, home: Home?, dark: Boolean = false, offline: Boolean = false) {
        compose.setContent {
            DelulutopiaTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalFirstCitizen provides 1) {
                    Surface(color = MaterialTheme.colorScheme.surface) {
                        HomeScreen(HomeRepository.State(home = home, offline = offline), Fake)
                    }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/home_$name.png")
    }

    @Test fun home_light() = shot("full_light", full)

    @Test fun home_dark() = shot("full_dark", full, dark = true)

    @Test fun waiting() = shot(
        "waiting",
        full.copy(friend = null, from = emptyList(), to = emptyList(), board = null, universe = UniverseInfo(1, "Moonbase", isCreator = true, waiting = true)),
    )

    @Test fun empty_dark() = shot("empty_dark", full.copy(from = emptyList(), to = emptyList(), board = null), dark = true)

    @Test fun loading() = shot("loading", null)

    @Test fun offline() = shot("offline", full, offline = true)
}
