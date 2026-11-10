package com.delulutopia.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.app.R
import com.delulutopia.core.designsystem.component.DlSectionLabel
import com.delulutopia.core.designsystem.theme.ThemeMode

/** Settings › Appearance (F-Appearance): Light / Dark / Match device, saved per device. */
@Composable
fun AppearanceScreen(themeMode: ThemeMode, onThemeModeChange: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DlSectionLabel(stringResource(R.string.appearance_theme), Modifier.padding(start = 4.dp, top = 8.dp))
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .selectableGroup(),
        ) {
            ThemeMode.entries.forEach { mode ->
                val label = when (mode) {
                    ThemeMode.Light -> R.string.appearance_light
                    ThemeMode.Dark -> R.string.appearance_dark
                    ThemeMode.System -> R.string.appearance_system
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(selected = mode == themeMode, role = Role.RadioButton, onClick = { onThemeModeChange(mode) })
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(label),
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    RadioButton(
                        selected = mode == themeMode,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.outline,
                        ),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.appearance_saved_note),
            modifier = Modifier.padding(start = 4.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
