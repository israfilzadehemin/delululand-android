package com.delulutopia.app.e2e

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.security.keystore.KeyGenParameterSpec
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.core.content.FileProvider
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.security.Key
import java.security.KeyStoreSpi
import java.security.MessageDigest
import java.security.Provider
import java.security.SecureRandom
import java.security.Security
import java.security.cert.Certificate
import java.security.spec.AlgorithmParameterSpec
import java.sql.Connection
import java.util.Collections
import java.util.Date
import java.util.Enumeration
import java.util.Properties
import javax.crypto.KeyGeneratorSpi
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assume.assumeTrue
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowDialog

/**
 * The CI-only database hook (decided 21 Nov 2026): the e2e job publishes its Postgres on localhost
 * and passes `-Pdl.e2e.db`. Cases use it only for states the API's clock or tools reach outside a
 * test's minutes — an invite (`bin/universe invite`), a selfie's or a file's expiry, a day's usage
 * or credits spent, a lock that makes a send time out — and to read state the API doesn't expose
 * (a deleted citizen's push tokens). Never to stand in for an API call a citizen can make.
 */
object Db {
    private val url: String? = System.getProperty("dl.e2e.db")?.takeIf { it.isNotBlank() }

    fun require() = assumeTrue("no e2e database (set -Pdl.e2e.db)", url != null)

    fun connect(): Connection {
        require()
        // The driver directly, not DriverManager: Robolectric's class loader hides drivers from it.
        return org.postgresql.Driver().connect(url!!, Properties()) ?: error("bad dl.e2e.db: $url")
    }

    fun exec(sql: String, vararg args: Any?): Int = connect().use { c ->
        c.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.executeUpdate()
        }
    }

    fun rows(sql: String, vararg args: Any?): List<Map<String, Any?>> = connect().use { c ->
        c.prepareStatement(sql).use { st ->
            args.forEachIndexed { i, a -> st.setObject(i + 1, a) }
            st.executeQuery().use { r ->
                val out = mutableListOf<Map<String, Any?>>()
                val n = r.metaData.columnCount
                while (r.next()) out += (1..n).associate { r.metaData.getColumnLabel(it) to r.getObject(it) }
                out
            }
        }
    }

    fun one(sql: String, vararg args: Any?): Any? = rows(sql, *args).firstOrNull()?.values?.firstOrNull()

    /** A citizen's row id, by universe name and username. */
    fun userId(universe: String, username: String): Int =
        (
            one(
                "select u.id from app_user u join universe v on v.id = u.universe_id where v.name = ? and lower(u.nickname) = lower(?)",
                universe,
                username,
            ) as Number
            ).toInt()

    /** An invite as `bin/universe invite [--premium]` makes it: the SHA-256 of the normalised code. */
    fun invite(code: String, tier: String) {
        val lookup = MessageDigest.getInstance("SHA-256").digest(code.uppercase().encodeToByteArray())
        exec(
            "insert into universe_invite (code_lookup, created_at, expires_at, tier) values (?, now(), now() + interval '1 day', ?)",
            lookup,
            tier,
        )
    }

    /** Holds an exclusive lock on [table] until closed: writes to it wait, so a send times out. */
    fun lock(table: String): AutoCloseable {
        val c = connect()
        c.autoCommit = false
        c.createStatement().use { it.execute("lock table $table in exclusive mode") }
        return AutoCloseable {
            runCatching { c.rollback() }
            c.close()
        }
    }
}

/** One citizen of a [Universe]: card [index] on the cards screen (1 = creator). */
data class Citizen(val username: String, val pin: String, val index: Int)

/**
 * A universe of its own for one case (decided 21 Nov 2026): flows that delete, create, cap or
 * expire things don't touch universe 1, and the order of the cases doesn't matter.
 */
class Universe(val name: String, val key: String, val tier: String, val a: Citizen, val b: Citizen?) {
    fun friend(c: Citizen = b!!): Friend = Friend(id = c.index, pin = c.pin, key = key).signIn()

    fun userId(c: Citizen): Int = Db.userId(name, c.username)

    companion object {
        private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun code(): String = (1..12).map { ALPHABET[random.nextInt(ALPHABET.length)] }.joinToString("")

        /** Lowercase letters and digits, starting with a letter. */
        fun tag(n: Int = 5): String =
            "abcdefghjkmnpqrstuvwxyz".random().toString() + (1 until n).map { "abcdefghjkmnpqrstuvwxyz23456789".random() }.joinToString("")

        /** Invite (SQL) → create (API) → the friend joins with the key (API), unless [withFriend] is false. */
        fun fresh(tier: String = "premium", withFriend: Boolean = true): Universe {
            val invite = code()
            Db.invite(invite, tier)
            val name = "U" + tag(7)
            val a = Citizen("ann" + tag(), "135792", 1)
            val creator = Friend(id = 1, pin = a.pin)
            val made = creator.call(
                "POST",
                "api/universes",
                """{"invite":"$invite","name":"$name","welcome":"Hello, testers.","username":"${a.username}","timezone":"Europe/Warsaw","pin":"${a.pin}","language":"en"}""",
            )
            check(made.status == 201) { "create universe: ${made.status} ${made.body}" }
            val key = made.obj["code"]!!.jsonPrimitive.content
            val b = if (withFriend) Citizen("bob" + tag(), "246802", 2) else null
            if (b != null) {
                val joiner = Friend(id = 2, pin = b.pin, key = key)
                check(joiner.call("POST", "api/entry", """{"code":"$key"}""").status in 200..299) { "entry" }
                val joined = joiner.call(
                    "POST",
                    "api/universe/join",
                    """{"username":"${b.username}","timezone":"Asia/Tehran","pin":"${b.pin}","language":"en"}""",
                )
                check(joined.status == 201) { "join: ${joined.status} ${joined.body}" }
            }
            return Universe(name, key, tier, a, b)
        }
    }
}

