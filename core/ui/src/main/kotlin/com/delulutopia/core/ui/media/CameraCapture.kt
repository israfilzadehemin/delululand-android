package com.delulutopia.core.ui.media

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.delulutopia.core.network.VideoLimits
import java.io.File

/**
 * Camera (doc "Delululand — Camera" CA2–CA4, CA10, CA11): Take a photo and Record a video hand off
 * to the phone's own camera app, so its lens switch, flash, zoom and HDR are the ones the citizen
 * knows. The app holds no CAMERA permission: the camera app has its own.
 *
 * Shots land in cache/camera/ through the `.photos` FileProvider and never reach the gallery.
 * Each new capture sweeps files there older than a day (an upload is long done by then).
 */
class CameraCapture internal constructor(val photo: () -> Unit, val video: () -> Unit)

/** CA3: the tier's length asked of the camera app; the composer re-checks it, as not all obey. */
private class CaptureVideoLimited : ActivityResultContracts.CaptureVideo() {
    override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input)
        .putExtra(MediaStore.EXTRA_DURATION_LIMIT, VideoLimits.seconds)
        .putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1)
}

/**
 * Selfies Q1: the phone's camera app, asked to open on the front lens. Not every camera app reads
 * these extras, and the citizen can still switch lenses there (decided 21 Nov 2026).
 */
private class TakeFrontPicture : ActivityResultContracts.TakePicture() {
    override fun createIntent(context: Context, input: Uri): Intent = super.createIntent(context, input)
        .putExtra("android.intent.extras.CAMERA_FACING", 1)
        .putExtra("android.intent.extras.LENS_FACING_FRONT", 1)
        .putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
}

/** Selfies DS4: one front-camera shot into cache/camera/, as [rememberCameraCapture]'s photos (CA10). */
@Composable
fun rememberSelfieCapture(onPhoto: (Uri) -> Unit, onNoCamera: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val target = remember { arrayOfNulls<Uri>(1) }
    val done by rememberUpdatedState(onPhoto)
    val none by rememberUpdatedState(onNoCamera)
    val take = rememberLauncherForActivityResult(TakeFrontPicture()) { ok ->
        target[0]?.takeIf { ok && sizeOf(context, it) > 0 }?.let(done)
    }
    return remember(take) {
        {
            val uri = captureUri(context, "jpg")
            target[0] = uri
            try {
                take.launch(uri)
            } catch (_: ActivityNotFoundException) {
                none()
            } catch (_: SecurityException) {
                none()
            }
        }
    }
}

@Composable
fun rememberCameraCapture(onPhoto: (Uri) -> Unit, onVideo: (Uri) -> Unit, onNoCamera: () -> Unit): CameraCapture {
    val context = LocalContext.current
    val target = remember { arrayOfNulls<Uri>(1) }
    val photoDone by rememberUpdatedState(onPhoto)
    val videoDone by rememberUpdatedState(onVideo)
    val none by rememberUpdatedState(onNoCamera)
    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        target[0]?.takeIf { ok && sizeOf(context, it) > 0 }?.let(photoDone)
    }
    val takeVideo = rememberLauncherForActivityResult(CaptureVideoLimited()) { ok ->
        target[0]?.takeIf { ok && sizeOf(context, it) > 0 }?.let(videoDone)
    }
    return remember(takePhoto, takeVideo) {
        fun launch(ext: String, start: (Uri) -> Unit) {
            val uri = captureUri(context, ext)
            target[0] = uri
            try {
                start(uri)
            } catch (_: ActivityNotFoundException) {
                none()
            } catch (_: SecurityException) {
                none()
            }
        }
        CameraCapture(
            photo = { launch("jpg") { takePhoto.launch(it) } },
            video = { launch("mp4") { takeVideo.launch(it) } },
        )
    }
}

private const val DIR = "camera"
private const val DAY_MS = 24L * 60 * 60 * 1000

private fun captureUri(context: Context, ext: String): Uri {
    val dir = File(context.cacheDir, DIR).apply { mkdirs() }
    val now = System.currentTimeMillis()
    dir.listFiles()?.filter { now - it.lastModified() > DAY_MS }?.forEach { it.delete() }
    val file = File(dir, "taken-$now.$ext")
    return FileProvider.getUriForFile(context, context.packageName + ".photos", file)
}

/** Some camera apps report OK yet write nothing; an empty file is no shot. */
private fun sizeOf(context: Context, uri: Uri): Long = runCatching {
    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } ?: 0L
}.getOrDefault(0L)
