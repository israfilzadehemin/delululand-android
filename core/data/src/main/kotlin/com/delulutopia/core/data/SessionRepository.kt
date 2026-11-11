package com.delulutopia.core.data

import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.SessionEvents
import com.delulutopia.core.network.model.HomeSession
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
}

@Singleton
class SessionRepository @Inject constructor(
    private val api: DelulutopiaApi,
    private val caller: ApiCaller,
    events: SessionEvents,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow<SessionState>(SessionState.Unknown)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        // FS-A13: a 401 NO_SESSION anywhere returns the app to the start.
        scope.launch { events.sessionEnded.collect { _state.value = SessionState.SignedOut } }
    }

    /** FS-A2: a session cookie that the API still accepts opens Home straight away. */
    suspend fun refresh() {
        _state.value = try {
            SessionState.SignedIn(caller.call { api.homeSession() })
        } catch (e: ApiException) {
            when {
                e.status == 401 -> SessionState.SignedOut
                _state.value is SessionState.SignedIn -> _state.value
                else -> SessionState.Unreachable
            }
        }
    }

    /** FS-A11: ends this device's session only; the entry pass stays, so the cards come back. */
    suspend fun logout() {
        runCatching { caller.call { api.logout() } }
        _state.value = SessionState.SignedOut
    }
}
