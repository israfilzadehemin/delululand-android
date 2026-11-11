package com.delulutopia.feature.you

import androidx.compose.foundation.background
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTimeZoneSheet
import com.delulutopia.core.designsystem.component.DlTopAppBar
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlShape
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
    val friend = homeState.home?.friend?.nickname ?: "your friend"
    val nowMs = rememberNow(30_000)
    var source by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }
    var zonePicker by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().imePadding()) {
        DlTopAppBar(title = "Profile", onBack = { nav.back() })
        val profile = vm.profile
        if (profile == null) {
            LoadOrFail(vm.failed)
            return@Column
        }
        val form = vm.form
        val errors = form.problems() + vm.serverError
        val canSave = form != vm.saved && form.problems().isEmpty() && !vm.busy
        val zone = Zones.option(form.timezone, nowMs)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    DlRemoteAvatar(
                        profile.avatarUrl,
                        "Your photo",
                        tintOf(profile.id),
                        Modifier.alpha(if (vm.photoBusy) 0.5f else 1f),
                        size = 96.dp,
                        initials = form.initials(),
                        ring = signatureOf(profile.id),
                    )
                    if (vm.photoBusy) CircularProgressIndicator(Modifier.size(32.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    DlOutlinedButton("Change photo", { source = true }, icon = WebIcons.Camera, enabled = !vm.photoBusy)
                    if (profile.avatarUrl != null) {
                        DlTextButton("Remove", { confirmRemove = true }, destructive = true, enabled = !vm.photoBusy)
                    }
                }
            }
            DlTextField(
                value = form.username,
                onValueChange = { vm.set("username", it.filterNot(Char::isWhitespace)) },
                label = "Username",
                supportingText = "3–16 letters or digits, no spaces",
                errorText = errors["username"],
                maxLength = 32,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
            )
            DlTextField(value = form.firstName, onValueChange = { vm.set("firstName", it) }, label = "Name", errorText = errors["firstName"], maxLength = 60)
            DlTextField(value = form.lastName, onValueChange = { vm.set("lastName", it) }, label = "Surname", errorText = errors["lastName"], maxLength = 60)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh, DlShape.textField)
                        .clickable(role = Role.Button) { zonePicker = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("TIME ZONE", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${zone.city} · ${form.timezone}", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), maxLines = 1)
                    }
                    Text(zone.time, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
                    Icon(DlIcons.ChevronRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                errors["timezone"]?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
            Text(
                "Your username is what $friend sees everywhere. Name and surname show only on your profiles. Your time zone sets your clock on Home.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DlFilledButton(
            if (vm.busy) "Saving…" else "Save",
            { vm.save { it?.let(notice) } },
            Modifier.fillMaxWidth().padding(16.dp),
            enabled = canSave,
        )
        if (confirmRemove) {
            DlDialog(
                title = "Remove your photo?",
                text = "Your initials show instead, for you and for $friend.",
                confirmLabel = "Remove",
                onConfirm = {
                    confirmRemove = false
                    vm.remove(notice)
                },
                dismissLabel = "Keep it",
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
        onTooBig = { notice("This photo is larger than 10 MB") },
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
                Text(fullName.ifEmpty { "No name added" }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(
                Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                friend.timezone?.let { tz ->
                    Fact("Local time", "${Zones.option(tz, nowMs).time} · ${Zones.cityOf(tz)}")
                }
                Fact("Mood", if (mood != null) "$mood · ${ageWords(friend.moodSetAt, nowMs)}" else "No mood yet")
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
