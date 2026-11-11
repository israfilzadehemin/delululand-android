package com.delulutopia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.app.settings.DeviceSettingsRepository
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.data.SessionState
import com.delulutopia.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-wide state: the theme and the session, both read before the first frame so the splash never
 * hands over to the wrong screen (FS-A2: a valid session opens Home straight away).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settings: DeviceSettingsRepository,
    private val session: SessionRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = combine(settings.themeMode, session.state) { theme, s ->
        if (s == SessionState.Unknown) MainUiState.Loading else MainUiState.Ready(theme, s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    init {
        refreshSession()
    }

    fun refreshSession() {
        viewModelScope.launch { session.refresh() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }

    fun logout() {
        viewModelScope.launch { session.logout() }
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val themeMode: ThemeMode, val session: SessionState) : MainUiState
}
