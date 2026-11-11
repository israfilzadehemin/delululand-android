package com.delulutopia.feature.emails

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recording
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.camera.view.video.AudioConfig
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlFilledButton
import com.delulutopia.core.designsystem.component.DlOutlinedButton
import com.delulutopia.core.ui.media.MAX_VIDEO_MS
import com.delulutopia.core.ui.media.VideoPlayer
import com.delulutopia.core.ui.media.mmss
import java.io.File
import kotlinx.coroutines.delay

private const val CAMERA_OFF = "Camera is off · Allow it in Settings"

/**
 * FS-E16 (web media/VideoRecorderSheet.tsx; canvas E-Video, E-Video-review): the camera preview,
 * flip, ● start / ■ stop with "0:07 / 0:15", auto-stop at 15 s, then Use / Retake. The phone asks
 * for the camera and microphone when the sheet opens; a refusal shows the line and keeps "Pick a
 * video".
 */
@SuppressLint("MissingPermission")
@Composable
internal fun VideoRecorderSheet(onUse: (Uri, Long) -> Unit, onPick: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    fun granted(p: String) = ContextCompat.checkSelfPermission(context, p) == PackageManager.PERMISSION_GRANTED
    var allowed by remember { mutableStateOf(granted(Manifest.permission.CAMERA) && granted(Manifest.permission.RECORD_AUDIO)) }
    var denied by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        allowed = r.values.all { it }
        denied = !allowed
    }
    LaunchedEffect(Unit) { if (!allowed) ask.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }
    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.VIDEO_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
        }
    }
    DisposableEffect(allowed) {
        if (allowed) controller.bindToLifecycle(owner)
        onDispose { controller.unbind() }
    }
    var recording by remember { mutableStateOf<Recording?>(null) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    var take by remember { mutableStateOf<Pair<File, Long>?>(null) }
    LaunchedEffect(recording) {
        while (recording != null) {
            elapsed = System.currentTimeMillis() - startedAt
            if (elapsed >= MAX_VIDEO_MS) recording?.stop()
            delay(200)
        }
    }
    DisposableEffect(Unit) { onDispose { recording?.stop() } }

    val start = {
        val file = File.createTempFile("video", ".mp4", context.cacheDir)
        startedAt = System.currentTimeMillis()
        elapsed = 0
        recording = controller.startRecording(FileOutputOptions.Builder(file).build(), AudioConfig.create(true), ContextCompat.getMainExecutor(context)) { e ->
            if (e is VideoRecordEvent.Finalize) {
                val ms = (System.currentTimeMillis() - startedAt).coerceAtMost(MAX_VIDEO_MS)
                recording = null
                take = if (!e.hasError() || e.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED) file to ms else null
            }
        }
    }

    DlBottomSheet(onDismissRequest = onClose) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Record a video", Modifier.padding(horizontal = 8.dp), style = MaterialTheme.typography.headlineSmall)
            when {
                denied || !allowed -> {
                    Text(CAMERA_OFF, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    DlFilledButton("Pick a video", onPick)
                }
                take != null -> {
                    val (file, ms) = take!!
                    VideoPlayer(Uri.fromFile(file).toString(), null, ms, null, null)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DlOutlinedButton("Retake", {
                            file.delete()
                            take = null
                        }, Modifier.weight(1f))
                        DlFilledButton("Use", { onUse(Uri.fromFile(file), ms) }, Modifier.weight(1f))
                    }
                }
                else -> {
                    Box(Modifier.fillMaxWidth().aspectRatio(3f / 4f).clip(RoundedCornerShape(12.dp))) {
                        AndroidView(factory = {
                            PreviewView(it).apply { this.controller = controller }
                        }, modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f))
                    }
                    Text(
                        "${mmss(elapsed)} / ${mmss(MAX_VIDEO_MS)}",
                        Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DlOutlinedButton(
                            "Flip",
                            {
                                controller.cameraSelector =
                                    if (controller.cameraSelector ==
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    ) {
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    } else {
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    }
                            },
                            Modifier.weight(1f),
                            enabled = recording == null,
                        )
                        if (recording == null) {
                            DlFilledButton("● Start", start, Modifier.weight(1f))
                        } else {
                            DlFilledButton("■ Stop", { recording?.stop() }, Modifier.weight(1f))
                        }
                    }
                    DlOutlinedButton("Pick a video", onPick, Modifier.fillMaxWidth(), enabled = recording == null)
                }
            }
        }
    }
}
