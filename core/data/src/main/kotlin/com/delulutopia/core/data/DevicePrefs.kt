package com.delulutopia.core.data

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Choices kept on this device only, as the web keeps them per browser in localStorage: the board
 * fold, the Letters layout, falling leaves, recent reaction emojis, the notification ask.
 */
@Singleton
class DevicePrefs @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("device", Context.MODE_PRIVATE)

    private fun flag(key: String, default: Boolean): MutableStateFlow<Boolean> = MutableStateFlow(prefs.getBoolean(key, default))

    private val _boardMini = flag(BOARD_MINI, false)

    /** Board §9: the Home tile folded to a strip. */
    val boardMini: StateFlow<Boolean> = _boardMini.asStateFlow()

    fun setBoardMini(mini: Boolean) = put(BOARD_MINI, mini, _boardMini)

    private val _lettersList = flag(LETTERS_LIST, false)

    /** Letters: the compact list instead of the tile grid. */
    val lettersList: StateFlow<Boolean> = _lettersList.asStateFlow()

    fun setLettersList(list: Boolean) = put(LETTERS_LIST, list, _lettersList)

    private val _leaves = flag(LEAVES, true)

    /** Falling leaves over Home and the lists (Appearance switch). */
    val leaves: StateFlow<Boolean> = _leaves.asStateFlow()

    fun setLeaves(on: Boolean) = put(LEAVES, on, _leaves)

    private val _recentEmojis = MutableStateFlow(prefs.getString(RECENT_EMOJIS, null)?.split('\u0001')?.filter { it.isNotEmpty() } ?: emptyList())

    /** Reactions: the emojis picked last, newest first (web reactions/recents.ts). */
    val recentEmojis: StateFlow<List<String>> = _recentEmojis.asStateFlow()

    fun pushRecentEmoji(emoji: String, keep: Int = 8) {
        val next = (listOf(emoji) + _recentEmojis.value.filter { it != emoji }).take(keep)
        _recentEmojis.value = next
        prefs.edit().putString(RECENT_EMOJIS, next.joinToString("\u0001")).apply()
    }

    /** Whether the notification ask after a PIN entry was answered on this device. */
    var notifyAsked: Boolean
        get() = prefs.getBoolean(NOTIFY_ASKED, false)
        set(value) = prefs.edit().putBoolean(NOTIFY_ASKED, value).apply()

    private fun put(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        flow.value = value
        prefs.edit().putBoolean(key, value).apply()
    }

    private companion object {
        const val BOARD_MINI = "boardMini"
        const val LETTERS_LIST = "lettersList"
        const val LEAVES = "leaves"
        const val RECENT_EMOJIS = "recentEmojis"
        const val NOTIFY_ASKED = "notifyAsked"
    }
}
