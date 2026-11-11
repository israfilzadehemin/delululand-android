package com.delulutopia.core.ui.notify

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/** Where notifications stand on this device (web notify/push.ts `permission`). */
enum class NotifyPermission { Granted, Default, Denied }

private fun prefs(context: Context) = context.getSharedPreferences("device", Context.MODE_PRIVATE)

private const val ASKED = "notifyAsked"

fun notifyPermissionOf(context: Context, canAskAgain: Boolean): NotifyPermission {
    if (NotificationManagerCompat.from(context).areNotificationsEnabled()) return NotifyPermission.Granted
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return NotifyPermission.Denied
    return if (!prefs(context).getBoolean(ASKED, false) || canAskAgain) NotifyPermission.Default else NotifyPermission.Denied
}

/** The current permission, re-read whenever the app comes back to the front (the user may have changed it in Settings). */
class NotifyAccess(val permission: NotifyPermission, val allow: () -> Unit)

@Composable
fun rememberNotifyAccess(): NotifyAccess {
    val context = LocalContext.current
    val activity = LocalActivity.current
    fun read(): NotifyPermission {
        val again = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) == true
        return notifyPermissionOf(context, again)
    }
    var permission by remember { mutableStateOf(read()) }
    LifecycleResumeEffect(Unit) {
        permission = read()
        onPauseOrDispose { }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        prefs(context).edit().putBoolean(ASKED, true).apply()
        permission = read()
    }
    val allow = {
        if (permission == NotifyPermission.Default && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Blocked: the system's own page for this app is the only way back (web UnblockSheet).
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
    return NotifyAccess(permission, allow)
}

/**
 * FS-A15 under the friend card (web notify/NotificationHint.tsx): "Notifications are off · Turn on"
 * (asks), or the blocked text with Allow, which opens the app's notification settings.
 */
@Composable
fun NotificationHint(modifier: Modifier = Modifier) {
    val access = rememberNotifyAccess()
    if (access.permission == NotifyPermission.Granted) return
    val blocked = access.permission == NotifyPermission.Denied
    Row(modifier.fillMaxWidth().padding(horizontal = 2.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (blocked) "Notifications are blocked in your settings" else "Notifications are off",
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(onClick = access.allow) {
            Text(if (blocked) "Allow" else "Turn on", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp))
        }
    }
}
