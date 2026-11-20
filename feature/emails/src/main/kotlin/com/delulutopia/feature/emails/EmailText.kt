package com.delulutopia.feature.emails

import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.i18n.tn
import com.delulutopia.core.network.model.Email
import com.delulutopia.core.network.model.EmailCredits
import com.delulutopia.core.network.model.EmailQuote
import com.delulutopia.core.network.model.ThreadMedia
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

private fun visualOf(e: Email) = e.attachments.firstOrNull { (it.kind == "photo" || it.kind == "video") && it.thumbUrl != null }

/** The quote of an email already on screen, built the way the server builds `replyTo` (Email Replies RE11). */
fun quoteOf(e: Email): EmailQuote {
    val photos = e.attachments.count { it.kind == "photo" }
    val audios = e.attachments.count { it.kind == "audio" }
    val videos = e.attachments.count { it.kind == "video" }
    val oneLine = e.text.replace(Regex("\\s+"), " ").trim()
    val text = if (oneLine.isEmpty()) null else oneLine.take(oneLine.offsetByCodePoints(0, minOf(120, oneLine.codePointCount(0, oneLine.length))))
    return EmailQuote(
        id = e.id,
        senderId = e.senderId,
        text = text,
        media = ThreadMedia(photos, audios, videos),
        thumbUrl = visualOf(e)?.takeUnless { it.expired }?.thumbUrl,
        thumbExpired = visualOf(e)?.expired == true,
        sentAt = e.sentAt,
    )
}

/** RE9, RE11: the quote's text, else "Photo" / "Voice note" / "Video". */
fun quoteLine(q: EmailQuote): String =
    q.text ?: q.media?.let { mediaWords(it.photos, it.audios, it.videos) } ?: ""

/** RE6: the distance that arms a reply and the most a bubble follows the finger, in dp. */
const val SWIPE_ARM_DP = 64f
const val SWIPE_MAX_DP = 96f

/**
 * Where the bubble sits for a finger moved by [dx] (dp): toward the column's centre only ([toward]
 * +1 = right, −1 = left), eased past the arm point, never past the max. Same as web replyGesture.
 */
fun swipeOffset(dx: Float, toward: Int): Float {
    val along = dx * toward
    if (along <= 0f) return 0f
    val eased = if (along <= SWIPE_ARM_DP) along else SWIPE_ARM_DP + (along - SWIPE_ARM_DP) * 0.35f
    return minOf(SWIPE_MAX_DP, eased) * toward
}
