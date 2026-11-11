package com.delulutopia.feature.you

import com.delulutopia.core.network.model.Profile

/** Q2, Q3 (web src/profile/rules.ts): the API checks the same rules. */
private val USERNAME_RULE = Regex("^[\\p{L}\\p{Nd}]{3,16}$")
private val NAME_RULE = Regex("^\\p{L}[\\p{L} '-]{0,39}$")

data class ProfileForm(val username: String = "", val firstName: String = "", val lastName: String = "", val timezone: String = "") {
    fun with(field: String, value: String) = when (field) {
        "username" -> copy(username = value)
        "firstName" -> copy(firstName = value)
        "lastName" -> copy(lastName = value)
        else -> copy(timezone = value)
    }

    /** Client-side messages for the three text fields; empty = fine. */
    fun problems(): Map<String, String> {
        val out = mutableMapOf<String, String>()
        if (!USERNAME_RULE.matches(username)) out["username"] = "3–16 letters or digits, no spaces"
        for ((k, raw) in listOf("firstName" to firstName, "lastName" to lastName)) {
            val v = raw.trim().replace(Regex("\\s+"), " ")
            if (v.isNotEmpty() && v.codePointCount(0, v.length) > 40) {
                out[k] = "Up to 40 characters"
            } else if (v.isNotEmpty() && !NAME_RULE.matches(v)) {
                out[k] = "Letters, spaces, - and ’ only"
            }
        }
        return out
    }

    /** "EI" from name and surname, else the first letter of the name or username. */
    fun initials(): String {
        val first = firstName.trim()
        val last = lastName.trim()
        if (first.isNotEmpty() && last.isNotEmpty()) return (first.take(1) + last.take(1)).uppercase()
        return first.ifEmpty { username }.take(1).uppercase()
    }

    companion object {
        fun of(p: Profile) = ProfileForm(p.username, p.firstName.orEmpty(), p.lastName.orEmpty(), p.timezone)
    }
}

/** Notifications: section titles and event words with the friend's name (web src/api/me.ts). */
val SECTION_LABELS = mapOf("letters" to "Letters", "discussions" to "Discussions", "emails" to "Emails", "board" to "Board", "home" to "Home")

fun eventLabel(key: String, friend: String): Pair<String, String> = when (key) {
    "letter.arrived" -> "A letter arrives" to "When $friend's letter reaches you"
    "letter.incoming" -> "A letter is on its way" to "When $friend sends one · the arrival time only"
    "letter.read" -> "$friend read your letter" to "When it is opened for the first time"
    "discussion.arrived" -> "A discussion arrives" to "When $friend's discussion reaches you"
    "discussion.comment" -> "New comment" to "On any discussion"
    "discussion.reaction" -> "Reaction on your post" to "On your discussions and comments"
    "email.new" -> "New email" to "Every email from $friend"
    "email.reaction" -> "Reaction on your email" to ""
    "board.doodle" -> "New doodle" to "$friend drew on or edited the board"
    "board.reaction" -> "Reaction on your doodle" to ""
    "board.note" -> "Note on your doodle" to ""
    "mood.changed" -> "$friend changed mood" to "The last change within a minute"
    else -> key to ""
}

/** "2 of 3 on" / "None on" under an on section's title when some of its events are off. */
fun sectionCount(on: Int, total: Int): String? = when {
    on == total -> null
    on == 0 -> "None on"
    else -> "$on of $total on"
}

/** U5 rules, as the API checks them. */
fun universeNameError(name: String): String? = if (Regex("^[\\p{L}\\p{Nd}]{2,12}$").matches(name.trim())) null else "2–12 letters or digits"

const val WELCOME_MAX = 1500

fun welcomeError(welcome: String): String? {
    val w = welcome.trim()
    if (w.isEmpty()) return "Write a few words"
    return if (w.codePointCount(0, w.length) > WELCOME_MAX) "Up to 1,500 characters" else null
}
