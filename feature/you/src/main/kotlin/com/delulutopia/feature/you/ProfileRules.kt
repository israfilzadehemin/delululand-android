package com.delulutopia.feature.you

import com.delulutopia.core.i18n.t
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
        if (!USERNAME_RULE.matches(username)) out["username"] = t("profile.316LettersOrDigits")
        for ((k, raw) in listOf("firstName" to firstName, "lastName" to lastName)) {
            val v = raw.trim().replace(Regex("\\s+"), " ")
            if (v.isNotEmpty() && v.codePointCount(0, v.length) > 40) {
                out[k] = t("profile.nameTooLong")
            } else if (v.isNotEmpty() && !NAME_RULE.matches(v)) {
                out[k] = t("profile.nameRule")
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
val SECTION_LABELS get() = mapOf(
    "letters" to t("letters.letters"),
    "discussions" to t("discussions.discussions"),
    "emails" to t("emails.emails"),
    "board" to t("board.board"),
    "home" to t("notFound.home"),
)

fun eventLabel(key: String, friend: String): Pair<String, String> = when (key) {
    "letter.arrived" -> t("notifyEvent.letterArrived") to t("notifyEvent.letterArrivedHint", "friend" to friend)
    "letter.incoming" -> t("notifyEvent.letterIncoming") to t("notifyEvent.letterIncomingHint", "friend" to friend)
    "letter.read" -> t("notifyEvent.letterRead", "friend" to friend) to t("notifyEvent.letterReadHint")
    "discussion.arrived" -> t("notifyEvent.discussionArrived") to t("notifyEvent.discussionArrivedHint", "friend" to friend)
    "discussion.comment" -> t("notifyEvent.discussionComment") to t("notifyEvent.discussionCommentHint")
    "discussion.reaction" -> t("notifyEvent.discussionReaction") to t("notifyEvent.discussionReactionHint")
    "email.new" -> t("emails.newEmail") to t("notifyEvent.emailNewHint", "friend" to friend)
    "email.reaction" -> t("notifyEvent.emailReaction") to ""
    "board.doodle" -> t("notifyEvent.boardDoodle") to t("notifyEvent.boardDoodleHint", "friend" to friend)
    "board.reaction" -> t("notifyEvent.boardReaction") to ""
    "board.note" -> t("notifyEvent.boardNote") to ""
    "mood.changed" -> t("notifyEvent.moodChanged", "friend" to friend) to t("notifyEvent.moodChangedHint")
    else -> key to ""
}

/** "2 of 3 on" / "None on" under an on section's title when some of its events are off. */
fun sectionCount(on: Int, total: Int): String? = when {
    on == total -> null
    on == 0 -> t("settings.noneOn")
    else -> t("settings.someOn", "on" to on, "total" to total)
}

/** U5 rules, as the API checks them. */
fun universeNameError(name: String): String? = if (Regex("^[\\p{L}\\p{Nd}]{2,12}$").matches(name.trim())) null else t("universe.nameRule")

const val WELCOME_MAX = 1500

fun welcomeError(welcome: String): String? {
    val w = welcome.trim()
    if (w.isEmpty()) return t("universe.writeAFewWords")
    return if (w.codePointCount(0, w.length) > WELCOME_MAX) t("universe.welcomeMax") else null
}
