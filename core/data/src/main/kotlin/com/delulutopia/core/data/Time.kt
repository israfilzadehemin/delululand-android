package com.delulutopia.core.data

import com.delulutopia.core.i18n.Dates
import com.delulutopia.core.i18n.localDigits
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ServerTime
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/** Instants and clock text (web src/clock): the viewer's zone, 24-hour, in the language in force. */
object Time {
    fun ms(iso: String?): Long? = iso?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    fun now(): Long = ServerTime.nowMs()

    private fun zoned(ms: Long, zone: ZoneId = ZoneId.systemDefault()) = ZonedDateTime.ofInstant(Instant.ofEpochMilli(ms), zone)

    /**
     * FS-T6: `HH:MM` today; `27 Oct, 21:10` another day this year; `27 Oct 2025, 21:10` in another year. In the
     * language in force (L4): Azerbaijani month names; Persian solar dates and digits.
     */
    fun clock(iso: String?, nowMs: Long = now()): String {
        val ms = ms(iso) ?: return ""
        val d = zoned(ms)
        val today = zoned(nowMs)
        val time = Dates.time(d)
        return when {
            d.toLocalDate() == today.toLocalDate() -> time
            else -> t("common.dayTime", "day" to Dates.day(d, year = !Dates.sameYear(d, today)), "time" to time)
        }
    }

    /** `HH:MM` only (S5, the edit header). */
    fun time(iso: String?): String = ms(iso)?.let { Dates.time(zoned(it)) } ?: ""

    fun timeOf(ms: Long): String = Dates.time(zoned(ms))

    /** A day without the time: `10 Nov` this year, `10 Nov 2025` in another (Discussion Controls A3). */
    fun day(iso: String?, nowMs: Long = now()): String {
        val d = zoned(ms(iso) ?: return "")
        return Dates.day(d, year = !Dates.sameYear(d, zoned(nowMs)))
    }

    /** mm:ss for a remaining duration; never negative; hours never appear. */
    fun countdown(remainingMs: Long): String {
        val total = (remainingMs.coerceAtLeast(0) + 999) / 1000
        return localDigits("${total / 60}:${(total % 60).toString().padStart(2, '0')}")
    }

    /** `PT1H` → 3 600 000, `PT6M` → 360 000, `PT30S` → 30 000. */
    fun isoDurationMs(iso: String): Long? {
        val m = Regex("^PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+(?:\\.\\d+)?)S)?$").find(iso) ?: return null
        val h = m.groupValues[1].ifEmpty { "0" }.toLong()
        val min = m.groupValues[2].ifEmpty { "0" }.toLong()
        val s = m.groupValues[3].ifEmpty { "0" }.toDouble()
        return ((h * 3600 + min * 60) * 1000 + (s * 1000).toLong())
    }

    /** `mm:ss` of a duration, for the TEST banner ("DELAY 6:00 · WINDOW 0:30"). */
    fun durationText(iso: String): String {
        val ms = isoDurationMs(iso) ?: return iso
        val total = Math.round(ms / 1000.0)
        return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
    }

    /** Hour (0–23) and minute of [nowMs] on the wall clock of [zone], DST included; unknown zones read as UTC. */
    fun zonedTime(nowMs: Long, zone: String): Pair<Int, Int> {
        val z = runCatching { ZoneId.of(zone) }.getOrDefault(ZoneId.of("UTC"))
        val d = zoned(nowMs, z)
        return d.hour to d.minute
    }

    fun cityOf(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

    fun isNight(hour: Int) = hour >= 22 || hour < 7
}

/** Letter states (Technical Design §3.2): the server's answer is authoritative; this flips the UI at 0:00. */
object LetterState {
    const val IN_REACH = "IN_REACH"
    const val IN_TRANSIT = "IN_TRANSIT"
    const val DELIVERED = "DELIVERED"
    const val READ = "READ"
    const val ARRIVED = "ARRIVED"

    fun of(nowMs: Long, windowEndsAt: String, arrivesAt: String, readAt: String?): String = when {
        readAt != null -> READ
        nowMs >= (Time.ms(arrivesAt) ?: 0) -> DELIVERED
        nowMs >= (Time.ms(windowEndsAt) ?: 0) -> IN_TRANSIT
        else -> IN_REACH
    }

    /** Discussions have no Read state (Discussions §2). */
    fun discussion(nowMs: Long, windowEndsAt: String, arrivesAt: String): String = when {
        nowMs >= (Time.ms(arrivesAt) ?: 0) -> ARRIVED
        nowMs >= (Time.ms(windowEndsAt) ?: 0) -> IN_TRANSIT
        else -> IN_REACH
    }
}