/**
 * The system pickers and the camera app, answered the way they answer a real phone: a content Uri
 * of the app's own FileProvider (a copy of a fixture in cache/camera/), or bytes written to the
 * camera's EXTRA_OUTPUT.
 */
object Media {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    fun uriOf(fixture: String, copy: Int = 0): Uri {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        val f = File(dir, "picked-$copy-$fixture").apply { writeBytes(E2E.fixture(fixture)) }
        return FileProvider.getUriForFile(context, context.packageName + ".photos", f)
    }

    private fun nextRequest(activity: Activity, what: String): Intent {
        val shadow = shadowOf(activity)
        val started = shadow.nextStartedActivityForResult ?: error("no $what was opened")
        return started.intent
    }

    /** Answers the open picker (photos, audio file, video) with these fixtures. */
    fun pick(activity: Activity, vararg fixtures: String) {
        val request = nextRequest(activity, "picker")
        val uris = fixtures.mapIndexed { i, f -> uriOf(f, i) }
        val data = Intent().setData(uris.first())
        if (uris.size > 1) {
            data.clipData = ClipData.newRawUri(null, uris.first()).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        }
        shadowOf(activity).receiveResult(request, Activity.RESULT_OK, data)
    }

    /** Answers the open camera app: the fixture's bytes land where the app asked. */
    fun shoot(activity: Activity, fixture: String) {
        val request = nextRequest(activity, "camera app")
        check(request.action == MediaStore.ACTION_IMAGE_CAPTURE || request.action == MediaStore.ACTION_VIDEO_CAPTURE) {
            "not a camera intent: ${request.action}"
        }
        @Suppress("DEPRECATION")
        val out = request.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT) ?: error("no EXTRA_OUTPUT")
        val bytes = E2E.fixture(fixture)
        val written = runCatching { context.contentResolver.openOutputStream(out)!!.use { it.write(bytes) } }.isSuccess
        if (!written) File(File(context.cacheDir, "camera"), out.lastPathSegment!!).writeBytes(bytes)
        shadowOf(activity).receiveResult(request, Activity.RESULT_OK, Intent())
    }
}

/**
 * Fingerprint unlock under Robolectric (decided 21 Nov 2026): Robolectric has no AndroidKeyStore,
 * so a test provider of that name keeps AES keys in memory (the cipher stays the JDK's AES/GCM);
 * the fingerprint itself is Robolectric's FingerprintManager shadow on API 27, where
 * androidx.biometric shows its own dialog and asks FingerprintManager. No production code changes.
 */
object Biometrics {
    class FakeKeyStoreProvider : Provider("AndroidKeyStore", 1.0, "e2e in-memory AndroidKeyStore") {
        init {
            put("KeyStore.AndroidKeyStore", Store::class.java.name)
            put("KeyGenerator.AES", Generator::class.java.name)
        }
    }

    private val keys: MutableMap<String, SecretKey> = Collections.synchronizedMap(LinkedHashMap())

    class Store : KeyStoreSpi() {
        override fun engineGetKey(alias: String, password: CharArray?): Key? = keys[alias]

        override fun engineGetCertificateChain(alias: String): Array<Certificate>? = null

        override fun engineGetCertificate(alias: String): Certificate? = null

        override fun engineGetCreationDate(alias: String): Date = Date()

        override fun engineSetKeyEntry(alias: String, key: Key, password: CharArray?, chain: Array<out Certificate>?) {
            keys[alias] = key as SecretKey
        }

        override fun engineSetKeyEntry(alias: String, key: ByteArray, chain: Array<out Certificate>?) = throw UnsupportedOperationException()

        override fun engineSetCertificateEntry(alias: String, cert: Certificate) = throw UnsupportedOperationException()

        override fun engineDeleteEntry(alias: String) {
            keys.remove(alias)
        }

        override fun engineAliases(): Enumeration<String> = Collections.enumeration(keys.keys.toList())

        override fun engineContainsAlias(alias: String): Boolean = keys.containsKey(alias)

        override fun engineSize(): Int = keys.size

        override fun engineIsKeyEntry(alias: String): Boolean = keys.containsKey(alias)

        override fun engineIsCertificateEntry(alias: String): Boolean = false

        override fun engineGetCertificateAlias(cert: Certificate): String? = null

        override fun engineStore(stream: java.io.OutputStream?, password: CharArray?) = Unit

