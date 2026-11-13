package com.delulutopia.feature.emails

import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn
import com.delulutopia.core.network.model.EmailCredits
import java.text.BreakIterator

/** Emails Q4, Q5: plain text, counted in grapheme clusters, at most 1024. */
const val EMAIL_MAX = 1024
const val SUBJECT_MAX = 120
val TRIMMED: String get() = t("emails.trimmed")

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
    return if (c.left > 0) tn("emails.creditsLeft", c.left, "time" to renews) else t("emails.noCredits", "time" to renews)
}

/** Words for an email without text (FS-E3, Q3). */
fun mediaWords(photos: Int, audios: Int, videos: Int): String? = when {
    videos > 0 -> t("emails.media.video")
    photos > 0 -> if (photos > 1) t("emails.media.photos", "n" to photos) else t("emails.media.photo")
    audios > 0 -> if (audios > 1) t("emails.media.voiceNotes", "n" to audios) else t("emails.media.voiceNote")
    else -> null
}

fun emailCountWords(n: Int): String = tn("emails.count", n)
