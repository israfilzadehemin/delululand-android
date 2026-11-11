package com.delulutopia.core.network.cookie

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/**
 * The browser's cookie jar, on a phone (decision B1): the session (`__Host-dl`) and the entry pass
 * (`__Host-dl-entry`) live here exactly as they would in a browser, so the API needs no change.
 * Cookies are kept in memory and written, sealed by [CookieVault], to [file] on every change. A
 * Set-Cookie with Max-Age=0 removes the cookie; expired cookies are never sent.
 */
class PersistentCookieJar(
    private val file: File,
    private val vault: CookieVault,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {

    private val cookies: MutableList<Cookie> by lazy { load() }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        cookies.forEach { incoming ->
            this.cookies.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
            if (incoming.expiresAt > now()) this.cookies += incoming
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val time = now()
        if (cookies.removeAll { it.expiresAt <= time }) persist()
        return cookies.filter { it.matches(url) }
    }

    /** Names of the cookies held now, for tests and diagnostics (never their values). */
    @Synchronized
    fun names(): List<String> = cookies.map { it.name }

    @Synchronized
    fun clear() {
        cookies.clear()
        persist()
    }

    private fun load(): MutableList<Cookie> = runCatching {
        if (!file.exists()) return@runCatching mutableListOf()
        val json = vault.open(file.readBytes()).decodeToString()
        Json.decodeFromString(ListSerializer(StoredCookie.serializer()), json).mapNotNull { it.toCookie() }.toMutableList()
    }.getOrElse { mutableListOf() } // A key lost with a restore or a corrupt file: start signed out.

    private fun persist() {
        val json = Json.encodeToString(ListSerializer(StoredCookie.serializer()), cookies.filter { it.persistent }.map(StoredCookie::of))
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, file.name + ".tmp")
        tmp.writeBytes(vault.seal(json.encodeToByteArray()))
        tmp.renameTo(file)
    }

    @Serializable
    private data class StoredCookie(
        val name: String,
        val value: String,
        val expiresAt: Long,
        val domain: String,
        val path: String,
        val secure: Boolean,
        val httpOnly: Boolean,
        val hostOnly: Boolean,
        val sameSite: String? = null,
    ) {
        fun toCookie(): Cookie? = runCatching {
            Cookie.Builder()
                .name(name)
                .value(value)
                .expiresAt(expiresAt)
                .path(path)
                .apply {
                    if (hostOnly) hostOnlyDomain(domain) else domain(domain)
                    if (secure) secure()
                    if (httpOnly) httpOnly()
                    if (sameSite != null) sameSite(sameSite)
                }.build()
        }.getOrNull()

        companion object {
            fun of(c: Cookie) = StoredCookie(c.name, c.value, c.expiresAt, c.domain, c.path, c.secure, c.httpOnly, c.hostOnly, c.sameSite)
        }
    }
}
