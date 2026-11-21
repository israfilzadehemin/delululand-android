package com.delulutopia.feature.emails.stickers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.delulutopia.core.data.Backend
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.Signal
import com.delulutopia.core.network.model.StickerCatalogue
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The sticker catalogue (doc "Delululand — Stickers" §5.3; web api/stickers.ts): one call, kept for the
 * app's life, refetched when either citizen adds or deletes one of Ours (the `stickers` live signal),
 * after a favourite and after a send (recents).
 */
@Singleton
class StickerStore internal constructor(private val backend: Backend, private val scope: CoroutineScope) {
    @Inject constructor(backend: Backend) : this(backend, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))

    var catalogue by mutableStateOf<StickerCatalogue?>(null)
        private set
    var failed by mutableStateOf(false)
        private set
    private var listening = false

    /** Loads once; later calls are free. */
    fun load() {
        if (!listening) {
            listening = true
            scope.launch {
                backend.live.signals.collect { s -> if (s.name == SIGNAL || s.name == Signal.READY) refresh() }
            }
        }
        if (catalogue == null) refresh()
    }

    fun refresh() {
        scope.launch {
            try {
                catalogue = backend.run { stickers() }
                failed = false
            } catch (_: ApiException) {
                failed = catalogue == null
            }
        }
    }

    fun isFavourite(id: String): Boolean = catalogue?.favourites?.contains(id) == true

    /** SK7: the star in the layer and the sheet. The catalogue answers. */
    fun toggleFavourite(id: String) {
        val on = isFavourite(id)
        scope.launch {
            runCatching { backend.run { if (on) unfavouriteSticker(id) else favouriteSticker(id) } }
            refresh()
        }
    }

    /** SK13, FS-SK19–SK22: one image, drawn, cut out or imported. Null when added, else the words to show. */
    suspend fun add(bytes: ByteArray, mime: String, keywords: String): String? {
        if (bytes.size > MAX_BYTES) return t("stickers.tooLarge")
        return try {
            val file = MultipartBody.Part.createFormData("file", "sticker" + extensionOf(mime), bytes.toRequestBody(mime.toMediaType()))
            val words = keywords.trim().takeIf { it.isNotEmpty() }?.toRequestBody("text/plain".toMediaType())
            backend.run { addSticker(file, words) }
            refresh()
            null
        } catch (e: ApiException) {
            if (e.isOffline) t("common.notSavedTryAgain") else e.message
        }
    }

    /** FS-SK25: either citizen; emails that used it keep it. */
    suspend fun delete(id: String): Boolean = try {
        backend.run { deleteSticker(id) }
        refresh()
        true
    } catch (_: ApiException) {
        false
    }

    /** Tests only. */
    internal fun set(c: StickerCatalogue?) {
        catalogue = c
    }

    companion object {
        const val SIGNAL = "stickers"

        /** SK15: the server refuses anything larger. */
        const val MAX_BYTES = 1024 * 1024
        val TYPES = listOf("image/png", "image/webp", "image/gif", "image/jpeg")

        fun extensionOf(mime: String): String = when (mime) {
            "image/png" -> ".png"
            "image/webp" -> ".webp"
            "image/gif" -> ".gif"
            "image/jpeg" -> ".jpg"
            else -> ""
        }
    }
}
