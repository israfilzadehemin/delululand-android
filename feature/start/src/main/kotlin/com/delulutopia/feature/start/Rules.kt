package com.delulutopia.feature.start

/** Doc "Delululand — Universe" U5, U7, U9 (web src/api/universe.ts, src/profile/rules.ts). */
internal object Rules {
    private val universeName = Regex("^[\\p{L}\\p{Nd}]{2,12}$")
    private val username = Regex("^[\\p{L}\\p{Nd}]{3,16}$")
    const val WELCOME_MAX = 1500

    fun nameOk(name: String) = universeName.matches(name.trim())

    fun usernameOk(name: String) = username.matches(name)

    fun usernameTaken(name: String, taken: List<String>) = taken.any { it.equals(name, ignoreCase = true) }

    fun welcomeLength(welcome: String) = welcome.trim().codePointCount(0, welcome.trim().length)

    /** U5: the three paragraphs with the universe's name in place of "Delulutopia". */
    fun welcomeTemplate(name: String): String {
        val n = name.trim().ifEmpty { "Your universe" }
        return listOf(
            "$n is a place free from the negativity, limitations, and challenges of the real world. A little universe of its own, " +
                "belonging to two adults who refuse to let adulthood define them. A place where reality can be forgotten, " +
                "imagination knows no boundaries, and creativity is free to run wild.",
            "If you hold the key to this place, it means you're one of its citizens. You belong here. Enter the key below, " +
                "step inside, and surrender yourself to the embrace of $n.",
            "But if you don't have the key, then you were never meant to be here. Don't disturb the magic, don't spoil the " +
                "fantasy. Kindly turn around and leave. Immediately.",
        ).joinToString("\n\n")
    }

    fun paragraphs(welcome: String): List<String> = welcome.split(Regex("\\n\\s*\\n")).map { it.trim() }.filter { it.isNotEmpty() }
}
