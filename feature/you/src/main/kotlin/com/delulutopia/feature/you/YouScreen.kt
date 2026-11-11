package com.delulutopia.feature.you

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.component.DlSwitch
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.network.model.NotifySettings
import com.delulutopia.core.ui.AppearanceRoute
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.NotificationsRoute
import com.delulutopia.core.ui.ProfileRoute
import com.delulutopia.core.ui.UniverseRoute
import com.delulutopia.core.ui.fingerprint.Fingerprint
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.notify.NotifyPermission
import com.delulutopia.core.ui.notify.rememberNotifyAccess
import com.delulutopia.core.ui.signatureOf

val THEME_WORDS = mapOf(ThemeMode.Light to "Light", ThemeMode.Dark to "Dark", ThemeMode.System to "Match device")

/** The universe mark (web IconUniverse): a planet and its ring. */
val UniverseIcon: ImageVector by lazy {
    ImageVector.Builder("universe", 24.dp, 24.dp, 24f, 24f).apply {
        path(stroke = SolidColor(androidx.compose.ui.graphics.Color.Black), strokeLineWidth = 1.75f, strokeLineCap = StrokeCap.Round) {
            moveTo(17f, 12f)
            arcTo(5f, 5f, 0f, true, true, 7f, 12f)
            arcTo(5f, 5f, 0f, true, true, 17f, 12f)
            close()
        }
        group(rotate = -18f, pivotX = 12f, pivotY = 12f) {
            path(stroke = SolidColor(androidx.compose.ui.graphics.Color.Black), strokeLineWidth = 1.75f, strokeLineCap = StrokeCap.Round) {
                moveTo(22f, 12f)
                arcTo(10f, 3.5f, 0f, true, true, 2f, 12f)
                arcTo(10f, 3.5f, 0f, true, true, 22f, 12f)
                close()
            }
        }
    }.build()
}

fun deviceWords(p: NotifyPermission): String = when (p) {
    NotifyPermission.Granted -> "On"
    NotifyPermission.Denied -> "Blocked"
    NotifyPermission.Default -> "Off"
}

fun notifySummary(s: NotifySettings?): String {
    if (s == null) return ""
    if (s.paused) return "Paused"
    val total = s.sections.sumOf { it.events.size }
    val on = s.sections.filter { it.enabled }.sumOf { sec -> sec.events.count { it.enabled } }
    return "$on of $total on"
}

/**
 * The You tab (doc "Delululand — You Tab & New Buttons" §4): the profile card, Settings (Universe,
 * Appearance, Notifications), Account (Change PIN, fingerprint unlock, About soon) and Log out.
 */
