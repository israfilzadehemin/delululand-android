package com.delulutopia.core.i18n

import java.util.Locale

/**
 * The languages (doc "Delululand — Languages" L1, L15): English, Azerbaijani, Persian, Turkish, Russian.
 * [tag] is what the API stores and accepts; [native] is the language's name in its own words (L8).
 */
enum class Lang(val tag: String, val rtl: Boolean, val native: String) {
    EN("en", false, "English"),
    AZ("az", false, "Azərbaycanca"),
    FA("fa", true, "فارسی"),
    TR("tr", false, "Türkçe"),
    RU("ru", false, "Русский"),
    ;

    /** Formatting locale: en-GB for English (24-hour, day before month), az-AZ, fa-IR, tr-TR, ru-RU. */
    val locale: Locale
        get() = when (this) {
            EN -> Locale.UK
            AZ -> Locale.forLanguageTag("az-AZ")
            FA -> Locale.forLanguageTag("fa-IR")
            TR -> Locale.forLanguageTag("tr-TR")
            RU -> Locale.forLanguageTag("ru-RU")
        }

    companion object {
        fun parse(tag: String?): Lang? = entries.firstOrNull { it.tag == tag }

        /** L6: before sign-in, the phone's language when it is one of ours, else English. */
        fun device(locale: Locale = Locale.getDefault()): Lang = entries.firstOrNull { it != EN && it.tag == locale.language } ?: EN
    }
}
