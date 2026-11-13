package com.delulutopia.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang

/** The button's name for every reader before their language is known: "Language" in all five (web LangSwitch). */
private const val LABEL = "Language · Dil · زبان · Язык"

/**
 * L6, L20 (web i18n/LangSwitch): one quiet button under the key form and on the cards screen, a globe
 * and the language in force in its own words, opening the list of all five, each in its own words.
 * A pick applies at once, is remembered on this phone and goes with create or join.
 */
@Composable
internal fun LangSwitch(modifier: Modifier = Modifier) {
    val current = I18n.lang
    var open by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box {
            TextButton(
                onClick = { open = true },
                modifier = Modifier.semantics { contentDescription = "$LABEL: ${current.native}" },
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(DlIcons.Globe, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(current.native, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Icon(WebIcons.ChevronDown, contentDescription = null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                Lang.entries.forEach { lang ->
                    val on = lang == current
                    DropdownMenuItem(
                        text = { Text(lang.native, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal) },
                        trailingIcon = if (on) {
                            { Icon(DlIcons.Check, contentDescription = null, Modifier.size(16.dp)) }
                        } else {
                            null
                        },
                        onClick = {
                            I18n.set(lang)
                            open = false
                        },
                        modifier = Modifier.semantics {
                            selected = on
                            role = Role.RadioButton
                        },
                    )
                }
            }
        }
    }
}
