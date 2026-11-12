package com.delulutopia.core.ui.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.network.model.Att
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

/** Where an attachment's bytes come from: something picked (a content Uri) or made here (a doodle, a recording). */
sealed interface Source {
    data class Picked(val uri: Uri) : Source

    data class Made(val bytes: ByteArray, val mime: String) : Source
}

/** An upload in flight: progress fills, then `processing` while the server converts. */
data class Pending(val kind: Uploader.Kind, val progress: Float = 0f, val processing: Boolean = false, val durationMs: Long? = null)

/** One strip entry: a saved attachment, or one still uploading; [local] previews it meanwhile. */
data class Item(val key: String, val att: Att? = null, val pending: Pending? = null, val local: Any? = null) {
    val kind: String get() = att?.kind ?: pending?.kind?.wire.orEmpty()
}

private const val READ_FAILED = "This file couldn't be read from your phone"
private const val LONG_EDGE = 2048
private const val DECODE_MAX = 40L * 1024 * 1024

/**
 * The attachment strip's state (FS-W6–W10; web media/useAttachments.ts): server attachments plus
 * uploads in flight, two at a time, in the order they were added. Refusals become lines under the
 * strip with the spec's text, never a silent failure (O4). Photos are shrunk to 2048 px JPEG on the
 * phone first (web media/prepare.ts).
 */
