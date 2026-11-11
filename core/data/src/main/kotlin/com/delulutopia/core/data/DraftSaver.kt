package com.delulutopia.core.data

import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Saved
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Autosave for a draft (FS-W3, FS-W4, O9; web editor/DraftSaver.ts), for letters, discussions and
 * emails alike:
 * - saved no later than 2 s after the last change, and at once on leaving or hiding the screen;
 * - every save carries the version it was based on; `409 DRAFT_CONFLICT` stops autosave until the
 *   user picks Load or Keep mine;
 * - a failed save shows "Couldn't save your draft" and retries every 10 s with the text untouched;
 * - Send flushes first, so what is sent is exactly what was on screen.
 * [save] reads the current content itself and saves it on top of the given version.
 */
class DraftSaver(
    private val scope: CoroutineScope,
    private val save: suspend (version: Int) -> Saved,
    private val debounceMs: Long = 2_000,
    private val retryMs: Long = 10_000,
) {
    enum class Status { Clean, Saving, Saved, Error, Conflict }

    data class State(val status: Status = Status.Clean, val savedAt: String? = null, val serverVersion: Int? = null)

    class ConflictException : Exception("A newer version was saved on another device. Load it?")

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    var version: Int = 0
        private set
    var dirty: Boolean = false
        private set
    private var timer: Job? = null
    private var inFlight: CompletableDeferred<Unit>? = null
    private var conflictVersion: Int? = null

    /** The server's draft, at load or after Load in the conflict dialog. */
    fun load(version: Int, savedAt: String?) {
        timer?.cancel()
        this.version = version
        dirty = false
        conflictVersion = null
        _state.value = State(Status.Clean, savedAt)
    }

    /** Anything saved changed (text, pin, stamp, title, colour). */
    fun touch() {
        dirty = true
        if (_state.value.status == Status.Conflict) return
        schedule(debounceMs)
    }

    /** Before Send: the version the server holds once everything is saved. */
    suspend fun flush(): Int {
        repeat(3) {
            inFlight?.await()
            if (_state.value.status == Status.Conflict) throw ConflictException()
            if (!dirty) {
                if (_state.value.status == Status.Error) throw IllegalStateException("Couldn't save your draft")
                return version
            }
            saveNow()
        }
        if (_state.value.status == Status.Conflict) throw ConflictException()
        if (dirty || _state.value.status == Status.Error) throw IllegalStateException("Couldn't save your draft")
        return version
    }

    /** The screen is left or hidden: save now, outliving the screen (the web's `keepalive`). */
    fun flushOnHide() {
        if (dirty && _state.value.status != Status.Conflict) {
            CoroutineScope(Dispatchers.IO).launch { withContext(NonCancellable) { saveNow() } }
        }
    }

    /** The conflict dialog's Keep mine: re-send on top of the server's version. */
    fun keepMine() {
        val v = conflictVersion ?: return
        version = v
        conflictVersion = null
        dirty = true
        _state.value = _state.value.copy(status = Status.Saving, serverVersion = null)
        scope.launch { saveNow() }
    }

    fun dispose() {
        timer?.cancel()
    }

    private fun schedule(ms: Long) {
        timer?.cancel()
        timer = scope.launch {
            delay(ms)
            saveNow()
        }
    }

    private suspend fun saveNow() {
        inFlight?.let {
            it.await()
            if (!dirty || _state.value.status == Status.Conflict) return
        }
        if (!dirty || _state.value.status == Status.Conflict) return
        timer?.cancel()
        val done = CompletableDeferred<Unit>()
        inFlight = done
        dirty = false
        _state.value = _state.value.copy(status = Status.Saving)
        try {
            val r = save(version)
            version = r.version
            _state.value = State(Status.Saved, r.savedAt)
            if (dirty) schedule(debounceMs)
        } catch (e: ApiException) {
            dirty = true
            if (e.code == "DRAFT_CONFLICT") {
                conflictVersion = (e.extra["serverVersion"] as? JsonPrimitive)?.intOrNull
                _state.value = _state.value.copy(status = Status.Conflict, serverVersion = conflictVersion)
            } else {
                _state.value = _state.value.copy(status = Status.Error)
                schedule(retryMs)
            }
        } finally {
            inFlight = null
            done.complete(Unit)
        }
    }

    companion object {
        /** `savedAt` of a refusal's extra, when it carries one. */
        fun savedAtOf(e: ApiException): String? = (e.extra["savedAt"] as? JsonPrimitive)?.contentOrNull
    }
}
