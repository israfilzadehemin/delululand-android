package com.delulutopia.feature.you

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlGhostButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTimeZoneSheet
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.t
import com.delulutopia.core.ui.LocalAppNav
import com.delulutopia.core.ui.LocalNotice
import com.delulutopia.core.ui.ageWords
import com.delulutopia.core.ui.initials
import com.delulutopia.core.ui.moodText
import com.delulutopia.core.ui.photo.PhotoChooser
import com.delulutopia.core.ui.rememberNow
import com.delulutopia.core.ui.signatureOf
import com.delulutopia.core.ui.tintOf

/**
 * Profile (doc "Delululand — Menu, Profile & Settings" §3): the photo (choose or take, crop,
 * remove), username, name, surname and time zone. The text fields and the zone save together with
 * Save; the photo saves on its own as soon as it is cropped.
 */
@Composable
fun ProfileScreen(vm: ProfileViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val notice = LocalNotice.current
    val homeState by vm.homeState.collectAsStateWithLifecycle()
    val friend = homeState.home?.friend?.nickname ?: t("settings.yourFriend")
    val nowMs = rememberNow(30_000)
    var source by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var zonePicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        DlTopAppBar(title = t("profile.profile"), onBack = { nav.back() })
        val profile = vm.profile
        if (profile == null) {
            LoadOrFail(vm.failed)
            return@Column
        }
        val form = vm.form
        val errors = form.problems() + vm.serverError
        val canSave = form != vm.saved && form.problems().isEmpty() && !vm.busy
        val zone = Zones.option(form.timezone, nowMs)
        // web .dl-sub-main.dl-profile: 4/20/24 padding, 14 dp between fields.
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // web .dl-profile-photo: the 96 dp picture centred, its two buttons under it.
            Column(
                Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    DlRemoteAvatar(
                        profile.avatarUrl,
                        t("universe.yourPhoto"),
                        tintOf(profile.id),
                        Modifier.alpha(if (vm.photoBusy) 0.5f else 1f),
                        size = 96.dp,
                        initials = form.initials(),
                        ring = signatureOf(profile.id),
                    )
                    if (vm.photoBusy) CircularProgressIndicator(Modifier.size(32.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    DlGhostButton(t("profile.changePhoto"), { source = true }, icon = WebIcons.Camera, enabled = !vm.photoBusy)
                    if (profile.avatarUrl != null) {
                        DlTextButton(t("profile.remove"), { confirmRemove = true }, destructive = true, enabled = !vm.photoBusy, icon = WebIcons.Trash)
                    }
                }
            }
            DlTextField(
                value = form.username,
                onValueChange = { vm.set("username", it.filterNot(Char::isWhitespace)) },
                label = t("profile.username"),
                supportingText = t("profile.316LettersOrDigits"),
                errorText = errors["username"],
                maxLength = 32,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            )
            DlTextField(value = form.firstName, onValueChange = {
                vm.set("firstName", it)
            }, label = t("settings.name"), errorText = errors["firstName"], maxLength = 60)
            DlTextField(value = form.lastName, onValueChange = {
                vm.set("lastName", it)
            }, label = t("profile.surname"), errorText = errors["lastName"], maxLength = 60)
            // web .dl-field with .dl-field-picker: label above, a 48 dp box; the time in the seal.
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    t("profile.timeZone").uppercase(I18n.lang.locale),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.06.em,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val shape = RoundedCornerShape(8.dp)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(shape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        .clickable(role = Role.Button) { zonePicker = true }
                        .padding(start = 14.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        buildAnnotatedString {
                            append(zone.city)
                            withStyle(SpanStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(" · ${form.timezone}") }
                        },
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        zone.time,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, fontFeatureSettings = "tnum"),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                errors["timezone"]?.let { Text(it, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.error) }
            }
            Text(
                t("profile.yourUsernameIsWhatSees", "friend" to friend),
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 19.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        // web .dl-sub-foot: 8/20/12 around a full-width Save.
        DlFilledButton(
            if (vm.busy) t("common.saving") else t("common.save"),
            { vm.save { it?.let(notice) } },
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
            enabled = canSave,
        )
        if (confirmRemove) {
            DlDialog(
                title = t("profile.removeYourPhoto"),
                text = t("profile.yourInitialsShowInsteadFor", "friend" to friend),
                confirmLabel = t("profile.remove"),
                onConfirm = {
                    confirmRemove = false
                    vm.remove(notice)
                },
                dismissLabel = t("profile.keepIt"),
                onDismiss = { confirmRemove = false },
                destructive = true,
            )
        }
        if (zonePicker) {
            DlTimeZoneSheet(value = form.timezone, onPick = {
                vm.set("timezone", it)
                zonePicker = false
            }, onDismiss = { zonePicker = false })
        }
    }
    PhotoChooser(
        sheet = source,
        onDismissSheet = { source = false },
        onPhoto = { vm.upload(it, notice) },
        onTooBig = { notice(t("profile.photoTooLarge")) },
    )
}

/** The friend's profile, read-only (Menu Q5, P7): picture, username, name, local time and mood. */
@Composable
fun FriendScreen(vm: YouViewModel = hiltViewModel()) {
    val nav = LocalAppNav.current
    val homeState by vm.home.collectAsStateWithLifecycle()
    val friend = homeState.home?.friend
    val nowMs = rememberNow(30_000)
    Column(Modifier.fillMaxSize()) {
        DlTopAppBar(title = friend?.nickname.orEmpty(), onBack = { nav.back() })
        if (friend == null) return@Column
        val fullName = listOfNotNull(friend.firstName, friend.lastName).filter { it.isNotBlank() }.joinToString(" ")
        val mood = friend.moodText()
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DlRemoteAvatar(friend.avatarUrl, friend.nickname, tintOf(friend.id), size = 96.dp, initials = friend.initials(), ring = signatureOf(friend.id))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(friend.nickname, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    fullName.ifEmpty {
                        t("profile.noNameAdded")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                friend.timezone?.let { tz ->
                    Fact(t("profile.localTime"), t("profile.moodSince", "mood" to Zones.option(tz, nowMs).time, "age" to Zones.cityOf(tz)))
                }
                Fact(
                    t("profile.mood"),
                    if (mood !=
                        null
                    ) {
                        t("profile.moodSince", "mood" to mood, "age" to ageWords(friend.moodSetAt, nowMs))
                    } else {
                        t("profile.noMoodYet")
                    },
                )
            }
        }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    Column {
        DlSectionLabel(label)
        Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum"))
    }
}
