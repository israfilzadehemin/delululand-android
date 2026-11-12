package com.delulutopia.core.network

import com.delulutopia.core.network.model.Att
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.ForwardingSink
import okio.buffer
import okio.source

/**
 * Attachment uploads with progress (web src/media/upload.ts): `kind` + `file` multipart, `201 Att`,
 * the spec's refusal text otherwise. Whole-request ceilings per kind so a stalled connection ends in a
 * message instead of a ring that never moves.
 */
@Singleton
class Uploader @Inject constructor(
    private val client: OkHttpClient,
    private val config: ServerConfig,
    private val json: Json,
    private val events: SessionEvents,
) {
    enum class Kind(val wire: String, val timeoutS: Long) { Photo("photo", 120), Audio("audio", 180), Video("video", 360), Doodle("doodle", 60) }

    suspend fun upload(
        path: String,
        kind: Kind,
        bytes: () -> ByteArray,
        filename: String,
        mime: String,
        onProgress: (Float) -> Unit,
    ): Att {
        val data = bytes()
        return upload(path, kind, data.size.toLong(), { data.inputStream() }, filename, mime, onProgress)
    }

    /**
     * A file streamed from where it lives (a picked video can be 100 MB: it is never held in memory
     * whole). [open] is called once per attempt; [size] must be the exact length.
     */
    suspend fun upload(
        path: String,
        kind: Kind,
        size: Long,
        open: () -> java.io.InputStream,
        filename: String,
        mime: String,
        onProgress: (Float) -> Unit,
    ): Att {
        precheck(kind, size)?.let { throw ApiException(413, "TOO_LARGE", it) }
        val stream = object : RequestBody() {
            override fun contentType(): MediaType = mime.toMediaType()

            override fun contentLength(): Long = size

            override fun writeTo(sink: BufferedSink) {
                open().use { input -> sink.writeAll(input.source()) }
            }
        }
        val form = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("kind", kind.wire)
            .addFormDataPart("file", filename, ProgressBody(stream, onProgress))
            .build()
        val request = Request.Builder().url(config.baseUrl().trimEnd('/') + "/" + path.trimStart('/')).post(form).build()
        val call = client.newBuilder().callTimeout(kind.timeoutS, TimeUnit.SECONDS).build().newCall(request)
        val (code, body) = call.await()
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        if (code == 201) {
            onProgress(1f)
            return json.decodeFromString(Att.serializer(), body)
        }
        val c = (obj?.get("code") as? JsonPrimitive)?.contentOrNull ?: "HTTP_$code"
        if (code == 401 && c == "NO_SESSION") events.emitEnded()
        val message = (obj?.get("message") as? JsonPrimitive)?.contentOrNull ?: UPLOAD_FAILED
        throw ApiException(code, c, message, JsonObject(obj.orEmpty().filterKeys { it != "code" && it != "message" }))
    }

    private suspend fun Call.await(): Pair<Int, String> = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { cancel() }
        enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val timedOut = e is java.io.InterruptedIOException
                    cont.resumeWithException(
                        ApiException(0, if (timedOut) "TIMEOUT" else "NETWORK", if (timedOut) UPLOAD_TIMED_OUT else UPLOAD_FAILED, cause = e),
                    )
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { cont.resume(it.code to it.body.string()) }
                }
            },
        )
    }

    private class ProgressBody(private val delegate: RequestBody, private val onProgress: (Float) -> Unit) : RequestBody() {
        override fun contentType(): MediaType? = delegate.contentType()

        override fun contentLength(): Long = delegate.contentLength()

        override fun writeTo(sink: BufferedSink) {
            val total = contentLength().coerceAtLeast(1)
            var written = 0L
            val counting = object : ForwardingSink(sink) {
                override fun write(source: Buffer, byteCount: Long) {
                    super.write(source, byteCount)
                    written += byteCount
                    onProgress((written.toFloat() / total).coerceAtMost(0.99f))
                }
            }.buffer()
            delegate.writeTo(counting)
            counting.flush()
        }
    }

    companion object {
        const val PHOTO_MAX = 10L * 1024 * 1024
        const val AUDIO_MAX = 60L * 1024 * 1024
        const val VIDEO_MAX = 100L * 1024 * 1024
        const val DOODLE_MAX = 2L * 1024 * 1024
        const val PHOTO_TOO_LARGE = "This photo is larger than 10 MB"
        const val AUDIO_TOO_LARGE = "This audio file is larger than 60 MB"
        const val VIDEO_TOO_LARGE = "This video is larger than 100 MB"
        const val DOODLE_TOO_LARGE = "This doodle is larger than 2 MB"
        const val UPLOAD_FAILED = "Upload failed. Try again."
        const val UPLOAD_TIMED_OUT = "The upload took too long. Try again."

        /** The refusal the user sees before any byte leaves the phone, or null (O4). */
        fun precheck(kind: Kind, size: Long): String? = when {
            kind == Kind.Photo && size > PHOTO_MAX -> PHOTO_TOO_LARGE
            kind == Kind.Audio && size > AUDIO_MAX -> AUDIO_TOO_LARGE
            kind == Kind.Video && size > VIDEO_MAX -> VIDEO_TOO_LARGE
            kind == Kind.Doodle && size > DOODLE_MAX -> DOODLE_TOO_LARGE
            else -> null
        }
    }
}
