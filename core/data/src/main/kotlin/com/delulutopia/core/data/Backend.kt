package com.delulutopia.core.data

import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.LiveUpdates
import com.delulutopia.core.network.ServerConfig
import com.delulutopia.core.network.Uploader
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope

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
) {
    suspend fun <T> run(block: suspend DelulutopiaApi.() -> T): T = caller.call { api.block() }

    /** Absolute URL of an API media path (`/api/attachments/…`). */
    fun url(path: String): String = if (path.startsWith("http")) path else config.baseUrl().trimEnd('/') + "/" + path.trimStart('/')

    fun refresher(
        scope: CoroutineScope,
        signals: Set<String>,
        pollMs: Long = Refresher.POLL_MS,
        deadlines: () -> List<String?> = { emptyList() },
        reload: suspend () -> Unit,
    ): Refresher = Refresher(scope, live, signals, pollMs, deadlines, reload).start()
}
