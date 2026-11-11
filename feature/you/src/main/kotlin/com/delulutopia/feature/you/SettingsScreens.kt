package com.delulutopia.feature.you

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlSwitch
import com.delulutopia.core.designsystem.component.DlSwitchItem
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.notify.NotifyPermission
import com.delulutopia.core.ui.notify.rememberNotifyAccess

private val THEME_SUBS = mapOf(ThemeMode.Light to "Paper and navy", ThemeMode.Dark to "Dusk paper, cream ink", ThemeMode.System to "Follows your phone")

/** S2 Appearance, per device (DM3): Light / Dark / Match device and the falling leaves (L6). */
@Composable
fun AppearanceScreen(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit, vm: YouViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val context = LocalContext.current
    val leaves by vm.leaves.collectAsStateWithLifecycle()
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = "Appearance", onBack = { nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card {
                Column(Modifier.selectableGroup()) {
                    listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System).forEach { mode ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 72.dp)
                                .selectable(selected = mode == themeMode, role = Role.RadioButton, onClick = { onThemeMode(mode) })
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(THEME_WORDS.getValue(mode), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                Text(THEME_SUBS.getValue(mode), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected = mode == themeMode, onClick = null)
                        }
                    }
                }
            }
            Card {
                DlSwitchItem(
                    "Seasonal effects",
                    leaves,
                    vm::setLeaves,
                    supporting = if (reduced) "Falling leaves · off while your phone removes animations" else "Falling leaves",
                )
            }
            Text(
                "Saved on this phone only.",
                Modifier.padding(start = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** S3–S7: this phone, Pause all, sections and events; each switch saves at once. */
@Composable
fun NotificationsScreen(vm: NotificationsViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val friend = homeState.home?.friend?.nickname ?: "your friend"
    val fail = { notice("Couldn't save. Try again.") }
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = "Notifications", onBack = { nav.back() })
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DeviceCard()
            val s = vm.settings
            if (s == null) {
                LoadOrFail(vm.failed)
                return@Column
            }
            Card {
                Box(
                    Modifier.background(
                        if (s.paused) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    ),
                ) {
                    DlSwitchItem(
                        "Pause all",
                        s.paused,
                        { vm.change(paused = it, onFail = fail) },
                        supporting = if (s.paused) "Nothing is pushed to any of your devices" else "Stop every push on all your devices",
                    )
                }
            }
            Text(
                "These choices follow you to every device. Dots and NEW marks inside the app stay as they are.",
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            s.sections.forEach { sec ->
                val title = SECTION_LABELS[sec.key] ?: sec.key
                Card {
                    val count = if (sec.enabled) sectionCount(sec.events.count { it.enabled }, sec.events.size) else null
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 24.dp, top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                title,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (s.paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            count?.let { Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        DlSwitch(sec.enabled, { vm.change(values = mapOf("section.${sec.key}" to it), onFail = fail) }, enabled = !s.paused)
                    }
                    sec.events.forEach { e ->
                        val (label, hint) = eventLabel(e.key, friend)
                        DlSwitchItem(
                            label,
                            e.enabled,
                            { vm.change(values = mapOf(e.key to it), onFail = fail) },
                            supporting = hint.ifEmpty { null },
                            enabled = !s.paused && sec.enabled,
                        )
                    }
                }
            }
        }
    }
}

/** S3 for Android: this phone's permission, with Allow while it can still be given. */
@Composable
private fun DeviceCard() {
    val access = rememberNotifyAccess()
    val (state, sub) = when (access.permission) {
        NotifyPermission.Granted -> "On" to "Notifications reach this phone."
        NotifyPermission.Denied -> "Blocked" to "Notifications are off for this app in Android settings. Allow opens them."
        NotifyPermission.Default -> "Off" to "This phone won't show notifications."
    }
    val on = access.permission == NotifyPermission.Granted
    Card {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(10.dp).background(if (on) DlTheme.colors.userB else MaterialTheme.colorScheme.outline, CircleShape))
            Column(Modifier.weight(1f)) {
                Text("This phone · $state", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(sub, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!on) DlFilledButton("Allow", access.allow)
        }
    }
}
