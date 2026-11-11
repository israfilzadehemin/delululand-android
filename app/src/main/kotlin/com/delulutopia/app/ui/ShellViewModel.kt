package com.delulutopia.app.ui

import androidx.lifecycle.ViewModel
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.network.LiveUpdates
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/**
 * Everything behind the PIN shares Home (badges, draft dots, the universe's name) and one live
 * stream while the app is in the foreground (web AuthedLayout).
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val home: HomeRepository,
    private val live: LiveUpdates,
) : ViewModel() {
    val state: StateFlow<HomeRepository.State> = home.state

    init {
        home.start()
    }

    fun foreground() {
        live.start()
        home.refreshSoon()
    }

    fun background() = live.stop()

    override fun onCleared() {
        live.stop()
        home.stop()
    }
}
