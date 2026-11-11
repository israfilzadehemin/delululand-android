package com.delulutopia.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.app.settings.DeviceSettingsRepository
import com.delulutopia.core.data.Backend
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
    private val backend: Backend,
) : ViewModel() {

    /** The TEST line (Technical Design §3.5): shown on every screen while the API runs shortened timers. */
    var testBanner by mutableStateOf<String?>(null)
        private set

    val uiState: StateFlow<MainUiState> = combine(settings.themeMode, session.state) { theme, s ->
        if (s == SessionState.Unknown) MainUiState.Loading else MainUiState.Ready(theme, s)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState.Loading)

    init {
        refreshSession()
        viewModelScope.launch {
            runCatching { backend.run { config() } }.getOrNull()?.takeIf { it.testTiming }?.let { c ->
                testBanner = "TEST · DELAY ${durationWords(c.delay)} · WINDOW ${durationWords(c.window)}" +
                    (c.discussionDelay?.let { " · DISCUSSIONS ${durationWords(it)}" } ?: "")
            }
        }
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

/** "PT2M30S" → "2:30" (web formatDuration). */
fun durationWords(iso: String): String {
    val ms = runCatching { java.time.Duration.parse(iso).toMillis() }.getOrNull() ?: return iso
    val total = Math.round(ms / 1000.0)
    return "${total / 60}:${(total % 60).toString().padStart(2, '0')}"
}

sealed interface MainUiState {
    data object Loading : MainUiState

    data class Ready(val themeMode: ThemeMode, val session: SessionState) : MainUiState
}