@Stable
class Attachments(
    initial: List<Att>,
    private val uploadPath: String,
    private val scope: CoroutineScope,
    private val uploader: Uploader,
    private val context: Context,
) {
    var items by mutableStateOf(initial.map { Item(it.id, att = it) })
        private set
    var errors by mutableStateOf(listOf<Pair<String, String>>())
        private set
    private val gate = Semaphore(2)
    private val jobs = HashMap<String, Job>()
    private var seq = 0

    val uploading: Boolean get() = items.any { it.pending != null }
    val done: List<Att> get() = items.mapNotNull { it.att }

    fun has(kind: String) = items.any { it.kind == kind }

    fun add(kind: Uploader.Kind, source: Source, name: String, durationMs: Long? = null) {
        val key = "up-${++seq}"
        val local: Any = when (source) {
            is Source.Picked -> source.uri
            is Source.Made -> source.bytes
        }
        items = items + Item(key, pending = Pending(kind, durationMs = durationMs), local = local)
        jobs[key] = scope.launch {
            gate.withPermit {
                try {
                    // Progress arrives on OkHttp's thread; the strip is changed on the main thread only,
                    // so two uploads finishing together can't overwrite each other's state.
                    var shown = -1
                    val progress: (Float) -> Unit = { f ->
                        val step = (f * 50).toInt()
                        if (step != shown) {
                            shown = step
                            scope.launch(Dispatchers.Main) { patch(key) { p -> p.copy(progress = f, processing = f >= 0.99f) } }
                        }
                    }
                    val streamed = (source as? Source.Picked)?.takeIf { kind != Uploader.Kind.Photo }?.let { picked ->
                        withContext(Dispatchers.IO) { sizeOf(picked.uri) }?.let { size -> picked.uri to size }
                    }
                    val att = if (streamed != null) {
                        // Audio and video files are streamed from the phone, never read whole into memory.
                        val (uri, size) = streamed
                        val mime = context.contentResolver.getType(uri) ?: defaultMime(kind)
                        uploader.upload(uploadPath, kind, size, { context.contentResolver.openInputStream(uri) ?: throw ReadException() }, name, mime, progress)
                    } else {
                        val (bytes, file, mime) = withContext(Dispatchers.IO) { prepare(kind, source, name) }
                        Uploader.precheck(kind, bytes.size.toLong())?.let { throw ApiException(413, "TOO_LARGE", it) }
                        uploader.upload(uploadPath, kind, { bytes }, file, mime, progress)
                    }
                    items = items.map { if (it.key == key) Item(key, att = att, local = it.local) else it }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    fail(key, e.message)
                } catch (_: ReadException) {
                    fail(key, READ_FAILED)
                } catch (_: Exception) {
                    fail(key, Uploader.UPLOAD_FAILED)
                } finally {
                    jobs.remove(key)
                }
            }
        }
    }

    private fun patch(key: String, change: (Pending) -> Pending) {
        items = items.map { if (it.key == key && it.pending != null) it.copy(pending = change(it.pending)) else it }
    }

    private fun fail(key: String, text: String) {
        items = items.filterNot { it.key == key }
        errors = errors + (key to text)
    }

    /** Drop from the strip; an upload still in flight is cancelled. The caller deletes on the server if it should. */
    fun remove(key: String) {
        jobs.remove(key)?.cancel()
        items = items.filterNot { it.key == key }
    }

    fun restore(item: Item, index: Int) {
        items = items.toMutableList().apply { add(index.coerceAtMost(size), item) }
    }

    /** Replace everything (Load in the conflict dialog). */
    fun reset(atts: List<Att>) {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        items = atts.map { Item(it.id, att = it) }
    }

    fun dismissError(key: String) {
        errors = errors.filterNot { it.first == key }
    }

    private class ReadException : java.io.IOException()

    /** The exact length of a picked file, or null when the provider doesn't say (then it is read whole). */
    private fun sizeOf(uri: Uri): Long? = runCatching {
        context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0).takeIf { it > 0 } else null
        }
    }.getOrNull()

    /** A stable copy of the bytes; a photo shrunk to 2048 px on its long edge, upright, as JPEG. */
    private fun prepare(kind: Uploader.Kind, source: Source, name: String): Triple<ByteArray, String, String> {
        val (bytes, mime) = when (source) {
            is Source.Made -> source.bytes to source.mime
            is Source.Picked -> {
                val data = runCatching { context.contentResolver.openInputStream(source.uri)?.use { it.readBytes() } }.getOrNull() ?: throw ReadException()
                data to (context.contentResolver.getType(source.uri) ?: defaultMime(kind))
            }
        }
        if (kind != Uploader.Kind.Photo || bytes.size > DECODE_MAX) return Triple(bytes, name, mime)
        val small = shrink(bytes) ?: return Triple(bytes, name, mime)
        if (small.size >= bytes.size) return Triple(bytes, name, mime)
        return Triple(small, name.substringBeforeLast('.') + ".jpg", "image/jpeg")
    }

    private fun shrink(bytes: ByteArray): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        val long = max(bounds.outWidth, bounds.outHeight)
        val orientation = ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        if (long <= LONG_EDGE && orientation == ExifInterface.ORIENTATION_NORMAL) return null
        var sample = 1
        while (long / (sample * 2) >= LONG_EDGE) sample *= 2
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample }) ?: return null
        val k = minOf(1f, LONG_EDGE.toFloat() / max(decoded.width, decoded.height))
        val matrix = Matrix().apply {
            postScale(k, k)
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }
        val turned = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
        // JPEG has no alpha: a transparent PNG lands on white, as the web does.
        val flat = Bitmap.createBitmap(turned.width, turned.height, Bitmap.Config.ARGB_8888)
        Canvas(flat).apply {
            drawColor(Color.WHITE)
            drawBitmap(turned, 0f, 0f, null)
        }
        val out = ByteArrayOutputStream()
        flat.compress(Bitmap.CompressFormat.JPEG, 90, out)
        if (turned !== decoded) turned.recycle()
        decoded.recycle()
        flat.recycle()
        out.toByteArray()
    }.getOrNull()

    private fun defaultMime(kind: Uploader.Kind) = when (kind) {
        Uploader.Kind.Photo -> "image/jpeg"
        Uploader.Kind.Audio -> "audio/mp4"
        Uploader.Kind.Video -> "video/mp4"
        Uploader.Kind.Doodle -> "image/png"
    }
}
