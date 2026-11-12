package com.delulutopia.core.data

import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Mood
import java.text.Normalizer

/** Moods (web src/text/moods.ts, doc "Delululand — Moods"). The API's catalogue is the source; these show while it loads. */
object Moods {
    val DEFAULTS: List<Mood> get() = listOf(
        "HAPPY" to ("😊" to t("mood.HAPPY")), "CALM" to ("🍵" to t("mood.CALM")), "CURIOUS" to ("🤔" to t("mood.CURIOUS")),
        "INSPIRED" to ("✨" to t("mood.INSPIRED")),
        "BUSY" to ("🏃" to t("mood.BUSY")), "TIRED" to ("😴" to t("mood.TIRED")), "SAD" to ("🌧️" to t("mood.SAD")),
        "MISSING_YOU" to ("💌" to t("mood.MISSING_YOU")),
        "LOVED" to ("🥰" to t("mood.LOVED")), "GRATEFUL" to ("🙏" to t("mood.GRATEFUL")), "EXCITED" to ("🤩" to t("mood.EXCITED")),
        "PLAYFUL" to ("😜" to t("mood.PLAYFUL")),
        "COZY" to ("🧸" to t("mood.COZY")), "HOPEFUL" to ("🌱" to t("mood.HOPEFUL")), "PROUD" to ("🏆" to t("mood.PROUD")),
        "THINKING_OF_YOU" to ("💭" to t("mood.THINKING_OF_YOU")),
        "NEED_A_HUG" to ("🤗" to t("mood.NEED_A_HUG")), "BUTTERFLIES" to ("🦋" to t("mood.BUTTERFLIES")), "TOUCHED" to ("🥹" to t("mood.TOUCHED")),
        "SAFE_WITH_YOU" to ("🫶" to t("mood.SAFE_WITH_YOU")), "BLISSFUL" to ("🌸" to t("mood.BLISSFUL")), "FLIRTY" to ("😏" to t("mood.FLIRTY")),
        "CONTENT" to ("☺️" to t("mood.CONTENT")), "DREAMY" to ("🌙" to t("mood.DREAMY")),
        "ANXIOUS" to ("😰" to t("mood.ANXIOUS")), "STRESSED" to ("🤯" to t("mood.STRESSED")), "ANGRY" to ("😤" to t("mood.ANGRY")),
        "LONELY" to ("🥀" to t("mood.LONELY")),
        "BORED" to ("🥱" to t("mood.BORED")), "SICK" to ("🤒" to t("mood.SICK")), "OVERWHELMED" to ("🌊" to t("mood.OVERWHELMED")),
        "HOMESICK" to ("🏡" to t("mood.HOMESICK")),
    ).map { (key, v) -> Mood(key, v.first, v.second, false) }

    const val PER_PAGE = 8
    const val LIMIT = 48
    const val LABEL_MAX = 16
    val LABEL_RULE: String get() = t("home.moodLabelRule")
    val LABEL_TAKEN: String get() = t("home.moodTaken")

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