        override fun engineLoad(stream: java.io.InputStream?, password: CharArray?) = Unit
    }

    class Generator : KeyGeneratorSpi() {
        private var alias: String? = null
        private var bits = 256

        override fun engineInit(random: SecureRandom?) = Unit

        override fun engineInit(params: AlgorithmParameterSpec?, random: SecureRandom?) {
            val spec = params as KeyGenParameterSpec
            alias = spec.keystoreAlias
            if (spec.keySize > 0) bits = spec.keySize
        }

        override fun engineInit(keysize: Int, random: SecureRandom?) {
            bits = keysize
        }

        override fun engineGenerateKey(): SecretKey {
            val bytes = ByteArray(bits / 8).also { SecureRandom().nextBytes(it) }
            val key = SecretKeySpec(bytes, "AES")
            alias?.let { keys[it] = key }
            return key
        }
    }

    private val provider = FakeKeyStoreProvider()

    /** A phone with an enrolled fingerprint and a screen lock. */
    fun install() {
        keys.clear()
        if (Security.getProvider("AndroidKeyStore") !== provider) {
            Security.removeProvider("AndroidKeyStore")
            Security.insertProviderAt(provider, 1)
        }
        val context = ApplicationProvider.getApplicationContext<Context>()
        val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
        shadowOf(keyguard).setIsDeviceSecure(true)
        shadowOf(keyguard).setIsKeyguardSecure(true)
        fingerprints("setIsHardwareDetected", true)
        fingerprints("setDefaultFingerprints", 1)
    }

    fun uninstall() {
        Security.removeProvider("AndroidKeyStore")
        keys.clear()
    }

    /**
     * Robolectric's ShadowFingerprintManager, by reflection: compileSdk 37's stubs no longer carry
     * android.hardware.fingerprint.FingerprintManager, but API 27's runtime does.
     */
    private fun fingerprints(method: String, vararg args: Any) {
        val fm = ApplicationProvider.getApplicationContext<Context>().getSystemService("fingerprint") ?: error("no fingerprint service")
        val shadow = Shadow.extract<Any>(fm)
        val m = shadow.javaClass.methods.first { it.name == method && it.parameterCount == args.size }
        m.invoke(shadow, *args)
    }

    /** The finger on the sensor: the open prompt succeeds. */
    fun touch() = fingerprints("authenticationSucceeds")

    /** androidx.biometric's own fingerprint dialog (API < 28), if it is showing. */
    fun dialog(activity: FragmentActivity): android.app.Dialog? {
        fun all(fs: List<Fragment>): List<Fragment> = fs + fs.flatMap { runCatching { all(it.childFragmentManager.fragments) }.getOrDefault(emptyList()) }
        return all(activity.supportFragmentManager.fragments).filterIsInstance<DialogFragment>().firstNotNullOfOrNull { it.dialog?.takeIf { d -> d.isShowing } }
            ?: ShadowDialog.getLatestDialog()?.takeIf { it.isShowing }
    }

    /** The prompt's negative button ("Use PIN"). */
    fun usePin(activity: FragmentActivity) {
        val d = dialog(activity) ?: error("no fingerprint dialog")
        val button = d.javaClass.getMethod("getButton", Int::class.javaPrimitiveType).invoke(d, DialogInterface.BUTTON_NEGATIVE) as android.widget.Button
        button.performClick()
    }
}

/** Draws one stroke across the doodle surface labelled [label] ("Empty board", "Empty paper"). */
@OptIn(ExperimentalTestApi::class)
fun Ui.draw(label: String) {
    until(20_000, "the pad [$label]") { seesLabel(label) }
    test.onAllNodes(hasContentDescription(label, substring = true))[0].performTouchInput {
        swipe(Offset(width * 0.2f, height * 0.3f), Offset(width * 0.8f, height * 0.7f), durationMillis = 300)
    }
    pass(500)
}

/** A press held on the first node showing [text] (bubble actions: 450 ms). */
@OptIn(ExperimentalTestApi::class)
fun Ui.longPress(text: String) {
    until(20_000, "\"$text\" to hold") { sees(text) }
    test.onAllNodes(hasText(text, substring = true), useUnmergedTree = true)[0].performTouchInput { longClick(center, durationMillis = 900) }
    pass(500)
}

/** A press held on the first node labelled [label] (a sticker's delete). */
@OptIn(ExperimentalTestApi::class)
fun Ui.longPressLabel(label: String) {
    until(20_000, "[$label] to hold") { seesLabel(label) }
    test.onAllNodes(hasContentDescription(label, substring = true))[0].performTouchInput { longClick(center, durationMillis = 900) }
    pass(500)
}

/** Whether a clickable node labelled exactly [label] is enabled now. */
@OptIn(ExperimentalTestApi::class)
fun Ui.enabled(label: String): Boolean = runCatching {
    test.onAllNodes(hasContentDescription(label, substring = false) and hasClickAction() and isEnabled()).fetchSemanticsNodes().isNotEmpty()
}.getOrDefault(false)

/** The board's JSON for this citizen (null when empty). */
fun Friend.board() = home()["board"]?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.jsonObject
