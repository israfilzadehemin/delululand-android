package com.delulutopia.feature.start

import com.delulutopia.core.i18n.t

/** Doc "Delululand — Universe" U5, U7, U9 (web src/api/universe.ts, src/profile/rules.ts). */
object Rules {
    private val universeName = Regex("^[\\p{L}\\p{Nd}]{2,12}$")
    private val username = Regex("^[\\p{L}\\p{Nd}]{3,16}$")
    const val WELCOME_MAX = 1500

    fun nameOk(name: String) = universeName.matches(name.trim())

    fun usernameOk(name: String) = username.matches(name)

    fun usernameTaken(name: String, taken: List<String>) = taken.any { it.equals(name, ignoreCase = true) }

    fun welcomeLength(welcome: String) = welcome.trim().codePointCount(0, welcome.trim().length)

    /** U5: the three paragraphs with the universe's name in place of "Delulutopia". */
    fun welcomeTemplate(name: String): String {
        val n = name.trim().ifEmpty { t("universe.yourUniverse") }
        // C52: the template is written in the creator's language.
        return listOf(t("universe.welcome.p1", "name" to n), t("universe.welcome.p2", "name" to n), t("universe.welcome.p3")).joinToString("\n\n")
    }

    fun paragraphs(welcome: String): List<String> = welcome.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
}
