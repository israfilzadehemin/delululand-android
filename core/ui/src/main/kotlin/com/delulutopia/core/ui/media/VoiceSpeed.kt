package com.delulutopia.core.ui.media

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang

/**
 * Voice-note playback speed (doc "Delululand — Voice Note Speed" VS3–VS9; web media/speed.ts). One
 * chip on every voice-note player cycles 0.5× → 1× → 1.5× → 2× → 0.5×. The last pick is kept on this
 * phone (VS4, the "device" preferences like the reaction recents) and applies to every note.
 */
object VoiceSpeed {
    val SPEEDS = floatArrayOf(0.5f, 1f, 1.5f, 2f)
    const val DEFAULT = 1f
    private const val PREFS = "device"
    private const val KEY = "voice-speed"

    var speed by mutableFloatStateOf(DEFAULT)
        private set
    private var loaded = false

    /** Reads the kept pick once per process; an unknown value falls back to 1×. */
    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val v = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY, DEFAULT)
        speed = if (v in SPEEDS) v else DEFAULT
    }

    fun next(s: Float): Float = SPEEDS[(SPEEDS.indexOf(s).coerceAtLeast(0) + 1) % SPEEDS.size]

    /** VS5: applies at once to the note that plays, from its current position. */
    fun set(context: Context, s: Float) {
        speed = s
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY, s).apply()
        OnePlayer.setSpeed(s)
    }

    /** "1.5×"; Persian digits and the Persian decimal sign in Persian, "۱٫۵×" (VS9). */
    fun label(s: Float, lang: Lang = I18n.lang): String {
        val n = if (s % 1f == 0f) s.toInt().toString() else s.toString()
        if (lang != Lang.FA) return "$n×"
        val b = StringBuilder()
        for (c in n) b.append(if (c in '0'..'9') '۰' + (c - '0') else if (c == '.') '٫' else c)
        return "$b×"
    }

    /** Test-only. */
    internal fun reset() {
        speed = DEFAULT
        loaded = false
    }
}
