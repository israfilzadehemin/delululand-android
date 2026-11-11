package com.delulutopia.feature.start

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.UserCard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S1 PIN (FS-A3–A8): enter, or on first login create then confirm (the compare is here; the API
 * receives the PIN once). Nothing typed outlives an attempt. Success refreshes the session, which
 * takes the app to Home.
 */
@HiltViewModel
class PinViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {

    enum class Mode { Enter, Create, Confirm }

    /** Error kinds; the screen picks the words. */
    sealed interface Problem {
        data object Wrong : Problem

        data object Differ : Problem

        data object Offline : Problem

        data class Text(val text: String) : Problem
    }

    data class UiState(
        val user: UserCard? = null,
        val index: Int = 0,
        val universe: String = "",
        val mode: Mode = Mode.Enter,
        val digits: String = "",
        val problem: Problem? = null,
        val busy: Boolean = false,
        /** No entry pass any more, or not a citizen here: back to the start. */
        val leave: Boolean = false,
    )

    private val userId = savedState.toRoute<PinDestination>().userId
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()
    private var first = ""

    init {
        viewModelScope.launch {
            try {
                val view = account.entry()
                val i = view.users.indexOfFirst { it.id == userId }
                if (i < 0) {
                    _state.update { it.copy(leave = true) }
                } else {
                    val u = view.users[i]
                    _state.update { it.copy(user = u, index = i, universe = view.name, mode = if (u.hasPin) Mode.Enter else Mode.Create) }
                }
            } catch (e: ApiException) {
                _state.update { if (e.code == "ENTRY_REQUIRED") it.copy(leave = true) else it.copy(problem = Problem.Offline) }
            }
        }
    }

    fun onDigits(d: String) = _state.update { it.copy(digits = d, problem = if (d.isNotEmpty()) null else it.problem) }

    fun onComplete(pin: String) {
        val s = _state.value
        if (s.busy) return
        when (s.mode) {
            Mode.Create -> {
                first = pin
                _state.update { it.copy(mode = Mode.Confirm, digits = "", problem = null) }
            }
            Mode.Confirm -> if (pin != first) {
                first = ""
                fail(Problem.Differ, Mode.Create)
            } else {
                submit { account.createPin(userId, pin) }
            }
            Mode.Enter -> submit { account.login(userId, pin) }
        }
    }

    private fun submit(call: suspend () -> Unit) {
        val mode = _state.value.mode
        _state.update { it.copy(busy = true, problem = null) }
        viewModelScope.launch {
            try {
                call()
                session.refresh()
            } catch (e: ApiException) {
                when {
                    e.code == "ENTRY_REQUIRED" -> _state.update { it.copy(leave = true) }
                    e.code == "NO_PIN" -> fail(null, Mode.Create)
                    e.code == "PIN_EXISTS" -> fail(null, Mode.Enter) // Set a moment ago on another device.
                    mode == Mode.Enter && e.code in setOf("WRONG_PIN", "PIN_THROTTLED", "BAD_PIN") -> fail(Problem.Wrong, Mode.Enter)
                    e.isOffline -> fail(Problem.Offline, if (mode == Mode.Enter) Mode.Enter else Mode.Create)
                    else -> fail(Problem.Text(e.message), if (mode == Mode.Enter) Mode.Enter else Mode.Create)
                }
            }
        }
    }

    private fun fail(problem: Problem?, mode: Mode) {
        if (mode == Mode.Create) first = ""
        _state.update { it.copy(problem = problem, mode = mode, digits = "", busy = false) }
    }
}
