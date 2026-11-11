package com.delulutopia.feature.emails

import com.delulutopia.core.data.Time
import com.delulutopia.core.network.model.EmailCredits
import java.text.BreakIterator

/** Emails Q4, Q5: plain text, counted in grapheme clusters, at most 1024. */
const val EMAIL_MAX = 1024
const val SUBJECT_MAX = 120
const val TRIMMED = "Trimmed to 1024 characters"

fun normalize(text: String): String = text.replace(Regex("\r\n?"), "\n")

fun graphemes(text: String): List<String> {
    val it = BreakIterator.getCharacterInstance()
    it.setText(text)
    val out = ArrayList<String>()
    var start = it.first()
    var end = it.next()
    while (end != BreakIterator.DONE) {
        out += text.substring(start, end)
        start = end
        end = it.next()
    }
    return out
}

fun count(text: String): Int = graphemes(text).size

/** The first [max] characters; `cut` says whether anything was dropped. */
fun clamp(text: String, max: Int = EMAIL_MAX): Pair<String, Boolean> {
    val n = normalize(text)
    val g = graphemes(n)
    return if (g.size <= max) n to false else g.take(max).joinToString("") to true
}

/** Q20, Q21: "N left today · renews at HH:MM", or the out-of-credits line. */
fun creditsLine(c: EmailCredits?): String? {
    if (c == null) return null
    val renews = Time.time(c.renewsAt)
    return if (c.left > 0) "${c.left} left today · renews at $renews" else "No emails left today · renews at $renews"
}

/** Words for an email without text (FS-E3, Q3). */
fun mediaWords(photos: Int, audios: Int, videos: Int): String? = when {
    videos > 0 -> "Video"
    photos > 0 -> if (photos > 1) "$photos photos" else "Photo"
    audios > 0 -> if (audios > 1) "$audios voice notes" else "Voice note"
    else -> null
}

fun emailCountWords(n: Int): String = if (n == 1) "1 email" else "$n emails"
