package com.delulutopia.core.network

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * A refusal from the API (Technical Design §5.1): `{ "code", "message", …extra }`. [message] is the
 * spec's user text, shown as is. [status] 0 means the server was not reached (see [isOffline]).
 */
class ApiException(
    val status: Int,
    val code: String,
    override val message: String,
    val extra: JsonObject = JsonObject(emptyMap()),
    cause: Throwable? = null,
) : Exception(message, cause) {

    /** No answer, or a 5xx: the screen says "Can't reach <universe>. Try again." */
    val isOffline: Boolean get() = status == 0 || status >= 500

    /** Throttle answers carry how long to wait before the one silent resubmit. */
    val retryAfterMs: Long? get() = (extra["retryAfterMs"] as? JsonPrimitive)?.longOrNull

    companion object {
        const val OFFLINE = "OFFLINE"
    }
}
