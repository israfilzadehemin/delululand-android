package com.delulutopia.core.data

import com.delulutopia.core.network.model.Mood
import java.text.Normalizer

/** Moods (web src/text/moods.ts, doc "Delululand — Moods"). The API's catalogue is the source; these show while it loads. */
object Moods {
    val DEFAULTS: List<Mood> = listOf(
        "HAPPY" to ("😊" to "Happy"), "CALM" to ("🍵" to "Calm"), "CURIOUS" to ("🤔" to "Curious"), "INSPIRED" to ("✨" to "Inspired"),
        "BUSY" to ("🏃" to "Busy"), "TIRED" to ("😴" to "Tired"), "SAD" to ("🌧️" to "Sad"), "MISSING_YOU" to ("💌" to "Missing you"),
        "LOVED" to ("🥰" to "Loved"), "GRATEFUL" to ("🙏" to "Grateful"), "EXCITED" to ("🤩" to "Excited"), "PLAYFUL" to ("😜" to "Playful"),
        "COZY" to ("🧸" to "Cozy"), "HOPEFUL" to ("🌱" to "Hopeful"), "PROUD" to ("🏆" to "Proud"), "THINKING_OF_YOU" to ("💭" to "Thinking of you"),
        "NEED_A_HUG" to ("🤗" to "Need a hug"), "BUTTERFLIES" to ("🦋" to "Butterflies"), "TOUCHED" to ("🥹" to "Touched"),
        "SAFE_WITH_YOU" to ("🫶" to "Safe with you"), "BLISSFUL" to ("🌸" to "Blissful"), "FLIRTY" to ("😏" to "Flirty"),
        "CONTENT" to ("☺️" to "Content"), "DREAMY" to ("🌙" to "Dreamy"),
        "ANXIOUS" to ("😰" to "Anxious"), "STRESSED" to ("🤯" to "Stressed"), "ANGRY" to ("😤" to "Angry"), "LONELY" to ("🥀" to "Lonely"),
        "BORED" to ("🥱" to "Bored"), "SICK" to ("🤒" to "Sick"), "OVERWHELMED" to ("🌊" to "Overwhelmed"), "HOMESICK" to ("🏡" to "Homesick"),
    ).map { (key, v) -> Mood(key, v.first, v.second, false) }

    const val PER_PAGE = 8
    const val LIMIT = 48
    const val LABEL_MAX = 16
    const val LABEL_RULE = "Up to 16 letters, digits or simple punctuation"
    const val LABEL_TAKEN = "That mood already exists."

    /** "🌧️ Sad", or null for no mood. An emoji and label from the API win over the code. */
    fun text(code: String?, emoji: String? = null, label: String? = null): String? {
        if (code == null) return null
        if (!emoji.isNullOrEmpty() && !label.isNullOrEmpty()) return "$emoji $label"
        return DEFAULTS.firstOrNull { it.key == code }?.let { "${it.emoji} ${it.label}" }
    }

    private val labelChars = Regex("^[\\p{L}\\p{M}\\p{N} .,!?'’&()-]+$")
    private val letterOrDigit = Regex("[\\p{L}\\p{N}]")

    fun normalise(raw: String): String = Normalizer.normalize(raw, Normalizer.Form.NFC).trim().replace(Regex("\\s+"), " ")

    fun labelLength(raw: String): Int = normalise(raw).let { it.codePointCount(0, it.length) }

    /** Why [raw] can't be saved, or null; empty is not reported (Save is simply off). */
    fun labelProblem(raw: String, catalogue: List<Mood>, exceptKey: String?): String? {
        val l = normalise(raw)
        if (l.isEmpty()) return null
        if (labelLength(l) > LABEL_MAX || !labelChars.matches(l) || !letterOrDigit.containsMatchIn(l)) return LABEL_RULE
        if (catalogue.any { it.key != exceptKey && it.label.equals(l, ignoreCase = true) }) return LABEL_TAKEN
        return null
    }
}
