package com.delulutopia.core.ui.media

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.ui.LocalBrand
import com.delulutopia.core.ui.LocalNoticeAction
import java.io.File
import kotlinx.coroutines.delay

/** Notes stop at five minutes (FS-W8). */
const val MAX_RECORDING_MS = 5 * 60 * 1000L

/** AAC in MP4, which the server re-encodes to m4a anyway (web media/recorder.ts). */
private class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var file: File? = null
    var startedAt = 0L
        private set

    fun start() {
        val out = File.createTempFile("voice", ".m4a", context.cacheDir)

        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(context) else MediaRecorder()
        r.setAudioSource(MediaRecorder.AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioSamplingRate(44_100)
        r.setAudioEncodingBitRate(96_000)
        r.setOutputFile(out.absolutePath)
        r.prepare()
        r.start()
        recorder = r
        file = out
        startedAt = SystemClock.elapsedRealtime()
    }

    /** The recording, or null when it was too short to hold anything. */
    fun stop(): ByteArray? {
        val r = recorder ?: return null
        recorder = null
        val ok = runCatching { r.stop() }.isSuccess
        r.release()
        val bytes = file?.takeIf { ok }?.readBytes()
        file?.delete()
        return bytes?.takeIf { it.isNotEmpty() }
    }

    fun cancel() {
        recorder?.let { r ->
            runCatching { r.stop() }
            r.release()
        }
        recorder = null
        file?.delete()
    }
}

/** What the strip shows of the recorder. */
class VoiceState(val recording: Boolean, val elapsedMs: Long, val start: () -> Unit, val stop: () -> Unit)

/**
 * Recording with the microphone ask (FS-W7–W9; web media/useVoiceRecording.tsx; canvas X-Mic-ask,
 * X-Mic-blocked): the first tap on this phone shows the sheet, Allow asks the system, later taps
 * record at once. A refusal shows "Microphone is off" with a way to Settings; Attach audio file keeps
 * working. [onRecorded] gets the bytes and the length.
 */
@Composable
fun rememberVoiceRecording(onRecorded: (ByteArray, Long) -> Unit): VoiceState {
    val context = LocalContext.current
    val notice = LocalNoticeAction.current
    val brand = LocalBrand.current
    val done by rememberUpdatedState(onRecorded)
    val prefs = remember { context.getSharedPreferences("device", Context.MODE_PRIVATE) }
    val recorder = remember { VoiceRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var asking by remember { mutableStateOf(false) }
    var blocked by remember { mutableStateOf(false) }

    val finish: () -> Unit = {
        val length = (SystemClock.elapsedRealtime() - recorder.startedAt).coerceAtMost(MAX_RECORDING_MS)
        val bytes = recorder.stop()
        recording = false
        elapsed = 0
        if (bytes != null) done(bytes, length)
    }
    val begin: () -> Unit = {
        runCatching {
            OnePlayer.stop()
            recorder.start()
            recording = true
            elapsed = 0
        }.onFailure { blocked = true }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) begin() else blocked = true
    }
    LaunchedEffect(recording) {
        while (recording) {
            elapsed = SystemClock.elapsedRealtime() - recorder.startedAt
            if (elapsed >= MAX_RECORDING_MS) finish()
            delay(250)
        }
    }
    LaunchedEffect(blocked) {
        if (blocked) {
            notice("Microphone is off for $brand", "Settings") { openAppSettings(context) }
            blocked = false
        }
    }
    DisposableEffect(Unit) { onDispose { recorder.cancel() } }

    val granted = { ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED }
    val start: () -> Unit = {
        when {
            granted() -> begin()
            !prefs.getBoolean(ASKED, false) -> asking = true
            else -> permission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (asking) {
        DlBottomSheet(onDismissRequest = { asking = false }) {
            Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(56.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(WebIcons.Mic, null, Modifier.size(28.dp))
                }
                Text("Record a voice note", style = MaterialTheme.typography.headlineSmall)
                Text("Your phone asks once for the microphone.", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Notes stop at five minutes and play on any phone. If you say no, you can still attach an audio file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DlOutlinedButton("Not now", { asking = false }, Modifier.weight(1f))
                    DlFilledButton(
                        "Allow",
                        {
                            prefs.edit().putBoolean(ASKED, true).apply()
                            asking = false
                            permission.launch(Manifest.permission.RECORD_AUDIO)
                        },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
    return VoiceState(recording, elapsed, start, finish)
}

private const val ASKED = "microphoneAsked"

/** The app's page in the system settings (the way back after "Don't allow"). */
fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}
