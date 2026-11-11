package com.delulutopia.feature.letters

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DevicePrefs
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.data.LetterState
import com.delulutopia.core.data.LettersRepository
import com.delulutopia.core.data.Time
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.model.LetterFull
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive

/** One letter as S7 has it: loading, shown, or why not (not arrived yet, gone, unreachable). */
sealed interface LetterUi {
    data object Loading : LetterUi

    data class Shown(val letter: LetterFull) : LetterUi

    data class NotArrived(val arrivesAt: String) : LetterUi

    data object Gone : LetterUi

    data object Unreachable : LetterUi
}

/**
 * S6 Letters and S7 (web letters/LettersScreen.tsx, LetterView.tsx, useLetterRows.ts): the shared
 * rows, the layout toggle, and the letters opened on this screen.
 */
@HiltViewModel
class LettersViewModel @Inject constructor(
    private val repo: LettersRepository,
    private val homeRepo: HomeRepository,
    private val backend: Backend,
    private val prefs: DevicePrefs,
    private val letterActions: LetterActions,
) : ViewModel() {
    val rows: StateFlow<LettersRepository.State> = repo.state
    val home: StateFlow<HomeRepository.State> = homeRepo.state
    val list: StateFlow<Boolean> = prefs.lettersList

    private val _letters = MutableStateFlow<Map<Long, LetterUi>>(emptyMap())
    val letters: StateFlow<Map<Long, LetterUi>> = _letters.asStateFlow()
    private val polls = mutableMapOf<Long, Job>()

    init {
        repo.attach()
        // A letter signal can change any open letter (delivered, read, cancelled).
        viewModelScope.launch {
            backend.live.signals.collect { s -> if (s.name == "letter") _letters.value.keys.forEach { reload(it) } }
        }
    }

    override fun onCleared() = repo.detach()

    fun setList(list: Boolean) = prefs.setLettersList(list)

    fun loadMore() = repo.loadMore()

    /** S7: fetch a letter (the recipient's first fetch at or after arrival is its read mark, T8). */
    fun open(id: Long) {
        if (_letters.value[id] is LetterUi.Shown) return
        _letters.update { it + (id to (it[id] ?: LetterUi.Loading)) }
        reload(id)
    }

    private fun reload(id: Long) {
        viewModelScope.launch {
            val ui = try {
                val letter = backend.run { letter(id) }
                if (letter.direction == "from") {
                    // Opening a received letter marks it read: Home's NEW row and the tab dot follow.
                    homeRepo.refreshSoon()
                    repo.refreshSoon()
                }
                LetterUi.Shown(letter)
            } catch (e: ApiException) {
                when {
                    e.code == "NOT_ARRIVED" -> {
                        val at = (e.extra["arrivesAt"] as? JsonPrimitive)?.content.orEmpty()
                        retryAt(id, at)
                        LetterUi.NotArrived(at)
                    }
                    e.status == 404 -> LetterUi.Gone
                    _letters.value[id] is LetterUi.Shown -> _letters.value[id]!!
                    else -> LetterUi.Unreachable
                }
            }
            _letters.update { it + (id to ui) }
            if (ui is LetterUi.Shown) pollIfWaiting(id, ui.letter)
        }
    }

    /** §3.3: a tap a moment before arrival is retried once at arrivesAt + 300 ms. */
    private fun retryAt(id: Long, iso: String) {
        val wait = (Time.ms(iso) ?: return) + 300 - Time.now()
        if (wait >= 5 * 60_000) return
        viewModelScope.launch {
            delay(wait.coerceAtLeast(0))
            reload(id)
        }
    }

    /** FS-R7: a sent letter not read yet is re-read every 15 s while the stream is down, and at its arrival. */
    private fun pollIfWaiting(id: Long, letter: LetterFull) {
        polls.remove(id)?.cancel()
        if (letter.direction != "to" || letter.state == LetterState.READ) return
        polls[id] = viewModelScope.launch {
            val untilArrival = (Time.ms(letter.arrivesAt) ?: 0) + 500 - Time.now()
            val live = backend.live.live.value
            val wait = when {
                untilArrival > 0 && (live || untilArrival < 15_000) -> untilArrival
                live -> return@launch
                else -> 15_000L
            }
            delay(wait)
            reload(id)
        }
    }

    suspend fun cancel(id: Long): LetterActions.CancelResult {
        val r = letterActions.cancel(id)
        if (r == LetterActions.CancelResult.Done) {
            _letters.update { it - id }
            repo.refreshSoon()
        } else {
            reload(id)
        }
        return r
    }
}
