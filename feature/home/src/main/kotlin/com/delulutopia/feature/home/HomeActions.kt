package com.delulutopia.feature.home

import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.network.model.Board
import com.delulutopia.core.network.model.Mood
import kotlinx.coroutines.flow.StateFlow

/** What Home's screens ask for; [HomeViewModel] does it against the API, tests with a fake. */
interface HomeActions {
    val boardMini: StateFlow<Boolean>

    /** Selfies DS1: the selfie board folded. */
    val selfieMini: StateFlow<Boolean>

    fun setSelfieMini(mini: Boolean)

    /** Selfies Q3: once per id, the friend's selfie was shown unlocked. */
    fun selfieSeen(id: String)

    /** Selfies DS4: pins the shot; null when pinned, else the words to show. */
    suspend fun postSelfie(jpeg: ByteArray): String?

    fun flipped()

    fun setBoardMini(mini: Boolean)

    /** The sheet opened: re-read the universe's moods (web refetches `GET /api/moods` on open). */
    fun refreshMoods() {}

    suspend fun setMood(key: String, catalogue: List<Mood>): Boolean

    suspend fun saveMood(editing: Mood?, emoji: String, label: String): Result<Mood>

    suspend fun deleteMood(key: String): String?

    suspend fun cancel(id: String): LetterActions.CancelResult

    fun boardSeen(version: Int)

    suspend fun postBoard(png: ByteArray, baseVersion: Int, edit: Boolean): PostResult

    suspend fun react(board: Board, key: String?, friend: String): String?

    suspend fun comment(board: Board, text: String, friend: String): String?

    suspend fun uncomment(board: Board, friend: String): String?

    sealed interface PostResult {
        data object Posted : PostResult

        data class Conflict(val current: Board) : PostResult

        data class Failed(val message: String) : PostResult

        /** Subscription Tiers BD-2: today's doodles went elsewhere while the pad was open. */
        data class Capped(val renewsAt: String) : PostResult
    }
}
