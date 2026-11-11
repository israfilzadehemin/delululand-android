package com.delulutopia.feature.start

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.component.DlTextField
import com.delulutopia.core.designsystem.component.DlTimeZoneSheet
import com.delulutopia.core.designsystem.component.Zones
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.DlShape

/** C3 and J1: what a citizen needs to exist. The photo comes in Profile (P6), with its crop screen. */
data class Citizen(val username: String = "", val timezone: String = Zones.device())

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
internal fun CitizenStep(value: Citizen, onChange: (Citizen) -> Unit, taken: List<String>, serverError: String?) {
    var picker by remember { mutableStateOf(false) }
    val zone = remember(value.timezone) { Zones.option(value.timezone) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
