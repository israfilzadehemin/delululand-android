package com.delulutopia.core.designsystem.component

import android.icu.text.SimpleDateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.i18n.t
import java.text.Normalizer
import java.util.Date
import java.util.Locale

/** One row of the time-zone picker (web src/profile/zones.ts). */
data class ZoneOption(val zone: String, val city: String, val offset: String, val time: String)

/** The picker's data, with the web's rules: "Area/City" IANA names only, city search first, accent-blind. */
object Zones {
    private val fallback = listOf("Europe/Warsaw", "Asia/Tehran", "Asia/Baku", "Europe/London", "America/New_York", "Asia/Tokyo")

    val all: List<String> by lazy {
        runCatching { TimeZone.getAvailableIDs().filter { it.contains('/') && !it.startsWith("Etc/") && !it.startsWith("SystemV/") } }
            .getOrNull()?.takeIf { it.isNotEmpty() } ?: fallback
    }

    /** This device's zone; a device without an "Area/City" zone gets one the API accepts. */
    fun device(): String = TimeZone.getDefault().id.takeIf { it.contains('/') } ?: "Etc/UTC"

    fun cityOf(zone: String): String = zone.substringAfterLast('/').replace('_', ' ')

    fun option(zone: String, nowMs: Long = System.currentTimeMillis()): ZoneOption {
        val tz = TimeZone.getTimeZone(zone)
        val time = SimpleDateFormat("HH:mm", Locale.UK).apply { timeZone = tz }.format(Date(nowMs))
        return ZoneOption(zone, cityOf(zone), offsetWords(tz.getOffset(nowMs)), time)
    }

    /** 3 600 000 → "UTC+1", −12 600 000 → "UTC−3:30", 0 → "UTC". */
    fun offsetWords(offsetMs: Int): String {
        if (offsetMs == 0) return "UTC"
        val sign = if (offsetMs < 0) "−" else "+"
        val minutes = kotlin.math.abs(offsetMs) / 60_000
        val h = minutes / 60
        val m = minutes % 60
        return "UTC$sign$h" + if (m == 0) "" else ":%02d".format(m)
    }

    fun match(query: String, zones: List<String> = all, limit: Int = 60): List<String> {
        val q = fold(query.trim())
        if (q.isEmpty()) return zones.take(limit)
        val starts = mutableListOf<String>()
        val contains = mutableListOf<String>()
        for (z in zones) {
            when {
                fold(cityOf(z)).startsWith(q) -> starts += z
                fold(z.replace('_', ' ')).contains(q) -> contains += z
            }
            if (starts.size >= limit) break
        }
        return (starts + contains).take(limit)
    }

    private fun fold(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD).replace(Regex("\\p{M}"), "").lowercase(Locale.ROOT)
}

/**
 * Time zone picker sheet (F-Timezone): this device's zone first, then matches for a city or zone
 * name, each with its current time and UTC offset. Used by Create, Join and Profile.
 */
@Composable
fun DlTimeZoneSheet(
    value: String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    title: String = t("profile.timeZone"),
    searchHint: String = t("profile.searchACityOrZone"),
    deviceLabel: String = t("profile.thisDevice"),
    matchesLabel: String = t("profile.matches"),
    allLabel: String = t("profile.allZones"),
    noMatch: (String) -> String = { t("profile.noTimeZoneMatches", "trim" to it) },
) {
    var query by remember { mutableStateOf("") }
    val device = remember { Zones.device() }
    val now = remember { System.currentTimeMillis() }
    val matches = remember(query) { Zones.match(query).filter { it != device } }
    val showDevice = device.contains('/') && (query.isBlank() || Zones.match(query, listOf(device)).isNotEmpty())

    DlBottomSheet(onDismissRequest = onDismiss) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            placeholder = { Text(searchHint) },
            leadingIcon = { Icon(DlIcons.Search, contentDescription = null) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
            if (showDevice) item { ZoneRow(Zones.option(device, now), device == value, deviceLabel, onPick) }
            if (matches.isNotEmpty()) {
                item {
                    DlSectionLabel(
                        if (query.isNotBlank()) matchesLabel else allLabel,
                        Modifier.padding(start = 24.dp, top = 12.dp, bottom = 4.dp),
                    )
                }
            }
            items(matches, key = { it }) { z -> ZoneRow(Zones.option(z, now), z == value, null, onPick) }
            if (matches.isEmpty() && !showDevice) {
                item {
                    Text(
                        noMatch(query.trim()),
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneRow(o: ZoneOption, selected: Boolean, deviceLabel: String?, onPick: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable { onPick(o.zone) }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(o.city, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                if (deviceLabel != null) {
                    Text(deviceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(
                t("discussions.metaLine", "author" to o.zone, "when" to o.offset),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(o.time, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
        if (selected) {
            Icon(DlIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        } else {
            Spacer(Modifier.size(20.dp))
        }
    }
}
