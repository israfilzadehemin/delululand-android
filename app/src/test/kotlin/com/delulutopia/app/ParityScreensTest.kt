package com.delulutopia.app

import android.content.Context
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Design parity: the real app (Hilt graph, network, navigation) against the fixture API in
 * tools/parity/server.mjs, the same data the web app is screenshotted with. CI starts the fixture
 * before the tests; without it these tests are skipped. Shots land in build/outputs/roborazzi/parity_*.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h1400dp-xxhdpi")
class ParityScreensTest {
    private val server = "http://127.0.0.1:8080/"

    private fun up(): Boolean = runCatching {
        (URL(server + "api/config").openConnection() as HttpURLConnection).run {
            connectTimeout = 1000
            responseCode == 200
        }
    }.getOrDefault(false)

    private fun mock(query: String) {
        (URL(server + "__mock/state?$query").openConnection() as HttpURLConnection).run {
            requestMethod = "POST"
            responseCode
        }
    }

    private fun prepare(signedIn: Boolean) {
        assumeTrue("parity fixture not running", up())
        System.setProperty("dl.baseUrl", server)
        System.setProperty("dl.motion", "off")
        mock(if (signedIn) "auth=1" else "auth=0")
        val context = ApplicationProvider.getApplicationContext<Context>()
        // Static frames: no falling leaves, no first-run notification ask.
        context.getSharedPreferences("device", Context.MODE_PRIVATE).edit().putBoolean("leaves", false).putBoolean("notifyAsked", true).commit()
    }

    private fun ComposeUiTest.waitFor(text: String, substring: Boolean = true) {
        runCatching { waitUntil(timeoutMillis = 20_000) { onAllNodes(hasText(text, substring = substring)).fetchSemanticsNodes().isNotEmpty() } }
            .onFailure { println("parity: never saw \"$text\"") }
        waitForIdle()
    }

    private fun ComposeUiTest.tap(text: String) {
        runCatching { onAllNodes(hasText(text, substring = true) and hasClickAction()).onFirstOrSelf().performClick() }
            .onFailure { println("parity: can't tap \"$text\"") }
        waitForIdle()
    }

    private fun ComposeUiTest.tapLabel(label: String) {
        runCatching { onAllNodes(hasContentDescription(label, substring = true) and hasClickAction()).onFirstOrSelf().performClick() }
            .onFailure { println("parity: can't tap [$label]") }
        waitForIdle()
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.onFirstOrSelf() = this[0]

    private fun ComposeUiTest.shot(name: String, settle: Long = 600) {
        mainClock.advanceTimeBy(settle)
        waitForIdle()
        runCatching { onRoot().captureRoboImage("build/outputs/roborazzi/parity_$name.png") }.onFailure { println("parity: no shot $name: $it") }
    }

    private fun ComposeUiTest.back(activity: () -> MainActivity) {
        activity().onBackPressedDispatcher.onBackPressed()
        waitForIdle()
    }

    private fun app(block: ComposeUiTest.(() -> MainActivity) -> Unit) {
        prepare(signedIn = true)
        runAndroidComposeUiTest<MainActivity> {
            waitFor("FROM LEYLA")
            block { activity!! }
        }
    }

    @Test fun home() = app {
        shot("home", 1500)
        tap("Happy")
        shot("mood", 1200)
    }

    @Test fun letters() = app { act ->
        tap("Letters")
        waitFor("That is everything")
        shot("letters")
        tapLabel("Show as list")
        shot("letterlist")
        tapLabel("Show as thumbnails")
        tap("Dear Emin, the week")
        waitFor("Write soon")
        shot("letter_from", 1500)
        back(act)
        tapLabel("Continue your draft")
        waitFor("the trains are booked")
        shot("write", 1200)
        back(act)
        tap("Saturday works for me")
        waitFor("left to edit or cancel")
        shot("letter_mine", 1500)
    }

    @Test fun discussions() = app { act ->
        tap("Discussions")
        waitFor("Weekend in Zakopane")
        shot("discussions")
        tap("Weekend in Zakopane")
        waitFor("The 7:10 one")
        shot("discussion", 1200)
        back(act)
        tap("Books for the winter")
        waitFor("A list, in no order")
        shot("discussion_mine", 1200)
        back(act)
        tapLabel("Start a discussion")
        tapLabel("Continue your discussion draft")
        shot("compose", 1200)
    }

    @Test fun emails() = app { act ->
        tap("Emails")
        waitFor("Tickets for Saturday")
        shot("emails")
        tap("Tickets for Saturday")
        waitFor("Row 7, seats 12")
        shot("thread", 1200)
        back(act)
        tapLabel("New email")
        shot("newemail", 1200)
    }

    @Test fun you() = app { act ->
        tap("You")
        waitFor("of 8 on")
        waitFor("View profile")
        shot("you")
        tap("View profile")
        waitFor("Israfilzade")
        shot("profile")
        back(act)
        tap("Appearance")
        shot("appearance")
        back(act)
        tap("Notifications")
        waitFor("Pause all")
        shot("notifications")
        back(act)
        tap("Universe")
        waitFor("Welcome message")
        shot("universe")
    }

    @Test fun signedOut() {
        prepare(signedIn = false)
        runAndroidComposeUiTest<MainActivity> {
            waitFor("Emin")
            shot("cards", 1200)
            tap("Emin")
            waitFor("Enter your PIN")
            shot("pin", 1200)
        }
    }
}
