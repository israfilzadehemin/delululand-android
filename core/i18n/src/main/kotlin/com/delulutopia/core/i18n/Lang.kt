package com.delulutopia.core.i18n

import java.util.Locale

/** The three languages (doc "Delululand — Languages" L1). [tag] is what the API stores and accepts. */
enum class Lang(val tag: String, val rtl: Boolean, val native: String) {
    EN("en", false, "English"),
    AZ("az", false, "Azərbaycanca"),
    FA("fa", true, "فارسی"),
    ;

    /** Formatting locale: en-GB for English (24-hour, day before month), az-AZ, fa-IR. */
    val locale: Locale
        get() = when (this) {
            EN -> Locale.UK
            AZ -> Locale.forLanguageTag("az-AZ")
            FA -> Locale.forLanguageTag("fa-IR")
        }

    companion object {
        fun parse(tag: String?): Lang? = entries.firstOrNull { it.tag == tag }

        /** L6: before sign-in, the phone's language when it is Azerbaijani or Persian, else English. */
        fun device(locale: Locale = Locale.getDefault()): Lang = when (locale.language) {
            "az" -> AZ
            "fa" -> FA
            else -> EN
        }
    }
}
