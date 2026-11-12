package com.delulutopia.core.network

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Every call: `X-Requested-With: delululand` (the API's CSRF rule, Technical Design §8.3), a
 * `User-Agent` naming the app (the server lists sessions by it), and the server clock fed from
 * `X-Server-Time`. `Accept` is left to the caller: images (Coil), voice notes and videos (ExoPlayer)
 * share this client, and forcing `application/json` on them made the API answer 406 — no photo,
 * avatar, stamp, doodle, board, voice note or video ever loaded.
 */
class ApiHeadersInterceptor(
    private val clock: ServerClock,
    private val userAgent: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("X-Requested-With", "delululand")
            .header("User-Agent", userAgent)
            .build()
        val response = chain.proceed(request)
        response.header("X-Server-Time")?.toLongOrNull()?.let(clock::record)
        return response
    }
}

/** FS-A13: a 401 `NO_SESSION` anywhere means the session is gone; the app returns to the start. */
class SessionEvents {
    private val ended = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    val sessionEnded: SharedFlow<Unit> = ended

    fun emitEnded() {
        ended.tryEmit(Unit)
    }
}
