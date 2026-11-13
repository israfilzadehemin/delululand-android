package com.delulutopia.feature.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang
import com.delulutopia.core.i18n.t

/**
 * L6 (web i18n/LangSwitch): "English · Azərbaycanca · فارسی" under the key form and on the cards screen.
 * A pick applies at once, is remembered on this phone and goes with create or join.
 */
@Composable
internal fun LangSwitch(modifier: Modifier = Modifier) {
    val current = I18n.lang
    Row(
        modifier.fillMaxWidth().semantics { contentDescription = t("settings.language") },
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Lang.entries.forEachIndexed { i, lang ->
            if (i > 0) Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val on = lang == current
            Text(
                lang.native,
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .selectable(selected = on, role = Role.RadioButton) { I18n.set(lang) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                fontSize = 13.sp,
                fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                color = if (on) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
