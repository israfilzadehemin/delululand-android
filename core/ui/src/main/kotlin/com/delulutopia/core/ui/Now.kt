package com.delulutopia.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Time
import kotlinx.coroutines.delay

/** The server-aligned now, re-read every [intervalMs] (web `useNow`). */
@Composable
fun rememberNow(intervalMs: Long = 1_000): Long {
    var now by remember { mutableLongStateOf(Time.now()) }
    LaunchedEffect(intervalMs) {
        while (true) {
            now = Time.now()
            delay(intervalMs - now % intervalMs)
        }
    }
    return now
}

/** FS-M7: "just now", "N min ago", "N h ago", "N d ago". */
fun ageWords(sinceIso: String?, nowMs: Long): String {
    val since = Time.ms(sinceIso) ?: return ""
    val s = ((nowMs - since) / 1000).coerceAtLeast(0)
    if (s < 60) return "just now"
    val m = s / 60
    if (m < 60) return "$m min ago"
    val h = m / 60
    if (h < 24) return "$h h ago"
    return "${h / 24} d ago"
}
