package com.delulutopia.feature.start

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.designsystem.theme.DlTheme
import com.delulutopia.core.designsystem.theme.Literata
import com.delulutopia.core.i18n.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * C5 (U-Key): the key, shown once, with Copy and Share (the Android share sheet). Continue opens after
 * either, or after three seconds, so nobody leaves without at least seeing it.
 */
@Composable
fun KeyReveal(name: String, code: String, continueLabel: String? = null, onContinue: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var copied by remember { mutableStateOf(false) }
    var ready by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(3_000)
        ready = true
    }
    val grouped = groupKey(code)
    val shareText = t("universe.shareText", "name" to name, "key" to grouped)

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t("universe.thisIsSKey", "name" to name), fontFamily = Literata, style = MaterialTheme.typography.headlineSmall)
        Text(
            grouped,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(12.dp))
                .padding(vertical = 20.dp)
                .semantics { contentDescription = t("universe.key", "join" to code.toList().joinToString(" ")) },
            style = DlTheme.type.key.copy(fontSize = DlTheme.type.key.fontSize * 1.3f),
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DlOutlinedButton(
                t(if (copied) "universe.copied" else "universe.copy"),
                onClick = {
                    scope.launch { clipboard.setClipEntry(ClipData.newPlainText(name, grouped).toClipEntry()) }
                    copied = true
                    ready = true
                },
                icon = if (copied) DlIcons.Check else DlIcons.Copy,
            )
            DlOutlinedButton(
                t("universe.share"),
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                    context.startActivity(Intent.createChooser(send, null))
                    ready = true
                },
                icon = DlIcons.Share,
            )
        }
        Text(t("universe.sendItToTheOne"), style = MaterialTheme.typography.bodyMedium)
        Text(
            t("universe.itsShownOnlyNowLost"),
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(12.dp))
                .padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        DlFilledButton(continueLabel ?: t("universe.continue"), onContinue, Modifier.fillMaxWidth(), enabled = ready, height = 48.dp)
    }
}
