package com.delulutopia.app.e2e

import android.content.Context
import android.os.Looper
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import java.net.CookieManager
import java.net.CookiePolicy
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import org.junit.Assume.assumeTrue
import org.robolectric.Shadows.shadowOf

/**
 * End-to-end harness: the real app (Hilt graph, OkHttp, navigation) against a real API started by
 * CI's `e2e` job (Postgres + the delululand-api image, short timers). The URL comes from the
 * `dl.e2e` system property (`-Pdl.e2e=http://localhost:8080/`); without it every case is skipped.
 *
 * User A (`userone`) drives the app; user B (`usertwo`) acts through the API with [Friend], so a
 * case can receive a letter, a comment or an email without a second phone.
 */
object E2E {
    val server: String? = System.getProperty("dl.e2e")?.takeIf { it.isNotBlank() }?.let { if (it.endsWith("/")) it else "$it/" }

    const val KEY = "E2EKEY234567"
    const val A_ID = 1
    const val A_PIN = "135790"
    const val B_ID = 2
    const val B_PIN = "246801"

    /** The PIN the Change PIN case switches to and back from. */
    const val A_PIN_2 = "975310"

    fun prepare() {
        assumeTrue("no e2e API (set -Pdl.e2e)", server != null)
        System.setProperty("dl.baseUrl", server!!)
        System.setProperty("dl.motion", "off")
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("device", Context.MODE_PRIVATE).edit()
            .putBoolean("leaves", false)
            .putBoolean("notifyAsked", true)
            .commit()
    }
}

/** User B, through the API as the web app would call it. */
class Friend(private val base: String = E2E.server!!, private val id: Int = E2E.B_ID, private val pin: String = E2E.B_PIN) {
    private val cookies = CookieManager(null, CookiePolicy.ACCEPT_ALL)
    private val json = Json { ignoreUnknownKeys = true }

    data class Answer(val status: Int, val body: String) {
        val json: JsonElement? get() = runCatching { Json.parseToJsonElement(body) }.getOrNull()
        val obj: JsonObject get() = json!!.jsonObject
    }

    fun call(method: String, path: String, body: String? = null): Answer {
        repeat(5) {
            val a = once(method, path, body)
            if (a.status != 429) return a
            // Throttles (a wrong key typed in the app a moment ago) say how long to wait.
            val wait = runCatching { a.obj["retryAfterMs"]!!.jsonPrimitive.long }.getOrDefault(1_000L)
            Thread.sleep(wait + 100)
        }
        return once(method, path, body)
    }

    private fun once(method: String, path: String, body: String?): Answer {
        val url = URL(base + path.trimStart('/'))
        val c = url.openConnection() as HttpURLConnection
        c.requestMethod = method
        c.setRequestProperty("X-Requested-With", "delululand")
        c.setRequestProperty("Accept", "application/json")
        val jar = cookies.cookieStore.get(url.toURI()).joinToString("; ") { "${it.name}=${it.value}" }
        if (jar.isNotEmpty()) c.setRequestProperty("Cookie", jar)
        if (body != null) {
            c.doOutput = true
            c.setRequestProperty("Content-Type", "application/json")
            c.outputStream.use { it.write(body.encodeToByteArray()) }
        }
        val status = c.responseCode
        cookies.put(url.toURI(), c.headerFields.filterKeys { it != null })
        val text = (if (status >= 400) c.errorStream else c.inputStream)?.use { it.readBytes().decodeToString() }.orEmpty()
        return Answer(status, text)
    }

    private fun ok(a: Answer, what: String): Answer {
        check(a.status in 200..299) { "friend: $what answered ${a.status} ${a.body}" }
        return a
    }

    /** Key, then PIN (created on a fresh stack). */
    fun signIn(): Friend {
        ok(call("POST", "api/entry", """{"code":"${E2E.KEY}"}"""), "entry")
        val users = ok(call("GET", "api/universe/entry"), "cards").obj["users"]!!.jsonArray
        val me = users.first { it.jsonObject["id"]!!.jsonPrimitive.int == id }.jsonObject
        val hasPin = me["hasPin"]!!.jsonPrimitive.content == "true"
        val path = if (hasPin) "api/auth/login" else "api/auth/pin"
        ok(call("POST", path, """{"userId":$id,"pin":"$pin"}"""), "pin")
        return this
    }

    private fun doc(text: String) = """{"type":"doc","content":[{"type":"paragraph","content":[{"type":"text","text":${quote(text)}}]}]}"""

    fun quote(s: String) = json.encodeToString(kotlinx.serialization.serializer<String>(), s)

