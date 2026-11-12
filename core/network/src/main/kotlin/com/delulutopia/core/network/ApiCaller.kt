package com.delulutopia.core.network

import com.delulutopia.core.i18n.t
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import retrofit2.HttpException

/**
 * Runs one API call and turns every failure into an [ApiException]: the API's `{code, message}` for
 * refusals, [ApiException.OFFLINE] (status 0) when the server was not reached.
 */
class ApiCaller(private val json: Json, private val events: SessionEvents) {

    suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        throw toApiException(e).also { if (it.status == 401 && it.code == "NO_SESSION") events.emitEnded() }
    } catch (e: IOException) {
        throw ApiException(0, ApiException.OFFLINE, t("common.cantReachServer"), cause = e)
    } catch (e: SerializationException) {
        throw ApiException(0, "BAD_RESPONSE", t("common.somethingWentWrong"), cause = e)
    }

    private fun toApiException(e: HttpException): ApiException {
        val body = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
        val obj = body?.takeIf { it.isNotBlank() }?.let { runCatching { json.parseToJsonElement(it).jsonObject }.getOrNull() }
        val code = (obj?.get("code") as? JsonPrimitive)?.contentOrNull ?: "HTTP_${e.code()}"
        val message = (obj?.get("message") as? JsonPrimitive)?.contentOrNull ?: t("common.somethingWentWrong")
        val extra = JsonObject(obj.orEmpty().filterKeys { it != "code" && it != "message" })
        return ApiException(e.code(), code, message, extra, e)
    }
}
