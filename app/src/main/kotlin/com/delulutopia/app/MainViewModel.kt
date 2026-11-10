package com.delulutopia.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.app.settings.DeviceSettingsRepository
import com.delulutopia.core.designsystem.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** App-wide state: the theme, read before the first frame so the splash never hands over to the wrong one. */
@HiltViewModel
class MainViewModel @Inject constructor(
    private val settings: DeviceSettingsRepository,
) : ViewModel() {

    val uiState: StateFlow<MainUiState> = settings.themeMode
        .map { MainUiState.Ready(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}

sealed interface MainUiState {
    data object Loading : MainUiState
    data class Ready(val themeMode: ThemeMode) : MainUiState
}