    /** A letter to A; returns its id. */
    fun sendLetter(text: String): Long {
        val v = ok(call("GET", "api/draft"), "draft").obj["version"]!!.jsonPrimitive.int
        val saved = ok(call("PUT", "api/draft", """{"body":${doc(text)},"pinnedLetterId":null,"stampSlug":null,"version":$v}"""), "save draft").obj
        val sv = saved["version"]!!.jsonPrimitive.int
        return ok(call("POST", "api/letters", """{"draftVersion":$sv}"""), "send").obj["id"]!!.jsonPrimitive.long
    }

    /** A discussion by B; returns its id. */
    fun postDiscussion(title: String, text: String, color: String = "mint"): Long {
        val v = ok(call("GET", "api/discussions/draft"), "d draft").obj["version"]!!.jsonPrimitive.int
        val sv = ok(
            call("PUT", "api/discussions/draft", """{"title":${quote(title)},"body":${doc(text)},"version":$v,"color":"$color"}"""),
            "save d draft",
        ).obj["version"]!!.jsonPrimitive.int
        return ok(call("POST", "api/discussions", """{"draftVersion":$sv}"""), "post").obj["id"]!!.jsonPrimitive.long
    }

    fun comment(discussionId: Long, text: String): Long =
        ok(call("POST", "api/discussions/$discussionId/comments", """{"text":${quote(text)}}"""), "comment").obj["id"]!!.jsonPrimitive.long

    fun react(target: String, id: Long, emoji: String) = ok(call("PUT", "api/reactions/$target/$id", """{"emoji":${quote(emoji)}}"""), "react")

    /** A new email thread from B; returns the thread id. */
    fun email(subject: String, text: String): Long {
        val v = ok(call("GET", "api/emails/drafts/new"), "e draft").obj["version"]!!.jsonPrimitive.int
        val sv = ok(call("PUT", "api/emails/drafts/new", """{"subject":${quote(subject)},"text":${quote(text)},"version":$v}"""), "save e draft")
            .obj["version"]!!.jsonPrimitive.int
        return ok(call("POST", "api/emails/threads", """{"draftVersion":$sv}"""), "send email").obj["thread"]!!.jsonObject["id"]!!.jsonPrimitive.long
    }

    fun reply(threadId: Long, text: String) {
        val v = ok(call("GET", "api/emails/drafts/$threadId"), "r draft").obj["version"]!!.jsonPrimitive.int
        val sv = ok(call("PUT", "api/emails/drafts/$threadId", """{"subject":"","text":${quote(text)},"version":$v}"""), "save r draft")
            .obj["version"]!!.jsonPrimitive.int
        ok(call("POST", "api/emails/threads/$threadId/emails", """{"draftVersion":$sv}"""), "reply")
    }

    fun setMood(key: String) = ok(call("PUT", "api/me/mood", """{"mood":"$key"}"""), "mood")

    fun home(): JsonObject = ok(call("GET", "api/home"), "home").obj
}

/** UI steps with strict checks: a missing text fails the case with what the screen showed instead. */
@OptIn(ExperimentalTestApi::class)
class Ui(val test: ComposeUiTest, private val case: String) {
    @Volatile private var beat = System.currentTimeMillis()
    private val worker = Thread.currentThread()

    init {
        // A watchdog: if the test thread makes no progress for a minute (a recomposition or
        // network loop inside the app), print where it is stuck, so CI says why.
        Thread {
            var dumps = 0
            while (worker.isAlive && dumps < 3) {
                Thread.sleep(5_000)
                if (System.currentTimeMillis() - beat > 60_000) {
                    dumps++
                    println("e2e $case !! no progress for 60 s; the test thread is at:")
                    worker.stackTrace.take(60).forEach { println("e2e $case !!   at $it") }
                    val rt = Runtime.getRuntime()
                    println("e2e $case !! heap ${(rt.totalMemory() - rt.freeMemory()) shr 20} MB used")
                    runCatching {
                        // What keeps the main thread busy: the head of its message queue.
                        val q = Looper.getMainLooper().queue
                        val head = android.os.MessageQueue::class.java.getDeclaredField("mMessages").apply { isAccessible = true }
                        val next = android.os.Message::class.java.getDeclaredField("next").apply { isAccessible = true }
                        var m = head.get(q) as android.os.Message?
                        val seen = LinkedHashMap<String, Int>()
                        var n = 0
                        while (m != null && n < 5_000) {
                            val k = "${m.target?.javaClass?.name} ${m.callback?.javaClass?.name} what=${m.what}"
                            seen[k] = (seen[k] ?: 0) + 1
                            m = next.get(m) as android.os.Message?
                            n++
                        }
                        println("e2e $case !! main queue: $n messages")
                        seen.entries.sortedByDescending { it.value }.take(15).forEach { println("e2e $case !!   ${it.value} × ${it.key}") }
                    }.onFailure { println("e2e $case !! queue: $it") }
                    beat = System.currentTimeMillis()
                }
            }
        }.apply { isDaemon = true }.start()
    }

