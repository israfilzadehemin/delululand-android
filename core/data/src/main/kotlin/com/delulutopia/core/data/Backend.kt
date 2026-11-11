package com.delulutopia.core.data

import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.LiveUpdates
import com.delulutopia.core.network.ServerConfig
import com.delulutopia.core.network.Uploader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * One door to the API for every screen: `backend.run { letters() }` turns failures into
 * `ApiException` (the API's code and message, or offline). Uploads, live updates and media URLs too.
 */
@Singleton
class Backend @Inject constructor(
    val api: DelulutopiaApi,
    private val caller: ApiCaller,
    val uploader: Uploader,
    val live: LiveUpdates,
    private val config: ServerConfig,
    val json: Json,
) {
    suspend fun <T> run(block: suspend DelulutopiaApi.() -> T): T = caller.call { api.block() }

    /** Absolute URL of an API media path (`/api/attachments/…`). */
    fun url(path: String): String = if (path.startsWith("http")) path else config.baseUrl().trimEnd('/') + "/" + path.trimStart('/')

    /** A value the API put beside a refusal (`BOARD_CHANGED` carries the current `board`). */
    fun <T> extra(e: ApiException, key: String, serializer: KSerializer<T>): T? =
        e.extra[key]?.let { runCatching { json.decodeFromJsonElement(serializer, it) }.getOrNull() }

    fun refresher(
        scope: CoroutineScope,
        signals: Set<String>,
        pollMs: Long = Refresher.POLL_MS,
        deadlines: () -> List<String?> = { emptyList() },
        reload: suspend () -> Unit,
    ): Refresher = Refresher(scope, live, signals, pollMs, deadlines, reload).start()
}
