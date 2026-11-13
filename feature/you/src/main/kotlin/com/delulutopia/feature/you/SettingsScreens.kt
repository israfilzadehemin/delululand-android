package com.delulutopia.feature.you

import android.provider.Settings
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.Stroke
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
import com.delulutopia.core.i18n.t
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.notify.NotifyPermission
import com.delulutopia.core.ui.notify.rememberNotifyAccess

private val THEME_SUBS get() = mapOf(
    ThemeMode.Light to t("settings.theme.lightSub"),
    ThemeMode.Dark to t("settings.theme.darkSub"),
    ThemeMode.System to t("settings.theme.matchSubPhone"),
)

/** S2 Appearance, per device (DM3): Light / Dark / Match device and the falling leaves (L6). */
@Composable
fun AppearanceScreen(themeMode: ThemeMode, onThemeMode: (ThemeMode) -> Unit, vm: YouViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val context = LocalContext.current
    val leaves by vm.leaves.collectAsStateWithLifecycle()
    val reduced = remember { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = t("settings.appearance"), onBack = { nav.back() })
        // web .dl-sub-main.dl-settings: 4/20/24 padding, 12 dp between blocks.
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card {
                // web .dl-set-radio: the radio leads, 64 dp, 14 dp gap.
                listOf(ThemeMode.Light, ThemeMode.Dark, ThemeMode.System).forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 64.dp)
                            .selectable(selected = mode == themeMode, role = Role.RadioButton, onClick = { onThemeMode(mode) })
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Radio(mode == themeMode)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(THEME_WORDS.getValue(mode), fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold)
                            Text(THEME_SUBS.getValue(mode), fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Card {
                DlSwitchItem(
                    t("settings.seasonalEffects"),
                    leaves,
                    vm::setLeaves,
                    supporting = if (reduced) t("settings.leavesReducedPhone") else t("settings.leaves"),
                )
            }
            SubNote(t("settings.savedOnThisPhoneOnly"))
        }
    }
}

/** S3–S7: this phone, Pause all, sections and events; each switch saves at once. */
@Composable
fun NotificationsScreen(vm: NotificationsViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val friend = homeState.home?.friend?.nickname ?: t("settings.yourFriend")
    val fail = { notice(t("common.couldntSave")) }
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = t("settings.notifications"), onBack = { nav.back() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DeviceCard()
            val s = vm.settings
            if (s == null) {
                LoadOrFail(vm.failed)
                return@Column
            }
            // web .dl-set-pause--on: the seal tint while everything is paused.
            Card(background = if (s.paused) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                DlSwitchItem(
                    t("settings.pauseAll"),
                    s.paused,
                    { vm.change(paused = it, onFail = fail) },
                    supporting = if (s.paused) t("settings.pausedSub") else t("settings.pauseSub"),
                    emphasis = true,
                )
            }
            SubNote(t("settings.theseChoicesFollowYouTo"))
            s.sections.forEach { sec ->
                val title = SECTION_LABELS[sec.key] ?: sec.key
                Card {
                    val count = if (sec.enabled) sectionCount(sec.events.count { it.enabled }, sec.events.size) else null
                    // web .dl-set-section-head: 56 dp, 16/700, 12 dp gap.
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                title,
                                fontSize = 16.sp,
                                lineHeight = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (s.paused) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            )
                            count?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        }
                        DlSwitch(sec.enabled, { vm.change(values = mapOf("section.${sec.key}" to it), onFail = fail) }, enabled = !s.paused)
                    }
                    sec.events.forEach { e ->
                        val (label, hint) = eventLabel(e.key, friend)
                        EventRow(label, hint.ifEmpty { null }, e.enabled, !s.paused && sec.enabled) { vm.change(values = mapOf(e.key to it), onFail = fail) }
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
        NotifyPermission.Granted -> t("settings.device.on") to t("settings.device.onSubPhone")
        NotifyPermission.Denied -> t("settings.device.blocked") to t("settings.device.blockedSubAndroid")
        NotifyPermission.Default -> t("settings.device.off") to t("settings.device.offSubPhone")
    }
    val on = access.permission == NotifyPermission.Granted
    // web .dl-set-device: 14/16, 12 dp gap, a teal dot when on and a red one when off.
    Card {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(10.dp).background(if (on) DlTheme.colors.userB else MaterialTheme.colorScheme.error, CircleShape))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(t("settings.thisPhone", "state" to state), fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
                Text(sub, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!on) DlFilledButton(t("settings.allow"), access.allow, height = 40.dp)
        }
    }
}

/** web .dl-set-event: 56 dp, 6/16/6/20, label 15/500; off rows read quiet. */
@Composable
private fun EventRow(label: String, hint: String?, checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val ink = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium, color = ink)
            hint?.let { Text(it, fontSize = 13.sp, lineHeight = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        DlSwitch(checked, null, enabled = enabled)
    }
}

/** web .dl-sub-note: 13/19 quiet, 4 dp in. */
@Composable
internal fun SubNote(text: String) {
    Text(
        text,
        Modifier.padding(horizontal = 4.dp),
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A native-looking radio in the seal (web input[type=radio] accent-color: seal), 20 dp. */
@Composable
internal fun Radio(selected: Boolean) {
    val seal = MaterialTheme.colorScheme.primary
    val off = MaterialTheme.colorScheme.onSurfaceVariant
    val fill = MaterialTheme.colorScheme.surface
    Canvas(Modifier.size(20.dp)) {
        val r = size.minDimension / 2
        drawCircle(fill, r)
        if (selected) {
            drawCircle(seal, r - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
            drawCircle(seal, 5.dp.toPx())
        } else {
            drawCircle(off, r - 0.75.dp.toPx(), style = Stroke(1.5.dp.toPx()))
        }
    }
}
