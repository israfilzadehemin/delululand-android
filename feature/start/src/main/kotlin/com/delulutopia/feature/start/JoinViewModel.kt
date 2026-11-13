package com.delulutopia.feature.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.network.ApiException
import com.delulutopia.feature.start.PinViewModel.Problem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Join (doc "Delululand — Universe" §2.3, J1–J2): needs the universe's entry pass. The username is
 * checked at once against the citizen already there, and again by the server; a universe that filled
 * up meanwhile sends the person back to its cards.
 */
@HiltViewModel
class JoinViewModel @Inject constructor(
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val name: String = "",
        val taken: List<String> = emptyList(),
        val step: Int = 1,
        val citizen: Citizen = Citizen(),
        val serverError: String? = null,
        val pin: WizardPin = WizardPin(),
        val leave: Boolean = false,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val view = account.entry()
                _state.update {
                    if (!view.canJoin) it.copy(leave = true) else it.copy(loaded = true, name = view.name, taken = view.users.map { u -> u.nickname })
                }
            } catch (e: ApiException) {
                _state.update { it.copy(leave = e.code == "ENTRY_REQUIRED" || !e.isOffline, loaded = true) }
            }
        }
    }

    fun onCitizen(c: Citizen) = _state.update { it.copy(citizen = c, serverError = null) }

    fun next() = _state.update { it.copy(step = 2) }

    fun back(): Boolean {
        if (_state.value.step == 2) {
            _state.update { it.copy(step = 1, pin = WizardPin()) }
            return true
        }
        return false
    }

    fun onPinDigits(d: String) = _state.update { it.copy(pin = it.pin.type(d)) }

    fun onPinComplete(entered: String) {
        val (next, pin) = _state.value.pin.complete(entered)
        _state.update { it.copy(pin = next) }
        if (pin != null) join(pin)
    }

    private fun join(pin: String) {
        val s = _state.value
        viewModelScope.launch {
            try {
                account.join(s.citizen.username, s.citizen.timezone, pin)
                account.uploadPhoto(s.citizen.photo)
                session.refresh()
            } catch (e: ApiException) {
                _state.update {
                    when {
                        e.isOffline -> it.copy(pin = it.pin.restart(Problem.Offline))
                        e.code == "UNIVERSE_FULL" || e.code == "ENTRY_REQUIRED" -> it.copy(leave = true)
                        e.code in setOf("USERNAME_TAKEN", "USERNAME_INVALID", "TIMEZONE_INVALID") ->
                            it.copy(step = 1, serverError = e.message, pin = WizardPin())
                        else -> it.copy(pin = it.pin.restart(Problem.Text(e.message)))
                    }
                }
            }
        }
    }
}
