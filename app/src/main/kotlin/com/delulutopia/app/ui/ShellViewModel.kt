package com.delulutopia.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.app.notify.Arrivals
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DevicePrefs
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LettersRepository
import com.delulutopia.core.network.LiveUpdates
import com.delulutopia.core.network.model.EmojiBody
import com.delulutopia.core.ui.reactions.ReactionsApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch

/**
 * Everything behind the PIN shares Home (badges, draft dots, the universe's name) and one live
 * stream while the app is in the foreground (web AuthedLayout).
 */
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val home: HomeRepository,
    private val live: LiveUpdates,
    private val letters: LettersRepository,
    prefs: DevicePrefs,
    backend: Backend,
    arrivals: Arrivals,
) : ViewModel() {
    val state: StateFlow<HomeRepository.State> = home.state

    /** Settings › Appearance › Seasonal effects (L6). */
    val leaves: StateFlow<Boolean> = prefs.leaves

    /** Reactions on discussions, comments and emails (doc "Delululand — Reactions"). */
    val reactions = ReactionsApi { target, id, emoji, add ->
        backend.run { if (add) react(target, id, EmojiBody(emoji)) else unreact(target, id, EmojiBody(emoji)) }.reactions
    }

    init {
        viewModelScope.launch { home.state.mapNotNull { it.home }.collect { arrivals.announce(it) } }
    }

    /**
     * The shell is shown for a signed-in citizen. This view model lives as long as the activity, so
     * a sign-out followed by another sign-in (the other card, or the same one) must not keep the
     * previous session's Home, letters or stream: [leave] forgets them, [enter] starts afresh.
     */
    fun enter() = home.start()

    fun leave() {
        live.stop()
        home.stop()
        letters.clear()
    }

    fun foreground() {
        live.start()
        home.refreshSoon()
    }

    fun background() = live.stop()

    override fun onCleared() {
        live.stop()
        home.stop()
        letters.clear()
    }
}
