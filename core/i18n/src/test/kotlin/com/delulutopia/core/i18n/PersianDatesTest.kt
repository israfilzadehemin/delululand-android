package com.delulutopia.core.i18n

import com.google.common.truth.Truth.assertThat
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Persian days need Android's ICU (the solar calendar), so they run under Robolectric. */
@RunWith(RobolectricTestRunner::class)
class PersianDatesTest {
    @Test fun solarDatesInPersianDigits() {
        // 10 Nov 2026 is 19 Aban 1405.
        val d = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        assertThat(Dates.day(d, year = false, lang = Lang.FA)).isEqualTo("۱۹ آبان")
        assertThat(Dates.day(d, year = true, lang = Lang.FA)).isEqualTo("۱۹ آبان ۱۴۰۵")
    }

    @Test fun thisYearIsCountedInTheCalendarShown() {
        // 10 Mar and 25 Mar 2026 fall in different solar years (Nowruz is 21 Mar).
        val before = ZonedDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        val after = ZonedDateTime.of(2026, 3, 25, 12, 0, 0, 0, ZoneId.of("Asia/Tehran"))
        assertThat(Dates.sameYear(before, after, Lang.FA)).isFalse()
        assertThat(Dates.sameYear(before, after, Lang.EN)).isTrue()
    }
}