    private var shots = 0

    /** Lets real time pass for the app: the main looper's clock follows the wall clock. */
    fun pass(ms: Long) {
        var left = ms
        while (left > 0) {
            val step = minOf(100L, left)
            Thread.sleep(step)
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(step))
            // Delays inside composition (effects, a sheet's coroutine) run on the test clock.
            runCatching { test.mainClock.advanceTimeBy(step) }
            left -= step
            beat = System.currentTimeMillis()
        }
        runCatching { test.waitForIdle() }.onFailure {
            // An exception inside the app (a coroutine, a recomposition) surfaces here: show it.
            println("e2e $case !! ${it::class.simpleName}: ${it.message}\n${it.stackTraceToString().lines().take(12).joinToString("\n")}")
            throw it
        }
    }

    fun until(timeoutMs: Long, what: String, cond: () -> Boolean) {
        var waited = 0L
        while (true) {
            if (runCatching { cond() }.getOrDefault(false)) return
            if (waited >= timeoutMs) fail("timed out waiting for $what")
            if (waited > 0 && waited % 10_000 == 0L) trace("still waiting for $what")
            pass(200)
            waited += 200
        }
    }

    /** What the screen shows now, in the log (one line). */
    fun trace(why: String) = println("e2e $case · $why: ${visible().take(700)}")

    private fun count(m: SemanticsMatcher, unmerged: Boolean = false) =
        runCatching { test.onAllNodes(m, useUnmergedTree = unmerged).fetchSemanticsNodes().size }.getOrDefault(0)

    fun sees(text: String, substring: Boolean = true) = count(hasText(text, substring = substring)) > 0 || count(hasText(text, substring = substring), true) > 0

    fun seesLabel(label: String) = count(hasContentDescription(label, substring = true)) > 0 || count(hasContentDescription(label, substring = true), true) > 0

    fun see(text: String, timeoutMs: Long = 20_000) = until(timeoutMs, "\"$text\"") { sees(text) }

    fun seeLabel(label: String, timeoutMs: Long = 20_000) = until(timeoutMs, "[$label]") { seesLabel(label) }

    fun gone(text: String, timeoutMs: Long = 20_000) = until(timeoutMs, "\"$text\" to go") { !sees(text) }

    /**
     * Scrolls [node] into view only when none of it shows: performScrollTo never settles on a node
     * drawn a few dp outside its list (the Discussions "Archived · N" link is pulled 6 dp up).
     */
    private fun scrollIn(node: SemanticsNodeInteraction) {
        if (runCatching { node.assertIsDisplayed() }.isFailure) runCatching { node.performScrollTo() }
    }

    /** Taps the first clickable node showing [text] (scrolled into view first). */
    fun tap(text: String, exact: Boolean = false, timeoutMs: Long = 20_000) {
        val m = hasText(text, substring = !exact) and hasClickAction()
        until(timeoutMs, "a tappable \"$text\"") { count(m) > 0 }
        val node = test.onAllNodes(m)[0]
        scrollIn(node)
        node.performClick()
        pass(300)
        trace("after tap \"$text\"")
    }

    fun tapLabel(label: String, exact: Boolean = false, timeoutMs: Long = 20_000) {
        val m = hasContentDescription(label, substring = !exact) and hasClickAction()
        until(timeoutMs, "a tappable [$label]") { count(m) > 0 }
        val node = test.onAllNodes(m)[0]
        scrollIn(node)
        node.performClick()
        pass(300)
    }

    /** A navigation bar destination (its label is the last node with that text). */
    fun tab(name: String) {
        tapLast(name, exact = true)
        pass(500)
    }

    /**
     * The lowest of several matches on screen: a sheet's or dialog's confirm button repeats the
     * screen's action word in the top bar ("Send", "Post", "Log out", "Enter").
     */
    fun tapLast(text: String, exact: Boolean = true) {
        val m = hasText(text, substring = !exact) and hasClickAction()
        until(20_000, "a tappable \"$text\"") { count(m) > 0 }
        // The newest window's match: a dialog or sheet opens above the screen whose top bar
        // repeats the same word ("Send", "Post", "Log out", "Enter").
        val roots = test.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().map { it.id }
        val nodes = test.onAllNodes(m)
        val all = nodes.fetchSemanticsNodes()
        fun rootOf(n: SemanticsNode): Int {
            var x = n
            while (x.parent != null) x = x.parent!!
            return roots.indexOf(x.id)
        }
        val pick = all.indices.maxWith(compareBy<Int> { rootOf(all[it]) }.thenBy { all[it].boundsInWindow.top })
        nodes[pick].performClick()
        pass(300)
        trace("after tap last \"$text\"")
    }

    /** Types into the text field labelled [label] (content description or placeholder). */
    fun type(label: String, text: String, replace: Boolean = false) {
        val m = hasSetTextAction() and (hasContentDescription(label, substring = true) or hasText(label, substring = true))
        until(20_000, "a field [$label]") { count(m) > 0 }
        val node = test.onAllNodes(m)[0]
        scrollIn(node)
        if (replace) node.performTextReplacement(text) else node.performTextInput(text)
        pass(300)
    }

    fun pin(pin: String) {
        pin.forEach { d ->
            val m = hasText(d.toString(), substring = false) and hasClickAction()
            until(10_000, "PIN key $d") { count(m) > 0 }
            test.onAllNodes(m)[0].performClick()
            pass(120)
        }
        pass(500)
    }

    fun back(activity: () -> androidx.activity.ComponentActivity) {
        activity().onBackPressedDispatcher.onBackPressed()
        pass(500)
    }

    fun shot(name: String) {
        runCatching {
            test.onAllNodes(isRoot())[0].captureRoboImage("build/outputs/roborazzi/e2e_${case}_${++shots}_$name.png")
        }
    }

    /** Every text and label on screen, for failure messages. */
    fun visible(): String = runCatching {
        val out = mutableListOf<String>()
        fun walk(n: SemanticsNode) {
            n.config.getOrNull(SemanticsProperties.Text)?.forEach { out += it.text }
            n.config.getOrNull(SemanticsProperties.ContentDescription)?.forEach { out += "[$it]" }
            n.config.getOrNull(SemanticsProperties.EditableText)?.let { out += "{${it.text}}" }
            n.children.forEach(::walk)
        }
        test.onAllNodes(isRoot(), useUnmergedTree = true).fetchSemanticsNodes().forEach(::walk)
        out.joinToString(" | ").take(4000)
    }.getOrElse { "(no tree: $it)" }

    fun fail(why: String): Nothing {
        shot("FAIL")
        throw AssertionError("$case: $why\nOn screen: ${visible()}")
    }

    /** One named step: a failure inside names the step. */
    fun step(name: String, block: () -> Unit) {
        println("e2e $case › $name")
        try {
            block()
        } catch (e: AssertionError) {
            throw AssertionError("[$name] ${e.message}", e)
        } catch (e: Throwable) {
            shot("ERR")
            throw AssertionError("[$name] ${e::class.simpleName}: ${e.message}\nOn screen: ${visible()}", e)
        }
    }

    /** From a fresh install to Home as user A: key, welcome, card, PIN (created on a fresh stack). */
    fun signIn(name: String = "userone") {
        step("landing") {
            until(30_000, "the landing or the cards") { sees("Enter your universe") || sees(name) }
            if (!sees(name)) {
                type("Key", E2E.KEY)
                tap("Enter", exact = true)
                // UV-3: the welcome; Enter goes on to the cards.
                until(20_000, "the welcome or the cards") { sees("Welcome to") || sees(name) }
                if (sees("Welcome to")) tapLast("Enter")
            }
        }
        step("card") {
            see(name)
            tap(name)
            until(20_000, "the PIN screen") { sees("Enter your PIN") || sees("Create your PIN") }
        }
        step("pin") {
            if (sees("Create your PIN")) {
                pin(E2E.A_PIN)
                see("Confirm your PIN")
            }
            pin(E2E.A_PIN)
            pass(1500)
            // A Change PIN case that stopped half-way leaves the other PIN: put the usual one back.
            if (sees("Wrong PIN")) {
                pin(E2E.A_PIN_2)
                runCatching {
                    Friend(id = E2E.A_ID, pin = E2E.A_PIN_2).signIn()
                        .call("POST", "api/me/pin", """{"currentPin":"${E2E.A_PIN_2}","newPin":"${E2E.A_PIN}"}""")
                }
            }
            home()
        }
    }

    /** Home is up; the one-time fingerprint offer (X1) is declined if it shows. */
    fun home() {
        // The tab bar (all four tabs) only exists once the session is in; Home's own content varies.
        until(30_000, "Home's content") { sees("Letters", false) && sees("Discussions", false) && sees("Emails", false) && sees("Home", false) }
        pass(800)
        if (sees("Unlock with your fingerprint?")) tap("Not now")
    }
}
