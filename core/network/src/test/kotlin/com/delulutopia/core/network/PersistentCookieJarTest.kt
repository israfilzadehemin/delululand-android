package com.delulutopia.core.network

import com.delulutopia.core.network.cookie.CookieVault
import com.delulutopia.core.network.cookie.PersistentCookieJar
import com.google.common.truth.Truth.assertThat
import java.io.File
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PersistentCookieJarTest {
    @get:Rule val folder = TemporaryFolder()

    /** Reversible stand-in for the Keystore (not available on the JVM). */
    private val vault = object : CookieVault {
        override fun seal(plain: ByteArray) = plain.reversedArray()

        override fun open(sealed: ByteArray) = sealed.reversedArray()
    }
    private val url = "https://test.delulutopia.com/api/home".toHttpUrl()
    private var now = 1_000_000L

    private fun jar(file: File = File(folder.root, "cookies.bin")) = PersistentCookieJar(file, vault) { now }

    private fun header(h: String) = Cookie.parse(url, h)!!

    @Test
    fun `session and entry cookies survive a restart and are sent`() {
        val file = File(folder.root, "cookies.bin")
        jar(file).saveFromResponse(
            url,
            listOf(
                header("__Host-dl=abc; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=31536000"),
                header("__Host-dl-entry=xyz; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=31536000"),
            ),
        )
        val reopened = jar(file)
        assertThat(reopened.loadForRequest(url).map { it.name }).containsExactly("__Host-dl", "__Host-dl-entry")
        assertThat(file.readBytes().decodeToString()).doesNotContain("abc\"") // sealed, not plain JSON
    }

    @Test
    fun `a Keystore that refuses keeps the cookies in memory instead of failing the call`() {
        val broken = object : CookieVault {
            override fun seal(plain: ByteArray): ByteArray = throw java.security.KeyStoreException("AndroidKeyStore KeyStore not available")

            override fun open(sealed: ByteArray): ByteArray = throw java.security.KeyStoreException("AndroidKeyStore KeyStore not available")
        }
        val jar = PersistentCookieJar(File(folder.root, "cookies.bin"), broken) { now }
        jar.saveFromResponse(url, listOf(header("__Host-dl=abc; Path=/; Secure; Max-Age=600")))
        assertThat(jar.loadForRequest(url).map { it.name }).containsExactly("__Host-dl")
    }

    @Test
    fun `max-age zero removes the cookie`() {
        val jar = jar()
        jar.saveFromResponse(url, listOf(header("__Host-dl=abc; Path=/; Secure; Max-Age=600")))
        jar.saveFromResponse(url, listOf(header("__Host-dl=; Path=/; Secure; Max-Age=0")))
        assertThat(jar.loadForRequest(url)).isEmpty()
    }

    @Test
    fun `cookies are not sent to another host`() {
        val jar = jar()
        jar.saveFromResponse(url, listOf(header("__Host-dl=abc; Path=/; Secure; Max-Age=600")))
        assertThat(jar.loadForRequest("https://delulutopia.com/api/home".toHttpUrl())).isEmpty()
    }

    @Test
    fun `an unreadable file starts signed out`() {
        val file = File(folder.root, "cookies.bin").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        assertThat(jar(file).loadForRequest(url)).isEmpty()
    }
}
