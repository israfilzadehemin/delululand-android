package com.delulutopia.app.e2e

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runAndroidComposeUiTest
import com.delulutopia.app.MainActivity
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Every user flow of the app, end to end against the real API (see [E2E]). Each case starts from a
 * fresh install, signs in as user A and walks one area; user B acts through the API. The stack runs
 * short timers (CI `e2e` job): delay 40 s, window 30 s, discussions 40 s.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h915dp-xxhdpi")
class FlowsE2ETest {
    private fun case(name: String, block: Ui.(() -> MainActivity) -> Unit) {
        E2E.prepare()
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            val ui = Ui(this, name)
            ui.signIn()
            ui.block { activity!! }
            ui.shot("end")
        }
    }

    @Test fun startWrongKeyThenRightKey() {
        E2E.prepare()
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            val ui = Ui(this, "start")
            ui.step("wrong key") {
                ui.until(30_000, "the landing") { ui.sees("Enter your universe") }
                ui.type("Key", "AAAABBBBCCCC")
                ui.tap("Enter", exact = true)
                ui.see("That key doesn")
            }
            ui.step("right key") {
                ui.type("Key", E2E.KEY, replace = true)
                ui.tap("Enter", exact = true)
                ui.until(20_000, "the welcome") { ui.sees("Welcome to") }
                ui.tapLast("Enter")
                ui.see("userone")
                ui.see("usertwo")
            }
            ui.step("wrong PIN") {
                // A PIN exists once any case signed in; on a fresh stack the first case creates it.
                Friend().signIn()
                ui.tap("userone")
                ui.until(20_000, "the PIN screen") { ui.sees("Enter your PIN") || ui.sees("Create your PIN") }
                if (ui.sees("Create your PIN")) {
                    ui.pin(E2E.A_PIN)
                    ui.pin("111111")
                    ui.see("PINs differ")
                    ui.pin(E2E.A_PIN)
                    ui.pin(E2E.A_PIN)
                } else {
                    ui.pin("000000")
                    ui.see("Wrong PIN")
                    ui.pin(E2E.A_PIN)
                }
                ui.home()
            }
        }
    }

    @Test fun moods() = case("moods") {
        step("set a mood") {
            until(20_000, "the mood pill") { sees("Set your mood") || sees(" · set ") }
            tap(if (sees("Set your mood")) "Set your mood" else " · set ")
            see("How do you feel?")
            tap("Happy", exact = true)
            gone("How do you feel?")
            see("Happy · set")
        }
        step("custom mood: the friend's shows up") {
            // The emoji picker is a platform View; the new mood comes from user B, the form is tested on edit.
            Friend().signIn().call("POST", "api/moods", """{"emoji":"🧪","label":"Testing"}""").also {
                check(it.status in 200..299) { "add mood: ${it.status} ${it.body}" }
            }
            pass(2000)
            tap("Happy · set")
            see("How do you feel?")
            repeat(8) { if (!sees("Testing")) tapLabel("More moods") }
            see("Testing")
        }
        step("custom mood: edit and delete") {
            tapLabel("Edit Testing")
            type("Label", "Tested", replace = true)
            tap("Save", exact = true)
            see("Tested")
            tapLabel("Edit Tested")
            tap("Delete mood")
            tap("Yes, delete")
            gone("Tested")
        }
    }

    @Test fun letterSendCancelEditAndReceive() = case("letters") {
        val friend = Friend().signIn()
        step("write and send") {
            tab("Letters")
            until(20_000, "the letter button") { sees("Write a letter") || sees("Continue your draft") }
            tap(if (sees("Continue your draft")) "Continue your draft" else "Write a letter")
            seeLabel("Letter text")
            type("Letter text", "E2E letter one")
            tap("Send", exact = true)
            see("Send this letter?")
            tapLast("Send")
            // Sealed, then Letters: the row with its window.
            until(30_000, "Letters with the sent letter") { sees("to edit") && !seesLabel("Letter text") }
            see("E2E letter one")
        }
        step("take it back") {
            tap("E2E letter one")
            see("left to edit or cancel")
            tap("Cancel", exact = true)
            see("Take it back?")
            tap("Yes, take it back")
            seeLabel("Letter text")
            see("E2E letter one")
        }
        step("send again and edit in the window") {
            tap("Send", exact = true)
            tapLast("Send")
            until(30_000, "Letters with the sent letter") { sees("to edit") && !seesLabel("Letter text") }
            tap("E2E letter one")
            tap("Edit", exact = true)
            see("Editing a sent letter")
            type("Letter text", " edited")
            tap("Send again")
            // Back on the letter, which shows the edit at once (not after the next signal).
            until(10_000, "the edited letter") { sees("edited") && !seesLabel("Letter text") }
        }
        step("receive a letter") {
            friend.sendLetter("E2E letter from two")
            // Delay 40 s; Home re-reads at the arrival.
            tapLabel("Back")
            tab("Home")
            see("E2E letter from two", 90_000)
        }
        step("open it and reply") {
            tap("E2E letter from two")
            see("From usertwo")
            tap("Reply", exact = true)
            seeLabel("Letter text")
            see("REPLYING TO")
            type("Letter text", "E2E reply")
            tap("Send", exact = true)
            tapLast("Send")
            until(30_000, "Letters with the reply") { sees("E2E reply") && !seesLabel("Letter text") }
        }
    }

    @Test fun discussions() = case("discussions") {
        val friend = Friend().signIn()
        step("compose and post") {
            tab("Discussions")
            until(20_000, "the discussion button") { sees("Start a discussion") || sees("Continue your discussion draft") }
            tap(if (sees("Start a discussion")) "Start a discussion" else "Continue your discussion draft")
            type("Title", "E2E topic", replace = true)
            type("Discussion text", "What do we think?")
            tap("Post", exact = true)
            see("Post this discussion?")
            tapLast("Post")
            until(30_000, "the posted discussion") { sees("Write a comment") || seesLabel("Write a comment") }
            see("E2E topic")
            see("What do we think?")
        }
        step("comment, edit, delete") {
            type("Write a comment", "First!")
            tapLast("Post")
            see("First!")
            tapLabel("Comment options")
            tap("Edit", exact = true)
            type("Edit comment", "First, edited", replace = true)
            tap("Save", exact = true)
            see("First, edited")
            tapLabel("Comment options")
            tap("Delete", exact = true)
            tap("Yes, delete")
            gone("First, edited")
        }
        step("react to the discussion") {
            tapLabel("Add a reaction")
            pass(500)
            tap("❤")
            seeLabel("❤")
        }
        step("friend's discussion and comment arrive") {
            val id = friend.postDiscussion("E2E from two", "Hello from two")
            tapLabel("Back")
            see("A discussion is on its way", 30_000)
            see("E2E from two", 90_000)
            friend.comment(id, "Two says hi")
            tap("E2E from two")
            see("Two says hi", 30_000)
        }
        step("archive and unarchive") {
            tapLabel("Discussion options")
            tap("Archive", exact = true)
            see("Archived", 20_000)
            tapLabel("Back")
            tap("Archived")
            see("E2E from two")
            tap("E2E from two")
            tap("Unarchive")
            gone("Unarchive")
        }
        step("edit and delete mine") {
            tapLabel("Back")
            tapLabel("Back")
            tap("E2E topic")
            tapLabel("Discussion options")
            tap("Edit", exact = true)
            type("Title", "E2E topic v2", replace = true)
            tap("Save", exact = true)
            see("E2E topic v2", 30_000)
            see("Edited")
            tapLabel("Discussion options")
            tap("Delete", exact = true)
            tap("Yes, delete")
            gone("E2E topic v2", 30_000)
        }
    }

    @Test fun emails() = case("emails") {
        val friend = Friend().signIn()
        step("new email") {
            tab("Emails")
            until(20_000, "the email button") { sees("New email") || sees("Continue your new email") }
            tap(if (sees("Continue your new email")) "Continue your new email" else "New email")
            type("Subject", "E2E subject", replace = true)
            type("Write an email", "Hello by email")
            tap("Send", exact = true)
            until(30_000, "the new thread") { seesLabel("Write a reply") || sees("Write a reply") }
            see("Hello by email")
            see("E2E subject")
        }
        step("friend replies, live") {
            val items = friend.call("GET", "api/emails/threads").obj["items"]!!.jsonArray
            val id = items.first { it.jsonObject["subject"]!!.jsonPrimitive.content == "E2E subject" }.jsonObject["id"]!!.jsonPrimitive.long
            friend.reply(id, "Reply from two")
            see("Reply from two", 30_000)
        }
        step("reply") {
            type("Write a reply", "My reply")
            tapLast("Send")
            see("My reply", 30_000)
        }
        step("friend's email arrives") {
            friend.email("E2E from two", "Mail from two")
            tapLabel("Back")
            see("E2E from two", 30_000)
            tap("E2E from two")
            see("Mail from two")
        }
    }

    @Test fun you() = case("you") {
        step("profile") {
            tab("You")
            tap("View profile")
            type("Name", "Ada", replace = true)
            tap("Save", exact = true)
            see("Profile saved")
        }
        step("notifications") {
            tapLabel("Back")
            tap("Notifications")
            see("Pause all")
            tap("Pause all")
            see("Nothing is pushed")
            tap("Pause all")
            see("Stop every push")
        }
        step("appearance") {
            tapLabel("Back")
            tap("Appearance")
            tap("Dark")
            pass(500)
            tap("Light")
            tapLabel("Back")
        }
        step("universe") {
            tap("Universe")
            see("WELCOME MESSAGE")
            tapLabel("Back")
        }
        step("change PIN and back") {
            tap("Change PIN")
            pin(E2E.A_PIN)
            pin(E2E.A_PIN_2)
            pin(E2E.A_PIN_2)
            see("PIN changed", 30_000)
            gone("PIN changed")
            tap("Change PIN")
            see("Current PIN")
            pin(E2E.A_PIN_2)
            pin(E2E.A_PIN)
            pin(E2E.A_PIN)
            see("PIN changed", 30_000)
            gone("PIN changed")
        }
        step("log out and in") {
            tap("Log out")
            tapLast("Log out")
            see("userone", 30_000)
            tap("userone")
            see("Enter your PIN")
            pin(E2E.A_PIN)
            home()
        }
    }

    @Test fun board() = case("board") {
        step("board shows") {
            until(20_000, "the board") { seesLabel("Draw") || sees("Draw the first doodle") || sees("The board is empty") || seesLabel("Board") }
            shot("board")
        }
    }
}
