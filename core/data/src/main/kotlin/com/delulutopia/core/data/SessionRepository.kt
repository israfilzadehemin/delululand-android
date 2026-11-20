package com.delulutopia.core.data

import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.SessionEvents
import com.delulutopia.core.network.model.DeleteMeBody
import com.delulutopia.core.network.model.DeleteUniverseBody
import com.delulutopia.core.network.model.DeletionPreview
import com.delulutopia.core.network.model.HomeSession
import com.delulutopia.core.network.model.PushDeviceRemoveBody
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Who is signed in on this device (FS-A2, FS-A13). */
sealed interface SessionState {
    data object Unknown : SessionState

    data object SignedOut : SessionState

    /** The start check could not reach the server; the start screen offers Try again. */
    data object Unreachable : SessionState

    data class SignedIn(val session: HomeSession) : SessionState

    /**
     * Doc "Delululand — Data Deletion" G1–G4: the account or universe is gone and the device is wiped.
     * [reason]: `account` (I deleted my data), `universe` (I deleted the universe), `deleted` (my data
     * was deleted on another device), `closed` (the creator deleted the universe). "Done" leads to the start.
     */
    data class Gone(val reason: String, val brand: String?) : SessionState
}

@Singleton
class SessionRepository @Inject constructor(
    private val api: DelulutopiaApi,
    private val caller: ApiCaller,
    private val pushDevice: PushDevice,
    private val wipe: DeviceWipe,
    events: SessionEvents,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        // FS-A13: a 401 NO_SESSION anywhere returns the app to the start.
        scope.launch { events.sessionEnded.collect { if (_state.value !is SessionState.Gone) _state.value = SessionState.SignedOut } }
        // F5: the account or universe was deleted elsewhere; the next call here hears it.
        scope.launch {
            events.gone.collect { code ->
                if (_state.value is SessionState.Gone) return@collect
                goodbye(if (code == SessionEvents.GONE_UNIVERSE) "closed" else "deleted")
            }
        }
    }

    private val brand: String? get() = (_state.value as? SessionState.SignedIn)?.session?.universe?.name

    private suspend fun goodbye(reason: String) {
        val name = brand
        runCatching { wipe.wipe() }
        pushDevice.token = null
        _state.value = SessionState.Gone(reason, name)
    }

    /** The confirm sheet's counts (§5.1 GET /api/me/deletion-preview). */
    suspend fun deletionPreview(): DeletionPreview = caller.call { api.deletionPreview() }

    /** DL-1: the second citizen deletes their own data; throws [ApiException] on a wrong PIN etc. */
    suspend fun deleteMe(pin: String) {
        caller.call { api.deleteMe(DeleteMeBody(pin, PLATFORM)) }
        goodbye("account")
    }

    /** DL-2: the creator deletes the whole universe, typing its [name] to confirm. */
    suspend fun deleteUniverse(pin: String, name: String) {
        caller.call { api.deleteUniverse(DeleteUniverseBody(pin, name, PLATFORM)) }
        goodbye("universe")
    }

    /** "Done" on the goodbye screen. */
    fun leaveGoodbye() {
        if (_state.value is SessionState.Gone) _state.value = SessionState.SignedOut
    }

    /** FS-A2: a session cookie that the API still accepts opens Home straight away. */
    suspend fun refresh() {
        _state.value = try {
            SessionState.SignedIn(caller.call { api.homeSession() })
        } catch (e: ApiException) {
            when {
                _state.value is SessionState.Gone -> _state.value
                e.status == 401 -> SessionState.SignedOut
                _state.value is SessionState.SignedIn -> _state.value
                else -> SessionState.Unreachable
            }
        }
    }

    /** FS-A11: ends this device's session only; the entry pass stays, so the cards come back. */
    suspend fun logout() {
        // B3: this phone stops getting the citizen's pushes; the session is still valid for this call.
        pushDevice.token?.let { token ->
            runCatching { caller.call { api.unregisterPushDevice(PushDeviceRemoveBody(PushDevice.PLATFORM, token)) } }
        }
        runCatching { caller.call { api.logout() } }
        _state.value = SessionState.SignedOut
    }

    private companion object {
        /** The deletion ledger's platform column (§5.4). */
        const val PLATFORM = "android"
    }
}
