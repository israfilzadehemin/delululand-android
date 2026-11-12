package com.delulutopia.feature.you

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlGroupCard
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSwitch
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.motion.pressScale
import com.delulutopia.core.designsystem.theme.ThemeMode
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.NotifySettings
import com.delulutopia.core.ui.AppearanceRoute
import com.delulutopia.core.ui.LanguageRoute
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
import com.delulutopia.core.ui.tintOf

val THEME_WORDS get() = mapOf(
    ThemeMode.Light to t("settings.theme.light"),
    ThemeMode.Dark to t("settings.theme.dark"),
    ThemeMode.System to t("settings.theme.match"),
)

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
    NotifyPermission.Granted -> t("settings.device.on")
    NotifyPermission.Denied -> t("settings.device.blocked")
    NotifyPermission.Default -> t("settings.device.off")
}

fun notifySummary(s: NotifySettings?): String {
    if (s == null) return ""
    if (s.paused) return t("settings.paused")
    val total = s.sections.sumOf { it.events.size }
    val on = s.sections.filter { it.enabled }.sumOf { sec -> sec.events.count { it.enabled } }
    return t("settings.someOn", "on" to on, "total" to total)
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

    // web .dl-you .dl-sub-main: 4/20/24 padding, 16 dp between blocks.
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (me != null) {
            val fullName = listOfNotNull(me.firstName, me.lastName).filter { it.isNotBlank() }.joinToString(" ")
            val place = me.timezone?.let { Zones.cityOf(it) }.orEmpty()
            val details = listOf(fullName, place).filter { it.isNotEmpty() }.joinToString(" · ")
            // web .dl-you-card: 14 dp padding, 6 dp corners, a line and a soft shadow.
            val shape = RoundedCornerShape(6.dp)
            val press = remember { MutableInteractionSource() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressScale(press, 0.98f)
                    .shadow(2.dp, shape)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                    .clickable(interactionSource = press, indication = ripple()) { nav.open(ProfileRoute) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                DlRemoteAvatar(me.avatarUrl, me.nickname, tintOf(me.id), size = 64.dp, initials = me.initials(), ring = signatureOf(me.id), ringWidth = 3.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(me.nickname, fontSize = 18.sp, lineHeight = 23.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (details.isNotEmpty()) {
                        Text(
                            details,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        t("settings.viewProfile"),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Group(t("settings.settings")) {
            DlListItem(t("settings.universe"), supporting = t("settings.nameWelcomeKey", "brand" to brand), icon = UniverseIcon, onClick = {
                nav.open(UniverseRoute)
            })
            DlListItem(
                t("settings.appearance"),
                supporting = t(if (leaves) "settings.appearanceSubOn" else "settings.appearanceSubOff", "theme" to THEME_WORDS[themeMode]),
                icon = DlIcons.Palette,
                onClick = { nav.open(AppearanceRoute) },
            )
            DlListItem(
                t("settings.language"),
                // Each language in its own words (L8), so it can be found from any language.
                supporting = I18n.lang.native,
                icon = DlIcons.Language,
                onClick = { nav.open(LanguageRoute) },
            )
            DlListItem(
                t("settings.notifications"),
                supporting = listOf(summary, t("settings.thisPhoneState", "device" to deviceWords(device.permission))).filter {
                    it.isNotEmpty()
                }.joinToString(" · "),
                icon = DlIcons.Bell,
                onClick = { nav.open(NotificationsRoute) },
                trailing = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (attention) Box(Modifier.size(9.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                        Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
            )
        }
        Group(t("settings.account")) {
            DlListItem(t("settings.changePin"), icon = WebIcons.Key, showChevron = false, onClick = { changePin = true })
            if (me != null && activity != null && Fingerprint.available(context)) {
                DlListItem(
                    t("settings.fingerprint"),
                    supporting = t("settings.fingerprintSub"),
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
            DlListItem(t("settings.about"), icon = WebIcons.Info, enabled = false, showChevron = false, trailing = { SoonChip() })
        }
        Card {
            DlListItem(t("settings.logOut"), icon = DlIcons.LogOut, showChevron = false, destructive = true, onClick = { confirmLogout = true })
        }
    }

    if (changePin) {
        ChangePinSheet(onClose = { changePin = false }, onChanged = { pin ->
            // A changed PIN replaces the one kept behind the fingerprint.
            if (me != null && activity != null && fingerprintOn) {
                Fingerprint.turnOff(context, me.id)
                Fingerprint.turnOn(activity, me.id, pin, t("settings.fingerprintKeep")) { fingerprintOn = it }
            }
        })
    }
    if (fingerprintPin && me != null && activity != null) {
        FingerprintPinSheet(
            userId = me.id,
            onClose = { fingerprintPin = false },
            onPin = { pin ->
                fingerprintPin = false
                Fingerprint.turnOn(activity, me.id, pin, t("settings.fingerprintTurnOn")) { ok ->
                    fingerprintOn = ok
                    if (ok) notice(t("settings.fingerprintOn"))
                }
            },
        )
    }
    if (confirmLogout) {
        DlDialog(
            title = t("app.logOutOf", "brand" to brand),
            text = t("app.yourLettersAndDraftsStay"),
            confirmLabel = t("settings.logOut"),
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            dismissLabel = t("settings.cancel"),
            onDismiss = { confirmLogout = false },
            destructive = true,
        )
    }
}

@Composable
internal fun Card(background: Color = MaterialTheme.colorScheme.surfaceContainerHigh, content: @Composable () -> Unit) {
    DlGroupCard(background = background, content = content)
}

/** A titled group (web section > h2.dl-set-group + .dl-set-card). */
@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column {
        // web .dl-set-group: 11/700, 0.08 em, 4 dp in, 6 dp above the card.
        Text(
            title.uppercase(),
            Modifier.padding(start = 4.dp, end = 4.dp, bottom = 6.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Card(content = content)
    }
}

/** Menu Q12: shown, greyed, not tappable, with a "Soon" chip (web .dl-set-soon-chip). */
@Composable
private fun SoonChip() {
    Text(
        "SOON",
        Modifier
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LoadOrFail(failed: Boolean) {
    val brand = LocalBrand.current
    Text(
        if (failed) t("common.cantReachTryAgain", "brand" to brand) else t("common.loading"),
        Modifier.padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun LoadEffect(key: Any?, block: () -> Unit) = LaunchedEffect(key) { block() }