@Composable
fun YouScreen(themeMode: ThemeMode, onLogout: () -> Unit, vm: YouViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val brand = LocalBrand.current
    val notice = LocalNotice.current
    val context = LocalContext.current
    val activity = LocalActivity.current as? FragmentActivity
    val homeState by vm.home.collectAsStateWithLifecycle()
    val leaves by vm.leaves.collectAsStateWithLifecycle()
    val me = homeState.home?.me
    val device = rememberNotifyAccess()
    var changePin by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    var fingerprintPin by remember { mutableStateOf(false) }
    var fingerprintOn by remember(me?.id) { mutableStateOf(me != null && Fingerprint.isOn(context, me.id)) }
    LifecycleResumeEffect(Unit) {
        vm.load()
        onPauseOrDispose { }
    }
    val summary = notifySummary(vm.notify)
    val wanted = homeState.home?.notifyPrefs?.let { it.enabled.isNotEmpty() } ?: true
    val attention = wanted && device.permission != NotifyPermission.Granted

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (me != null) {
            val fullName = listOfNotNull(me.firstName, me.lastName).filter { it.isNotBlank() }.joinToString(" ")
            val place = me.timezone?.let { Zones.cityOf(it) }.orEmpty()
            val details = listOf(fullName, place).filter { it.isNotEmpty() }.joinToString(" · ")
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { nav.open(ProfileRoute) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                DlRemoteAvatar(me.avatarUrl, me.nickname, signatureOf(me.id), size = 64.dp, initials = me.initials())
                Column(Modifier.weight(1f)) {
                    Text(me.nickname, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    if (details.isNotEmpty()) Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        "View profile",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DlSectionLabel("Settings", Modifier.padding(start = 4.dp, top = 8.dp))
        Card {
            DlListItem("Universe", supporting = "$brand · name, welcome, key", icon = UniverseIcon, onClick = { nav.open(UniverseRoute) })
            DlListItem(
                "Appearance",
                supporting = "${THEME_WORDS[themeMode]} · falling leaves ${if (leaves) "on" else "off"}",
                icon = DlIcons.Palette,
                onClick = { nav.open(AppearanceRoute) },
            )
            DlListItem(
                "Notifications",
                supporting = listOf(summary, "this phone: ${deviceWords(device.permission)}").filter { it.isNotEmpty() }.joinToString(" · "),
                icon = DlIcons.Bell,
                onClick = { nav.open(NotificationsRoute) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (attention) Box(Modifier.size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                        Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        }
        DlSectionLabel("Account", Modifier.padding(start = 4.dp, top = 8.dp))
        Card {
            DlListItem("Change PIN", icon = WebIcons.Key, showChevron = false, onClick = { changePin = true })
            if (me != null && activity != null && Fingerprint.available(context)) {
                DlListItem(
                    "Fingerprint unlock",
                    supporting = "Instead of typing your PIN on this phone",
                    icon = DlIcons.Fingerprint,
                    onClick = {
                        if (fingerprintOn) {
                            Fingerprint.turnOff(context, me.id)
                            fingerprintOn = false
                        } else {
                            fingerprintPin = true
                        }
                    },
                    trailing = { DlSwitch(fingerprintOn, null) },
                )
            }
            DlListItem("About", icon = WebIcons.Info, enabled = false, showChevron = false, trailing = { SoonChip() })
        }
        Card {
            DlListItem("Log out", icon = DlIcons.LogOut, showChevron = false, onClick = { confirmLogout = true })
        }
    }

    if (changePin) {
        ChangePinSheet(onClose = { changePin = false }, onChanged = { pin ->
            // A changed PIN replaces the one kept behind the fingerprint.
            if (me != null && activity != null && fingerprintOn) {
                Fingerprint.turnOff(context, me.id)
                Fingerprint.turnOn(activity, me.id, pin, "Keep fingerprint unlock") { fingerprintOn = it }
            }
        })
    }
    if (fingerprintPin && me != null && activity != null) {
        FingerprintPinSheet(
            userId = me.id,
            onClose = { fingerprintPin = false },
            onPin = { pin ->
                fingerprintPin = false
                Fingerprint.turnOn(activity, me.id, pin, "Turn on fingerprint unlock") { ok ->
                    fingerprintOn = ok
                    if (ok) notice("Fingerprint unlock is on")
                }
            },
        )
    }
    if (confirmLogout) {
        DlDialog(
            title = "Log out of $brand?",
            text = "Your letters and drafts stay safe. You'll need your PIN to come back on this device.",
            confirmLabel = "Log out",
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            dismissLabel = "Cancel",
            onDismiss = { confirmLogout = false },
            destructive = true,
        )
    }
}

@Composable
internal fun Card(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh), content = content)
}

/** Menu Q12: shown, greyed, not tappable, with a "Soon" chip. */
@Composable
private fun SoonChip() {
    Text(
        "Soon",
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LoadOrFail(failed: Boolean) {
    val brand = LocalBrand.current
    Text(
        if (failed) "Can't reach $brand. Try again." else "Loading…",
        Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LoadEffect(key: Any?, block: () -> Unit) = LaunchedEffect(key) { block() }
