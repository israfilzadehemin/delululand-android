package com.delulutopia.feature.start

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.designsystem.component.DlAvatar
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTimeZoneSheet
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.designsystem.theme.DlShape
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.ui.AfterStart
import com.delulutopia.core.ui.photo.PhotoChooser

/** C3 and J1: what a citizen needs to exist, and an optional photo (U9), uploaded once they are in. */
data class Citizen(val username: String = "", val timezone: String = Zones.device(), val photo: ByteArray? = null) {
    override fun equals(other: Any?) = other is Citizen && other.username == username && other.timezone == timezone && other.photo.contentEquals(photo)

    override fun hashCode() = username.hashCode() * 31 + timezone.hashCode()
}

/** "3–16 letters or digits, no spaces" / "That username is taken", or null when fine. */
@Composable
internal fun usernameProblem(username: String, taken: List<String>, serverError: String?): String? = when {
    serverError != null -> serverError
    username.isEmpty() -> null
    !Rules.usernameOk(username) -> stringResource(R.string.username_rule)
    Rules.usernameTaken(username, taken) -> stringResource(R.string.username_taken)
    else -> null
}

@Composable
internal fun CitizenStep(value: Citizen, onChange: (Citizen) -> Unit, taken: List<String>, serverError: String?, signature: Int = 0) {
    var picker by remember { mutableStateOf(false) }
    var source by remember { mutableStateOf(false) }
    var tooBig by remember { mutableStateOf(false) }
    val zone = remember(value.timezone) { Zones.option(value.timezone) }
    val preview = remember(value.photo) { value.photo?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DlAvatar(
                initials = value.username.take(1).uppercase().ifEmpty { "?" },
                signature = signatureTintOf(signature),
                ring = signatureOf(signature),
                size = 96.dp,
                initialsColor = MaterialTheme.colorScheme.onSurface,
                content = preview?.let { p -> { Image(p, stringResource(R.string.your_photo), Modifier.fillMaxSize(), contentScale = ContentScale.Crop) } },
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                DlOutlinedButton(
                    stringResource(if (value.photo != null) R.string.change_photo else R.string.add_photo),
                    { source = true },
                    icon = WebIcons.Camera,
                )
                Text(
                    stringResource(if (value.photo != null) R.string.photo_after else R.string.photo_optional),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (tooBig) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (tooBig) Text(stringResource(R.string.photo_too_big), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        }
        DlTextField(
            value = value.username,
            onValueChange = { onChange(value.copy(username = it.filterNot(Char::isWhitespace))) },
            label = stringResource(R.string.username_label),
            supportingText = stringResource(R.string.username_rule),
            errorText = usernameProblem(value.username, taken, serverError),
            maxLength = 32,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, DlShape.textField)
                    .clickable(role = Role.Button) { picker = true }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.timezone_label),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "${zone.city} · ${value.timezone}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        maxLines = 1,
                    )
                }
                Text(zone.time, style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"))
                Icon(DlIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Text(
                stringResource(R.string.timezone_hint),
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(stringResource(R.string.citizen_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    PhotoChooser(
        sheet = source,
        onDismissSheet = { source = false },
        onPhoto = {
            tooBig = false
            onChange(value.copy(photo = it))
        },
        onTooBig = { tooBig = true },
        onRemove = if (value.photo != null) ({ onChange(value.copy(photo = null)) }) else null,
    )
    if (picker) {
        DlTimeZoneSheet(
            value = value.timezone,
            onPick = {
                onChange(value.copy(timezone = it))
                picker = false
            },
            onDismiss = { picker = false },
        )
    }
}

/** U9: the photo goes up once the citizen exists; a failure is said on Home (Profile can add it). */
internal suspend fun AccountRepository.uploadPhoto(photo: ByteArray?) {
    if (photo == null) return
    try {
        uploadAvatar(photo)
    } catch (_: ApiException) {
        AfterStart.photoFailed = true
    }
}
