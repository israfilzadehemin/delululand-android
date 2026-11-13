package com.delulutopia.feature.start

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.AccountRepository
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.Entered
import com.delulutopia.core.network.model.EntryView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * S0 without a session (doc "Delululand — Universe" §2.1): the landing with the key field while this
 * device has no entry pass (UV-1, UV-2), else that universe's cards plus "Join" while it has room and
 * "Use a different key" (UV-4). A correct key first shows the welcome (UV-3).
 */
@HiltViewModel
class EntryViewModel @Inject constructor(
    private val account: AccountRepository,
) : ViewModel() {

    sealed interface Content {
        data object Loading : Content

        data class Landing(val key: String = "", val wrong: Boolean = false, val offline: Boolean = false, val busy: Boolean = false) : Content

        data class Cards(val view: EntryView) : Content

        data object Unreachable : Content
    }

    data class UiState(val content: Content = Content.Loading, val welcome: Entered? = null)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val content = try {
                Content.Cards(account.entry())
            } catch (e: ApiException) {
                if (e.code == "ENTRY_REQUIRED") Content.Landing() else Content.Unreachable
            }
            _state.update { it.copy(content = content) }
        }
    }

    fun onKeyChange(raw: String) {
        _state.update { s ->
            val landing = s.content as? Content.Landing ?: return@update s
            s.copy(content = landing.copy(key = groupKey(raw), wrong = false, offline = false))
        }
    }

    fun submitKey() {
        val landing = _state.value.content as? Content.Landing ?: return
        if (landing.busy || landing.key.isBlank()) return
        _state.update { it.copy(content = landing.copy(busy = true, wrong = false, offline = false)) }
        viewModelScope.launch {
            try {
                val entered = account.enter(landing.key)
                _state.update { it.copy(welcome = entered.takeIf { e -> e.welcome.isNotBlank() }) }
                load()
            } catch (e: ApiException) {
                val wrong = e.code == "WRONG_CODE" || e.code == "CODE_THROTTLED"
                _state.update { it.copy(content = landing.copy(busy = false, wrong = wrong, offline = !wrong)) }
            }
        }
    }

    fun dismissWelcome() = _state.update { it.copy(welcome = null) }

    /** "Use a different key": this device forgets its universe. */
    fun forget() {
        _state.update { it.copy(content = Content.Loading) }
        viewModelScope.launch {
            account.forgetEntry()
            load()
        }
    }
}

/** Keys and invites are typed and shown as XXXX-XXXX-XXXX; the server ignores case, spaces and hyphens. */
internal fun groupKey(value: String): String =
    value.filter { it.isLetterOrDigit() && it.code < 128 }.uppercase().take(12).chunked(4).joinToString("-")
