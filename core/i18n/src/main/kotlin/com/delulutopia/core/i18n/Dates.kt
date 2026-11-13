package com.delulutopia.core.i18n

import android.icu.text.SimpleDateFormat
import android.icu.util.Calendar
import android.icu.util.TimeZone
import android.icu.util.ULocale
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Day and time text in the language in force (L4): 24-hour everywhere; English `27 Oct`, Azerbaijani
 * `27 oktyabr`, Persian the solar (Jalali) calendar `۵ آبان` with Persian digits. "This year" is counted in
 * the calendar shown.
 */
object Dates {
    private val TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.UK)
    private val EN_DAY = DateTimeFormatter.ofPattern("d MMM", Locale.UK)
    private val EN_DAY_YEAR = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.UK)

    /** Written here so every phone says the same (the web does the same: Chrome has no Azerbaijani dates). */
    private val AZ_MONTHS = listOf(
        "yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avqust", "sentyabr", "oktyabr", "noyabr", "dekabr",
    )

    // Lazy: only Persian touches ICU, so English and Azerbaijani format on a plain JVM too.
    private val PERSIAN by lazy { ULocale("fa_IR@calendar=persian") }

    fun time(d: ZonedDateTime, lang: Lang = I18n.lang): String = localDigits(d.format(TIME), lang)

    /** `10 Nov`, `10 noyabr`, `۲۰ آبان`; with [year] the year follows. */
    fun day(d: ZonedDateTime, year: Boolean, lang: Lang = I18n.lang): String = when (lang) {
        Lang.EN -> d.format(if (year) EN_DAY_YEAR else EN_DAY)
        Lang.AZ -> "${d.dayOfMonth} ${AZ_MONTHS[d.monthValue - 1]}" + if (year) " ${d.year}" else ""
        Lang.FA -> persian(d, if (year) "d MMMM y" else "d MMMM")
    }

    /** Whether [a] and [b] fall in the same year of the calendar shown. */
    fun sameYear(a: ZonedDateTime, b: ZonedDateTime, lang: Lang = I18n.lang): Boolean =
        if (lang == Lang.FA) persianYear(a) == persianYear(b) else a.year == b.year

    private fun persian(d: ZonedDateTime, pattern: String): String {
        val f = SimpleDateFormat(pattern, PERSIAN)
        f.timeZone = TimeZone.getTimeZone(d.zone.id)
        return localDigits(f.format(Date(d.toInstant().toEpochMilli())), Lang.FA)
    }

    private fun persianYear(d: ZonedDateTime): Int {
        val c = Calendar.getInstance(TimeZone.getTimeZone(d.zone.id), PERSIAN)
        c.timeInMillis = d.toInstant().toEpochMilli()
        return c.get(Calendar.YEAR)
    }
}
