package com.delulutopia.core.network

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources

/** One change in this universe (Backend Refactor Plan items 6–9): its name and at most an id, never content. */
data class Signal(val name: String, val id: Long?) {
    companion object {
        /** The stream (re)opened: anything fetched while it was down may be stale. */
        const val READY = "ready"
    }
}

/**
 * Live updates (web src/live/liveUpdates.ts): one `/api/events` stream while the app is in the
 * foreground and signed in. Screens refetch only what a signal names. While the stream is not live
 * (connecting, dropped, refused) every screen polls exactly as the web does, so behaviour never
 * depends on the stream. A refused stream is retried after 30 s, doubling to 5 min.
 */
@Singleton
class LiveUpdates @Inject constructor(
    client: OkHttpClient,
    private val config: ServerConfig,
    private val json: Json,
) {
    private val sseClient = client.newBuilder().readTimeout(0, java.util.concurrent.TimeUnit.SECONDS).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _live = MutableStateFlow(false)
    val live: StateFlow<Boolean> = _live.asStateFlow()

    private val _signals = MutableSharedFlow<Signal>(extraBufferCapacity = 64)
    val signals: SharedFlow<Signal> = _signals

    private var source: EventSource? = null
    private var retryJob: Job? = null
    private var retryMs = RETRY_FIRST_MS
    private var running = false

    @Synchronized
    fun start() {
        if (running) return
        running = true
        open()
    }

    @Synchronized
    fun stop() {
        running = false
        retryJob?.cancel()
        source?.cancel()
        source = null
        _live.value = false
    }

    @Synchronized
    private fun open() {
        if (!running) return
        val request = Request.Builder().url(config.baseUrl().trimEnd('/') + "/api/events").header("Accept", "text/event-stream").build()
        source = EventSources.createFactory(sseClient).newEventSource(
            request,
            object : EventSourceListener() {
                override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                    when (type) {
                        Signal.READY -> {
                            retryMs = RETRY_FIRST_MS
                            _live.value = true
                            _signals.tryEmit(Signal(Signal.READY, null))
                        }
                        null -> Unit
                        else -> _signals.tryEmit(Signal(type, idOf(data)))
                    }
                }

                override fun onClosed(eventSource: EventSource) = dropped()

                override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) = dropped()
            },
        )
    }

    /** The server closes the stream every 30 min (reconnect at once); a refusal waits and backs off. */
    private fun dropped() {
        val wasLive = _live.value
        _live.value = false
        synchronized(this) {
            if (!running) return
            source = null
            retryJob?.cancel()
            val wait = if (wasLive) 1_000L else retryMs.also { retryMs = (retryMs * 2).coerceAtMost(RETRY_MAX_MS) }
            retryJob = scope.launch {
                delay(wait)
                open()
            }
        }
    }

    private fun idOf(data: String): Long? = runCatching {
        (json.parseToJsonElement(data).jsonObject["id"] as? JsonPrimitive)?.longOrNull
    }.getOrNull()

    private companion object {
        const val RETRY_FIRST_MS = 30_000L
        const val RETRY_MAX_MS = 5 * 60_000L
    }
}
