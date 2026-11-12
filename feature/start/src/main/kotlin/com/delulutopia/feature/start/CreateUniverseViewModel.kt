package com.delulutopia.feature.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.i18n.I18n
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.CreateUniverseRequest
import com.delulutopia.feature.start.PinViewModel.Problem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Create a universe (doc "Delululand — Universe" §2.2, C1–C5): invite → name and welcome → citizen →
 * PIN; the sixth confirm digit creates everything at once, then the key is shown once. Nothing is
 * stored before step 4, so leaving earlier keeps the invite unused.
 */
@HiltViewModel
class CreateUniverseViewModel @Inject constructor(
    private val account: AccountRepository,
    private val session: SessionRepository,
) : ViewModel() {

    data class UiState(
        val step: Int = 1,
        val invite: String = "",
        val inviteProblem: InviteProblem? = null,
        val name: String = "",
        val nameServerError: String? = null,
        val welcome: String = Rules.welcomeTemplate(""),
        val welcomeEdited: Boolean = false,
        val preview: Boolean = false,
        val citizen: Citizen = Citizen(),
        val citizenError: String? = null,
        val pin: WizardPin = WizardPin(),
        val busy: Boolean = false,
        val created: Created? = null,
    )

    data class Created(val code: String, val name: String)

    enum class InviteProblem { Wrong, Offline }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun onInvite(raw: String) = _state.update { it.copy(invite = groupKey(raw), inviteProblem = null) }

    fun checkInvite() {
        val s = _state.value
        if (s.busy || s.invite.isBlank()) return
        _state.update { it.copy(busy = true, inviteProblem = null) }
        viewModelScope.launch {
            try {
                account.checkInvite(s.invite)
                _state.update { it.copy(busy = false, step = 2) }
            } catch (e: ApiException) {
                _state.update { it.copy(busy = false, inviteProblem = if (e.isOffline) InviteProblem.Offline else InviteProblem.Wrong) }
            }
        }
    }

    /** C2: while untouched, the welcome follows the name as it is typed. */
    fun onName(raw: String) = _state.update {
        val name = raw.filterNot(Char::isWhitespace).take(24)
        it.copy(name = name, nameServerError = null, welcome = if (it.welcomeEdited) it.welcome else Rules.welcomeTemplate(name))
    }

    fun onWelcome(text: String) = _state.update { it.copy(welcome = text, welcomeEdited = true) }

    fun resetWelcome() = _state.update { it.copy(welcome = Rules.welcomeTemplate(it.name), welcomeEdited = false) }

    fun setPreview(open: Boolean) = _state.update { it.copy(preview = open) }

    fun onCitizen(c: Citizen) = _state.update { it.copy(citizen = c, citizenError = null) }

    fun next() = _state.update { it.copy(step = (it.step + 1).coerceAtMost(4)) }

    /** ← on steps 2–4 goes one step back; on step 1 the screen leaves. */
    fun back(): Boolean {
        val s = _state.value
        if (s.step in 2..4) {
            _state.update { it.copy(step = it.step - 1, pin = WizardPin()) }
            return true
        }
        return false
    }

    fun onPinDigits(d: String) = _state.update { it.copy(pin = it.pin.type(d)) }

    fun onPinComplete(entered: String) {
        val (next, pin) = _state.value.pin.complete(entered)
        _state.update { it.copy(pin = next) }
        if (pin != null) create(pin)
    }

    private fun create(pin: String) {
        val s = _state.value
        viewModelScope.launch {
            try {
                val r = account.createUniverse(
                    CreateUniverseRequest(s.invite, s.name.trim(), s.welcome, s.citizen.username, s.citizen.timezone, pin, I18n.lang.tag),
                )
                account.uploadPhoto(s.citizen.photo)
                _state.update { it.copy(step = 5, created = Created(r.code, r.universe.name), pin = WizardPin()) }
            } catch (e: ApiException) {
                // C4: a refusal sends the person to the step it belongs to.
                _state.update {
                    when {
                        e.isOffline -> it.copy(pin = it.pin.restart(Problem.Offline))
                        e.code == "INVITE_INVALID" || e.code == "INVITE_THROTTLED" ->
                            it.copy(step = 1, inviteProblem = InviteProblem.Wrong, pin = WizardPin())
                        e.code == "UNIVERSE_NAME_INVALID" || e.code == "WELCOME_INVALID" ->
                            it.copy(step = 2, nameServerError = e.message.takeIf { e.code == "UNIVERSE_NAME_INVALID" }, pin = WizardPin())
                        e.code == "USERNAME_INVALID" || e.code == "TIMEZONE_INVALID" ->
                            it.copy(step = 3, citizenError = e.message, pin = WizardPin())
                        else -> it.copy(pin = it.pin.restart(Problem.Text(e.message)))
                    }
                }
            }
        }
    }

    /** After the key: the new session opens Home. */
    fun finish() {
        viewModelScope.launch { session.refresh() }
    }
}
