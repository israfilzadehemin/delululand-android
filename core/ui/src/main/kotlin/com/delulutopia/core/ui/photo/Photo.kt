package com.delulutopia.core.ui.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.delulutopia.core.designsystem.component.DlBottomSheet
import com.delulutopia.core.designsystem.component.DlListItem
import com.delulutopia.core.designsystem.component.DlTextButton
import com.delulutopia.core.designsystem.icon.WebIcons
import com.delulutopia.core.i18n.t
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max

/** Q4: the web refuses photos over 10 MB before cropping. */
const val PHOTO_MAX = 10L * 1024 * 1024

/** P2: the crop's output, a square JPEG this many pixels wide. */
const val CROP_OUT = 384

/** Choose (system photo picker) or take (the camera app) a photo; [onPicked] gets its uri. */
class PhotoSource internal constructor(val choose: () -> Unit, val take: () -> Unit)

@Composable
fun rememberPhotoSource(onPicked: (Uri) -> Unit): PhotoSource {
    val context = LocalContext.current
    val target = remember { arrayOfNulls<Uri>(1) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok -> if (ok) target[0]?.let(onPicked) }
    return remember(picker, camera) {
        PhotoSource(
            choose = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            take = {
                val dir = File(context.cacheDir, "photos").apply { mkdirs() }
                val file = File(dir, "taken-${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(context, context.packageName + ".photos", file)
                target[0] = uri
                runCatching { camera.launch(uri) }
            },
        )
    }
}

/** "Change photo" / "Add photo": Choose photo, Take photo, Remove photo (when there is one), Cancel. */
@Composable
fun PhotoSourceSheet(source: PhotoSource, onDismiss: () -> Unit, onRemove: (() -> Unit)? = null) {
    DlBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            DlListItem(title = t("profile.choosePhoto"), icon = WebIcons.Photo, showChevron = false, onClick = {
                onDismiss()
                source.choose()
            })
            DlListItem(title = t("profile.takePhoto"), icon = WebIcons.Camera, showChevron = false, onClick = {
                onDismiss()
                source.take()
            })
            if (onRemove != null) {
                DlListItem(title = t("universe.removePhoto"), icon = WebIcons.Trash, showChevron = false, onClick = {
                    onDismiss()
                    onRemove()
                })
            }
            DlTextButton(t("profile.cancel"), onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/** The picked file's size in bytes, when the provider says. */
fun sizeOf(context: Context, uri: Uri): Long? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
    }
}.getOrNull() ?: runCatching { File(uri.path ?: "").takeIf { it.exists() }?.length() }.getOrNull()

/** Decodes a picked photo upright (EXIF), its long edge at most [maxEdge]; null when unreadable. */
fun decodePhoto(context: Context, uri: Uri, maxEdge: Int = 2048): Bitmap? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0) return null
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
    val orientation = ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
        }
    }
    if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
}.getOrNull()

/** The square of [photo] seen through the crop window, as a [CROP_OUT] px JPEG on white. */
fun cropJpeg(photo: Bitmap, left: Float, top: Float, side: Float): ByteArray {
    val out = Bitmap.createBitmap(CROP_OUT, CROP_OUT, Bitmap.Config.ARGB_8888)
    Canvas(out).apply {
        drawColor(Color.WHITE)
        val k = CROP_OUT / side
        val dst = RectF(-left * k, -top * k, (photo.width - left) * k, (photo.height - top) * k)
        drawBitmap(photo, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
    }
    val bytes = ByteArrayOutputStream()
    out.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
    out.recycle()
    return bytes.toByteArray()
}
