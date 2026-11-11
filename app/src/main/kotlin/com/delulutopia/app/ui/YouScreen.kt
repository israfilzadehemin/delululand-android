package com.delulutopia.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.delulutopia.app.R
import com.delulutopia.core.designsystem.component.DlDialog
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlRemoteAvatar
import com.delulutopia.core.designsystem.icon.DlIcons
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.ui.signatureOf

/**
 * The You destination (Y-A3). P1 carries the profile card, Appearance and Log out; Profile, Universe,
 * Notifications and Change PIN arrive in P6.
 */
@Composable
fun YouScreen(session: HomeSession, onOpenAppearance: () -> Unit, onLogout: () -> Unit, modifier: Modifier = Modifier) {
    var confirmLogout by remember { mutableStateOf(false) }
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DlRemoteAvatar(
                avatarUrl = session.me.avatarUrl,
                name = session.me.nickname,
                initials = session.me.nickname.take(1).uppercase(),
                signature = signatureOf(session.me.id),
                size = 56.dp,
            )
            Column {
                Text(session.me.nickname, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.you_signed_in, session.universe.name),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(
            Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        ) {
            DlListItem(title = stringResource(R.string.you_appearance), icon = DlIcons.Palette, onClick = onOpenAppearance)
            DlListItem(title = stringResource(R.string.you_log_out), icon = DlIcons.LogOut, showChevron = false, onClick = { confirmLogout = true })
        }
    }
    if (confirmLogout) {
        DlDialog(
            title = stringResource(R.string.logout_title, session.universe.name),
            text = stringResource(R.string.logout_text),
            confirmLabel = stringResource(R.string.you_log_out),
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            dismissLabel = stringResource(R.string.cancel),
            onDismiss = { confirmLogout = false },
            destructive = true,
        )
    }
}
