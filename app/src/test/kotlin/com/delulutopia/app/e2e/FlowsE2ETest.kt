package com.delulutopia.app.e2e

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runAndroidComposeUiTest
import androidx.test.core.app.ApplicationProvider
import com.delulutopia.app.MainActivity
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
// Tall enough that Home's letter rows sit inside what the LazyColumn composes, under both boards.
@Config(qualifiers = "w412dp-h1600dp-xxhdpi")
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

    /**
     * A case in a universe of its own (made after [E2E.prepare], through an SQL invite and the API),
     * signed in as [who] (the creator by default). The case gets the universe as its receiver's [u].
     */
    private fun fresh(
        name: String,
        tier: String = "premium",
        who: (Universe) -> Citizen = { it.a },
        block: Ui.(Universe, () -> MainActivity) -> Unit,
    ) {
        E2E.prepare()
        Db.require()
        val u = Universe.fresh(tier)
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            val ui = Ui(this, name)
            val c = who(u)
            ui.signIn(c.username, u.key, c.pin)
            ui.block(u) { activity!! }
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
        step("live: the friend's mood shows within seconds") {
            // Home polls every 15 s without the stream; the stream brings it at once.
            Friend().signIn().setMood("CALM")
            see("Calm", 8_000)
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
            // The emoji sheet (keyboard-sized cells) over the mood form; this flow stays under runTest's
            // 1-minute clock, which a dialog late in a long flow trips.
            tapLabel("Change it")
            see("Choose an emoji")
            pass(2_000) // the platform picker loads its emoji data off the main thread
            shot("emoji_sheet")
            closeSheet()
            gone("Choose an emoji")
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
            // Comments taller than the screen once squashed the note to 0 dp (only comments showed).
            repeat(6) { friend.comment(id, "Filler $it: " + "a long comment that wraps over several lines on the note. ".repeat(5)) }
            friend.comment(id, "Two says hi")
            tap("E2E from two")
            see("Two says hi", 30_000)
            seeShown("E2E from two")
            seeShown("Hello from two")
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
            val id = items.first { it.jsonObject["subject"]!!.jsonPrimitive.content == "E2E subject" }.jsonObject["id"]!!.jsonPrimitive.content
            friend.reply(id, "Reply from two")
            see("Reply from two", 30_000)
        }
        step("reply") {
            type("Write a reply", "My reply")
            // Email Bubbles EB8: the docked bar's Send is an icon button.
            tapLabel("Send", exact = true)
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

    // ---- Flows added 21 Nov 2026 (doc "Delululand — Android Build" §4). Each runs in a universe of its
    // own unless it needs universe 1's fixed citizens; the CI-only database hook is [Db].

    private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.list(key: String): List<JsonObject> = (this[key] as? JsonArray)?.map { it.jsonObject }.orEmpty()

    private fun today(kind: String, userId: Int, used: Int) = Db.exec(
        "insert into media_usage (user_id, day, kind, used) values (?, (now() at time zone 'utc')::date, ?, ?) " +
            "on conflict (user_id, day, kind) do update set used = excluded.used",
        userId,
        kind,
        used,
    )

    @Test fun boardDrawReactCommentEditAndCap() = fresh("board-flow", tier = "free") { u, _ ->
        val b = u.friend()
        val a = u.friend(u.a)
        val aId = a.me()
        var v = 0
        step("draw the first doodle") {
            tap("Draw the first doodle")
            see("Draw on the board")
            draw("Empty board")
            tapLast("Post")
            gone("Draw on the board", 30_000)
            seeLabel("Board: your doodle")
            until(20_000, "the friend sees it") { b.board()?.str("authorId") == aId }
            v = b.board()!!["version"]!!.jsonPrimitive.int
        }
        step("the friend reacts and comments") {
            b.must("PUT", "api/board/reaction", """{"version":$v,"reaction":"love"}""")
            see("${u.b!!.username} reacted", 20_000)
            b.must("PUT", "api/board/comment", """{"version":$v,"text":"Lovely lines"}""")
            see("Lovely lines", 20_000)
        }
        step("Free: one board doodle a day") {
            see("No doodles left today")
            val refused = a.upload("api/board", Friend.Multipart(mapOf("baseVersion" to "$v", "edit" to "false"), "board.png", "image/png"))
            check(refused.status == 409 && refused.body.contains("DOODLE_DAILY_LIMIT")) { "cap: ${refused.status} ${refused.body}" }
        }
        step("next day: an edit clears the reaction and the comment") {
            Db.exec("delete from media_usage where user_id = ? and kind = 'board_doodle'", u.userId(u.a))
            b.setMood("CALM") // a signal: Home re-reads the plan
            gone("No doodles left today", 20_000)
            tapLabel("Edit your doodle")
            see("Edit the board")
            draw("The doodle you are editing")
            tapLast("Post")
            gone("Edit the board", 30_000)
            gone("Lovely lines", 20_000)
            until(20_000, "a new version without reaction or comment") {
                val now = b.board()!!
                now["version"]!!.jsonPrimitive.int > v && now["reaction"] is JsonNull && now["comment"] is JsonNull
            }
            v = b.board()!!["version"]!!.jsonPrimitive.int
        }
        step("the friend draws; my comment on it, then deleted") {
            b.uploadOk("api/board", Friend.Multipart(mapOf("baseVersion" to "$v", "edit" to "false"), "board.png", "image/png"), "friend draws")
            seeLabel("Board: ${u.b!!.username}'s doodle", 20_000)
            type("Comment on", "So good")
            tapLabel("Post comment")
            see("So good")
            until(10_000, "the comment on the API") { b.board()!!.obj("comment")?.str("text") == "So good" }
            tapLabel("Delete your comment")
            gone("So good")
            until(10_000, "no comment on the API") { b.board()!!["comment"] is JsonNull }
        }
    }

    @Test fun uploadsPickerCameraAndTierLimits() = fresh("uploads") { u, activity ->
        val b = u.friend()
        val a = u.friend(u.a)
        step("compose an email with every kind") {
            tab("Emails")
            tap("New email")
            type("Subject", "Media mail", replace = true)
            type("Write an email", "All the kinds")
            tapLabel("Add photo")
            Media.pick(activity(), "photo.jpg")
            tapLabel("Camera", exact = true)
            tap("Take a photo")
            Media.shoot(activity(), "photo.jpg")
            tapLabel("Attach audio file")
            Media.pick(activity(), "voice.m4a")
            tapLabel("Pick a video")
            Media.pick(activity(), "clip.mp4")
            // Progress: the ring shows while a file is in the air or the server converts it.
            var progress = false
            until(90_000, "every upload done") {
                if (seesLabel("Uploading") || seesLabel("Processing")) progress = true
                seesLabel("Photo 2 of 2") && seesLabel("Play voice note") && seesLabel("Remove video") &&
                    !seesLabel("Uploading") && !seesLabel("Processing")
            }
            trace("progress ring seen: $progress")
            tap("Send", exact = true)
            until(30_000, "the new thread") { seesLabel("Write a reply") || sees("Write a reply") }
        }
        step("the friend receives every file") {
            val thread = b.json("api/emails/threads").list("items").first { it.str("subject") == "Media mail" }
            val atts = b.thread(thread.str("id")!!).last().list("attachments").map { it.str("kind") }.sortedBy { it }
            check(atts == listOf("audio", "photo", "photo", "video")) { "attachments: $atts" }
        }
        step("per message: the server's limit") {
            tapLabel("Back")
            tap("New email")
            type("Subject", "Too many", replace = true)
            type("Write an email", "Six photos")
            // A downgrade the app hasn't heard of yet: its plan still says 10 per message.
            Db.exec("update universe set tier = 'free' where name = ?", u.name)
            tapLabel("Add photo")
            Media.pick(activity(), "photo.jpg", "photo.jpg", "photo.jpg", "photo.jpg", "photo.jpg", "photo.jpg")
            see("Up to 5 photos per message", 60_000)
            until(60_000, "five photos uploaded") { seesLabel("Photo 5 of 5") && !seesLabel("Uploading") && !seesLabel("Processing") }
            val draft = a.json("api/emails/drafts/new")
            check(draft.list("attachments").count { it.str("kind") == "photo" } == 5) { "draft: $draft" }
        }
        step("a day's photos: refused at Send with the server's text") {
            today("photo", u.userId(u.a), 10)
            tap("Send", exact = true)
            see("Not enough photos left today", 30_000)
            check(b.json("api/emails/threads").list("items").none { it.str("subject") == "Too many" }) { "the email went out" }
        }
    }

    @Test fun stampsByCategoryAndSearch() = fresh("stamps") { u, _ ->
        val a = u.friend(u.a)
        val cat = a.json("api/stamps")
        val stamps = cat.list("stamps")
        val theme = cat.list("themes")[1]
        val byTheme = stamps.first { it.str("theme") == theme.str("slug") }
        val bySearch = stamps.first { it.str("theme") != theme.str("slug") && it.str("name")!!.length >= 5 }
        step("a stamp by category") {
            tab("Letters")
            until(20_000, "the letter button") { sees("Write a letter") || sees("Continue your draft") }
            tap(if (sees("Continue your draft")) "Continue your draft" else "Write a letter")
            seeLabel("Letter text")
            tapLabel("Add a stamp")
            seeLabel("Search stamps by name or keyword")
            tap(theme.str("name")!!, exact = true)
            tapLabel(byTheme.str("name")!!, exact = true)
            seeLabel("Stamp: ${byTheme.str("name")}. Change it")
        }
        step("a stamp by search") {
            tapLabel("Stamp: ${byTheme.str("name")}. Change it")
            type("Search stamps by name", bySearch.str("name")!!)
            tapLabel(bySearch.str("name")!!, exact = true)
            seeLabel("Stamp: ${bySearch.str("name")}. Change it")
        }
        step("send: the stamp on the tile and on the letter") {
            type("Letter text", "Stamped letter")
            tap("Send", exact = true)
            tapLast("Send")
            until(30_000, "Letters with the sent letter") { sees("to edit") && !seesLabel("Letter text") }
            seeLabel("Stamp: ${bySearch.str("name")}")
            val row = a.json("api/letters").list("items").first()
            check(row.obj("stamp")?.str("slug") == bySearch.str("slug")) { "letter: $row" }
            tap("Stamped letter")
            seeLabel("Stamp: ${bySearch.str("name")}")
            tapLabel("Back")
        }
        step("recents: newest first, in the picker") {
            val recent = (a.json("api/stamps")["recent"] as JsonArray).map { it.jsonPrimitive.content }
            check(recent.take(2) == listOf(bySearch.str("slug"), byTheme.str("slug"))) { "recent: $recent" }
            until(20_000, "the letter button") { sees("Write a letter") || sees("Continue your draft") }
            tap(if (sees("Continue your draft")) "Continue your draft" else "Write a letter")
            tapLabel("Add a stamp")
            see("Recent")
            seeLabel(bySearch.str("name")!!)
            closeSheet()
        }
    }

    @Test fun letterDoodle() = fresh("letter-doodle") { u, _ ->
        val a = u.friend(u.a)
        step("draw and attach") {
            tab("Letters")
            until(20_000, "the letter button") { sees("Write a letter") || sees("Continue your draft") }
            tap(if (sees("Continue your draft")) "Continue your draft" else "Write a letter")
            seeLabel("Letter text")
            tapLabel("Draw a doodle")
            draw("Empty paper")
            tapLast("Done")
            until(30_000, "the doodle on the letter") { seesLabel("Remove doodle") && !seesLabel("Uploading") && !seesLabel("Processing") }
            seeLabel("Doodle attached")
        }
        step("send: the PNG shows in the sent letter") {
            type("Letter text", "With a doodle")
            tap("Send", exact = true)
            tapLast("Send")
            until(30_000, "Letters with the sent letter") { sees("to edit") && !seesLabel("Letter text") }
            val row = a.json("api/letters").list("items").first()
            check(row.obj("media")?.get("doodles")?.jsonPrimitive?.int == 1) { "letter: $row" }
            val letter = a.json("api/letters/${row.str("id")}")
            val doodle = letter.list("attachments").first { it.str("kind") == "doodle" }
            check(a.fetch(doodle.str("url")!!) == 200) { "doodle image" }
            tap("With a doodle")
            seeLabel("Doodle, open full size")
        }
    }

    @Test fun createAndJoinWizards() {
        E2E.prepare()
        Db.require()
        val invite = Universe.code()
        Db.invite(invite, "free")
        val name = "W" + Universe.tag(6)
        val owner = "ann" + Universe.tag()
        val joiner = "bob" + Universe.tag()
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            val ui = Ui(this, "wizards")
            var key = ""
            ui.step("invite") {
                ui.until(30_000, "the landing") { ui.sees("Enter your universe") }
                ui.tap("Create a universe")
                ui.see("Your invite")
                ui.type("Invite", invite)
                ui.tap("Continue", exact = true)
            }
            ui.step("name and welcome") {
                ui.see("Your universe")
                ui.type("Name", name)
                ui.tap("Continue", exact = true)
            }
            ui.step("citizen and PIN") {
                ui.see("Your citizen")
                ui.type("Username", owner)
                ui.tap("Continue", exact = true)
                ui.see("Create your PIN")
                ui.pin("135792")
                ui.see("Confirm your PIN")
                ui.pin("135792")
            }
            ui.step("the key, shown once") {
                ui.see("This is $name's key", 30_000)
                key = Regex("Key ((?:[A-Z0-9] ){11}[A-Z0-9])").find(ui.visible())!!.groupValues[1].replace(" ", "")
                ui.tap("Copy", exact = true)
                ui.tap("Continue", exact = true)
                ui.home()
                ui.see("Waiting for someone to join")
                val row = Db.rows("select tier from universe where name = ?", name).single()
                check(row["tier"] == "free") { "universe: $row" }
                val creator = Friend(id = 1, pin = "135792", key = key).signIn().home()
                check(creator.obj("universe")?.get("waiting")?.jsonPrimitive?.content == "true") { "home: $creator" }
            }
            ui.step("the API refuses a taken username") {
                val other = Friend(id = 2, pin = "246802", key = key)
                other.must("POST", "api/entry", """{"code":"$key"}""")
                val taken = other.call(
                    "POST",
                    "api/universe/join",
                    """{"username":"${owner.uppercase()}","timezone":"Asia/Tehran","pin":"246802","language":"en"}""",
                )
                check(taken.status == 409) { "join as the creator's name: ${taken.status} ${taken.body}" }
            }
            ui.step("second device: the key, then join") {
                ui.tab("You")
                ui.tap("Log out")
                ui.tapLast("Log out")
                ui.tap("Use a different key", timeoutMs = 30_000)
                ui.until(20_000, "the landing") { ui.sees("Enter your universe") }
                ui.type("Key", key)
                ui.tap("Enter", exact = true)
                ui.until(20_000, "the welcome") { ui.sees("Welcome to") }
                ui.tapLast("Enter")
                ui.tap("Join $name")
                ui.see("Your citizen")
                ui.type("Username", owner)
                ui.see("That username is taken")
                ui.type("Username", joiner, replace = true)
                ui.gone("That username is taken")
                ui.tap("Continue", exact = true)
                ui.see("Create your PIN")
                ui.pin("246802")
                ui.see("Confirm your PIN")
                ui.pin("246802")
                ui.home()
                val creator = Friend(id = 1, pin = "135792", key = key).signIn().home()
                check(creator.obj("friend")?.str("nickname")?.equals(joiner, ignoreCase = true) == true) { "friend: ${creator["friend"]}" }
            }
            ui.shot("end")
        }
    }

    @Test
    @Config(sdk = [27])
    fun fingerprintUnlock() {
        E2E.prepare()
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            Biometrics.install()
            try {
                val ui = Ui(this, "fingerprint")
                ui.signIn()
                ui.step("turn on in You") {
                    ui.tab("You")
                    ui.tap("Fingerprint unlock")
                    ui.pin(E2E.A_PIN)
                    ui.until(20_000, "the fingerprint prompt") { Biometrics.dialog(activity!!) != null }
                    Biometrics.touch()
                    ui.see("Fingerprint unlock is on")
                }
                ui.step("sign out; Use PIN instead") {
                    ui.tap("Log out")
                    ui.tapLast("Log out")
                    ui.see("userone", 30_000)
                    ui.tap("userone")
                    ui.until(20_000, "the fingerprint prompt") { Biometrics.dialog(activity!!) != null }
                    Biometrics.usePin(activity!!)
                    ui.see("Enter your PIN")
                    ui.pin(E2E.A_PIN)
                    ui.home()
                }
                ui.step("sign out; the fingerprint signs in") {
                    ui.tab("You")
                    ui.tap("Log out")
                    ui.tapLast("Log out")
                    ui.see("userone", 30_000)
                    ui.tap("userone")
                    ui.until(20_000, "the fingerprint prompt") { Biometrics.dialog(activity!!) != null }
                    Biometrics.touch()
                    ui.home()
                    // The session the fingerprint opened is a real one.
                    ui.tab("You")
                    ui.see("View profile")
                }
                ui.shot("end")
            } finally {
                Biometrics.uninstall()
            }
        }
    }

    @Test fun selfiesLockedThenExpired() = fresh("selfies") { u, activity ->
        val b = u.friend()
        val bName = u.b!!.username
        step("pin mine with the camera app") {
            tap("Add your selfie")
            Media.shoot(activity(), "photo.jpg")
            see("Once you use it")
            tap("Use", exact = true)
            seeLabel("Your selfie, taken", 30_000)
        }
        step("the friend's view stays locked until they post") {
            val theirs = b.home().obj("selfies")!!.obj("theirs")!!
            check(theirs["locked"]!!.jsonPrimitive.content == "true" && theirs["url"] is JsonNull) { "locked: $theirs" }
            b.uploadOk("api/selfies", Friend.Multipart(emptyMap(), "photo.jpg", "image/jpeg"), "friend's selfie")
            val now = b.home().obj("selfies")!!.obj("theirs")!!
            check(now["locked"]!!.jsonPrimitive.content == "false" && now.str("url") != null) { "unlocked: $now" }
            seeLabel("$bName's selfie, taken", 20_000)
        }
        step("both expire after the universe's window") {
            Db.exec("update selfie set expires_at = now() - interval '1 second' where universe_id = (select id from universe where name = ?)", u.name)
            b.setMood("CALM") // a signal: Home re-reads
            see("Add your selfie", 30_000)
            val s = b.home().obj("selfies")!!
            check(s["mine"] is JsonNull && s["theirs"] is JsonNull) { "selfies: $s" }
        }
    }

    @Test fun stickersPackCustomAndDelete() = fresh("stickers") { u, _ ->
        val b = u.friend()
        val aId = u.friend(u.a).me()
        val tid = b.email("Sticker thread", "Hi from B")
        step("open the thread") {
            tab("Emails")
            see("Sticker thread", 30_000)
            tap("Sticker thread")
            see("Hi from B")
        }
        step("send a pack sticker") {
            tapLabel("Stickers", exact = true)
            tapLabel("Sticker: heart")
            until(20_000, "the sticker email") { b.thread(tid).any { it.str("senderId") == aId && it["sticker"] is JsonObject } }
        }
        step("make a custom sticker") {
            if (!seesLabel("Add a sticker")) tapLabel("Stickers", exact = true)
            tapLabel("Add a sticker")
            tap("Draw a sticker")
            draw("Empty paper")
            tapLast("Next")
            see("New sticker")
            type("Keywords", "zebra")
            tapLast("Add")
            gone("New sticker", 30_000)
            seeLabel("Sticker: zebra", 20_000)
        }
        step("the friend has it; deleted for both") {
            val custom = b.json("api/stickers").list("stickers").firstOrNull { it.obj("custom") != null && "zebra" in it["keywords"].toString() }
            check(custom != null) { "the friend has no custom sticker" }
            longPressLabel("Sticker: zebra")
            see("Delete this sticker for both of you?")
            tapLast("Delete")
            until(20_000, "gone from the picker") { !seesLabel("Sticker: zebra") }
            check(b.json("api/stickers").list("stickers").none { it.str("id") == custom!!.str("id") }) { "still in the friend's picker" }
        }
    }

    @Test fun dataDeletion() {
        E2E.prepare()
        Db.require()
        val leaver = Universe.fresh()
        val closed = Universe.fresh()
        val left = Universe.fresh()
        runAndroidComposeUiTest<MainActivity>(effectContext = StandardTestDispatcher()) {
            val ui = Ui(this, "deletion")
            val context = ApplicationProvider.getApplicationContext<Context>()
            ui.step("the member deletes their account here") {
                val me = leaver.b!!
                ui.signIn(me.username, leaver.key, me.pin)
                val token = "e2eTok_" + Universe.tag(10)
                leaver.friend().must("PUT", "api/push/devices", """{"platform":"fcm","token":"$token","appId":"com.delulutopia.app"}""")
                check((Db.one("select count(*) from push_device where token = ?", token) as Number).toInt() == 1) { "no token" }
                ui.tab("You")
                ui.tap("Appearance")
                ui.tap("Dark")
                ui.tapLabel("Back")
                context.getSharedPreferences("device", Context.MODE_PRIVATE).edit().putString("e2e.marker", "x").commit()
                val theme = File(context.filesDir, "datastore/device_settings.preferences_pb")
                ui.until(10_000, "the theme saved") { theme.exists() }
                ui.tap("Delete my account")
                ui.see("Delete your account?")
                ui.tap("Continue", exact = true)
                ui.see("Enter your PIN to delete")
                ui.pin(me.pin)
                ui.see("Your account is gone", 30_000)
                // Server: the account, its push token, the friend's view.
                check((Db.one("select count(*) from push_device where token = ?", token) as Number).toInt() == 0) { "token kept" }
                check((Db.one("select count(*) from app_user u join universe v on v.id = u.universe_id where v.name = ?", leaver.name) as Number).toInt() == 1)
                check(leaver.friend(leaver.a).home().obj("universe")!!["waiting"]!!.jsonPrimitive.content == "true")
                // Device: prefs wiped, the theme kept.
                check(!context.getSharedPreferences("device", Context.MODE_PRIVATE).contains("e2e.marker")) { "device prefs kept" }
                check(theme.exists()) { "the theme was wiped" }
                ui.tap("Done", exact = true)
                // No session and no entry pass: the cookies went too.
                ui.until(20_000, "the landing") { ui.sees("Enter your universe") }
            }
            ui.step("the owner closes the universe: the goodbye here") {
                val me = closed.b!!
                ui.signIn(me.username, closed.key, me.pin)
                closed.friend(closed.a).must("DELETE", "api/universe", """{"pin":"${closed.a.pin}","name":"${closed.name}","platform":"android"}""")
                ui.see("${closed.name} was closed", 30_000)
                check((Db.one("select count(*) from universe where name = ?", closed.name) as Number).toInt() == 0) { "universe kept" }
                ui.tap("Done", exact = true)
                ui.until(20_000, "the landing") { ui.sees("Enter your universe") }
            }
            ui.step("the member leaves: my Waiting card, the old key refused") {
                ui.signIn(left.a.username, left.key, left.a.pin)
                ui.see(left.b!!.username)
                left.friend().must("DELETE", "api/me", """{"pin":"${left.b!!.pin}","platform":"android"}""")
                ui.see("Waiting for someone to join", 30_000)
                val stale = Friend(key = left.key).call("POST", "api/entry", """{"code":"${left.key}"}""")
                check(stale.status in 400..499) { "old key: ${stale.status} ${stale.body}" }
            }
            ui.shot("end")
        }
    }

    @Test fun attachmentRetentionPlaceholders() = fresh("retention") { u, _ ->
        val b = u.friend()
        val photo = b.uploadOk("api/emails/drafts/new", Friend.Multipart(mapOf("kind" to "photo"), "photo.jpg", "image/jpeg"), "photo").obj
        val voice = b.uploadOk("api/emails/drafts/new", Friend.Multipart(mapOf("kind" to "audio"), "voice.m4a", "audio/mp4"), "voice").obj
        val tid = b.email("Old files", "These have expired")
        // What the daily job does at 00:05 UTC after the retention days (AR3): the rows become tombstones.
        Db.exec("update attachment set expired_at = now() where id in (?::uuid, ?::uuid)", photo.str("id"), voice.str("id"))
        step("the placeholders") {
            tab("Emails")
            see("Old files", 30_000)
            tap("Old files")
            see("These have expired")
            until(20_000, "the photo placeholder") { sees("Photo expired") || seesLabel("Photo expired") }
            until(20_000, "the voice placeholder") { sees("Voice note expired") || seesLabel("Voice note expired") }
        }
        step("the API marks them and answers 410") {
            val atts = b.thread(tid).last().list("attachments")
            check(atts.size == 2 && atts.all { it.str("expired") == "true" }) { "attachments: $atts" }
            val url = atts.first { it.str("kind") == "photo" }.str("url")!!
            check(b.fetch(url) == 410) { "the expired file still answers" }
        }
    }

    @Test fun emailTicksRepliesActionsAndOptimisticSend() = fresh("email-bubbles") { u, _ ->
        val b = u.friend()
        val aId = Db.userId(u.name, u.a.username)
        var tid = ""
        step("read ticks: sent, delivered, read") {
            tab("Emails")
            tap("New email")
            type("Subject", "Ticks", replace = true)
            type("Write an email", "Tick test")
            tap("Send", exact = true)
            until(30_000, "the new thread") { seesLabel("Write a reply") || sees("Write a reply") }
            seeLabel(", sent")
            tid = b.json("api/emails/threads").list("items").first { it.str("subject") == "Ticks" }.str("id")!!
            seeLabel(", delivered", 20_000)
            b.must("POST", "api/emails/threads/$tid/seen")
            seeLabel(", read", 20_000)
            check(b.thread(tid).first().str("readAt") != null) { "not read on the API" }
        }
        var fromB = ""
        step("reply to one email") {
            fromB = b.reply(tid, "From bob")
            see("From bob", 20_000)
            longPress("From bob")
            seeLabel("Email actions")
            tapLast("Reply")
            see("Replying to ${u.b!!.username}")
            type("Write a reply", "Answer to bob")
            tapLabel("Send", exact = true)
            seeLabel("Go to the email from ${u.b.username}", 20_000)
            until(20_000, "the reply on the API") { b.thread(tid).any { it.str("text") == "Answer to bob" && it.obj("replyTo")?.str("id") == fromB } }
        }
        step("bubble actions: hold, react") {
            longPress("From bob")
            tapLabel("😂")
            seeLabel("Reactions: 😂, 1")
            until(20_000, "the reaction on the API") { b.thread(tid).first { it.str("id") == fromB }.list("reactions").any { it.str("emoji") == "😂" } }
        }
        step("optimistic send refused: Cancel") {
            Db.exec(
                "insert into email_credit (user_id, day, used) values (?, (now() at time zone 'utc')::date, 1000) on conflict (user_id, day) do update set used = 1000",
                aId,
            )
            type("Write a reply", "Will not go")
            tapLabel("Send", exact = true)
            see("Will not go")
            seeLabel("Not sent. Options", 30_000)
            tapLabel("Not sent. Options")
            see("Email not sent")
            tap("Cancel email")
            gone("Will not go")
            check(b.thread(tid).none { it.str("text") == "Will not go" }) { "the cancelled email went out" }
        }
        step("optimistic send without an answer: Retry") {
            Db.exec("delete from email_credit where user_id = ?", aId)
            b.setMood("CALM") // a signal: the credits come back
            type("Write a reply", "Slow one")
            until(20_000, "Send enabled") { enabled("Send") }
            Db.lock("email").use {
                tapLabel("Send", exact = true)
                see("Slow one")
                // OS8: no answer in 20 s counts as failed.
                seeLabel("Not sent. Options", 40_000)
            }
            tapLabel("Not sent. Options")
            tap("Retry", exact = true)
            until(30_000, "sent") { !seesLabel("Not sent. Options") && !sees("Not sent") }
            until(20_000, "one email on the API") { b.thread(tid).count { it.str("text") == "Slow one" } == 1 }
        }
    }
}
