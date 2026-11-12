package com.delulutopia.core.data

import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.i18n.Lang
import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.model.ChangePinBody
import com.delulutopia.core.network.model.CreateUniverseRequest
import com.delulutopia.core.network.model.CreatedUniverse
import com.delulutopia.core.network.model.EnterRequest
import com.delulutopia.core.network.model.Entered
import com.delulutopia.core.network.model.EntryView
import com.delulutopia.core.network.model.InviteRequest
import com.delulutopia.core.network.model.JoinRequest
import com.delulutopia.core.network.model.LanguageBody
import com.delulutopia.core.network.model.LoginRequest
import com.delulutopia.core.network.model.Profile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.delay
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * The key, the cards, the PIN and creating or joining a universe (doc "Delululand — Universe" §2,
 * FS-A3–A8). Throttle answers (`CODE_THROTTLED`, `PIN_THROTTLED`) are invisible: wait as told (at
 * most 2 s) and resubmit once; a second one surfaces like a wrong key or PIN, as on the web.
 */
@Singleton
class AccountRepository @Inject constructor(
    private val api: DelulutopiaApi,
    private val caller: ApiCaller,
) {
    suspend fun enter(code: String): Entered = withThrottleRetry("CODE_THROTTLED") { api.enter(EnterRequest(code)) }

    /** UV-4 cards; `401 ENTRY_REQUIRED` when this device holds no entry pass. */
    suspend fun entry(): EntryView = caller.call { api.entry() }

    /** "Use a different key". */
    suspend fun forgetEntry() {
        runCatching { caller.call { api.forgetEntry() } }
    }

    suspend fun login(userId: Int, pin: String) = withThrottleRetry("PIN_THROTTLED") { api.login(LoginRequest(userId, pin)) }

    suspend fun createPin(userId: Int, pin: String) = withThrottleRetry("PIN_THROTTLED") { api.createPin(LoginRequest(userId, pin)) }

    suspend fun checkInvite(invite: String) = caller.call { api.checkInvite(InviteRequest(invite)) }

    suspend fun createUniverse(request: CreateUniverseRequest): CreatedUniverse = caller.call { api.createUniverse(request) }

    suspend fun join(username: String, timezone: String, pin: String) = caller.call { api.join(JoinRequest(username, timezone, pin, I18n.lang.tag)) }

    /** You › Language (L8): saved to the account; the other devices follow through the live signal `language`. */
    suspend fun setLanguage(lang: Lang) = caller.call { api.setLanguage(LanguageBody(lang.tag)) }

    /** FS-A17: every other device stays logged in. */
    suspend fun changePin(current: String, next: String) = withThrottleRetry("PIN_THROTTLED") { api.changePin(ChangePinBody(current, next)) }

    /** P2, U9: the cropped 384 px JPEG. */
    suspend fun uploadAvatar(jpeg: ByteArray): Profile = caller.call {
        api.uploadAvatar(MultipartBody.Part.createFormData("file", "avatar.jpg", jpeg.toRequestBody("image/jpeg".toMediaType())))
    }

    suspend fun removeAvatar(): Profile = caller.call { api.removeAvatar() }

    private suspend fun <T> withThrottleRetry(throttleCode: String, block: suspend () -> T): T {
        try {
            return caller.call(block)
        } catch (e: ApiException) {
            if (e.code != throttleCode) throw e
            delay((e.retryAfterMs ?: 1_000L).coerceAtMost(2_000L) + 50)
        }
        return caller.call(block)
    }
}
