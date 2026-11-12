package com.delulutopia.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.delulutopia.core.data.Backend
import com.delulutopia.core.data.DevicePrefs
import com.delulutopia.core.data.HomeRepository
import com.delulutopia.core.data.LetterActions
import com.delulutopia.core.data.Moods
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.ApiException
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.Uploader
import com.delulutopia.core.network.model.Board
import com.delulutopia.core.network.model.BoardCommentBody
import com.delulutopia.core.network.model.Mood
import com.delulutopia.core.network.model.MoodBody
import com.delulutopia.core.network.model.MoodEditBody
import com.delulutopia.core.network.model.ReactionBody
import com.delulutopia.core.network.model.VersionBody
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/** S2 Home (web home/HomeScreen.tsx, board): the shared Home state and every write Home makes. */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: HomeRepository,
    private val backend: Backend,
    private val prefs: DevicePrefs,
    private val letters: LetterActions,
) : ViewModel(),
    HomeActions {
    val state: StateFlow<HomeRepository.State> = repo.state
    override val boardMini: StateFlow<Boolean> = prefs.boardMini

    private var lastRefetch = 0L

    /** A row crossed a deadline here: show the new state now, then let the server confirm it. */
    override fun flipped() {
        val now = System.currentTimeMillis()
        if (now - lastRefetch > 2_000) {
            lastRefetch = now
            repo.refreshSoon()
        }
    }

    override fun setBoardMini(mini: Boolean) = prefs.setBoardMini(mini)

    // Moods (S3, custom moods CM1–CM7)
    override fun refreshMoods() {
        viewModelScope.launch { repo.loadMoods() }
    }

    override suspend fun setMood(key: String, catalogue: List<Mood>): Boolean = try {
        val r = backend.run { setMood(MoodBody(key)) }
        val m = catalogue.firstOrNull { it.key == key }
        repo.patch { it.copy(me = it.me.copy(mood = r.mood, moodSetAt = r.moodSetAt, moodEmoji = m?.emoji, moodLabel = m?.label)) }
        repo.refreshSoon()
        true
    } catch (_: ApiException) {
        false
    }

    /** Saves a new or edited mood; the error text on failure (a refusal's own sentence, else generic). */
    override suspend fun saveMood(editing: Mood?, emoji: String, label: String): Result<Mood> = try {
        val l = Moods.normalise(label)
        val m = backend.run { if (editing != null) editMood(editing.key, MoodEditBody(emoji, l)) else addMood(MoodEditBody(emoji, l)) }
        repo.loadMoods()
        if (editing != null) repo.refreshSoon()
        Result.success(m)
    } catch (e: ApiException) {
        Result.failure(Exception(moodError(e)))
    }

    /** Null on success (a mood already gone counts); else the error text. */
    override suspend fun deleteMood(key: String): String? = try {
        backend.run { deleteMood(key) }
        repo.loadMoods()
        repo.refreshSoon()
        null
    } catch (e: ApiException) {
        if (e.status == 404) {
            repo.loadMoods()
            null
        } else {
            moodError(e)
        }
    }

    private fun moodError(e: ApiException) = if (e.status in 400..499 && !e.code.startsWith("HTTP_")) e.message else t("home.notSavedTryAgain")

    // Letters

    override suspend fun cancel(id: Long): LetterActions.CancelResult = letters.cancel(id)

    // Board (doc "Delululand — Board")

    private val tellingSeen = mutableSetOf<Int>()

    /** B12, Q5: tell the server once that this version was on screen. */
    override fun boardSeen(version: Int) {
        if (!tellingSeen.add(version)) return
        viewModelScope.launch {
            try {
                backend.run { boardSeen(VersionBody(version)) }
            } catch (_: ApiException) {
                tellingSeen.remove(version) // try again on the next load
            }
        }
    }

    override suspend fun postBoard(png: ByteArray, baseVersion: Int, edit: Boolean): HomeActions.PostResult {
        Uploader.precheck(Uploader.Kind.Doodle, png.size.toLong())?.let { return HomeActions.PostResult.Failed(it) }
        return try {
            val text = "text/plain".toMediaType()
            val view = backend.run {
                postBoard(
                    baseVersion.toString().toRequestBody(text),
                    if (edit) "true".toRequestBody(text) else null,
                    MultipartBody.Part.createFormData("file", "doodle.png", png.toRequestBody("image/png".toMediaType())),
                )
            }
            repo.patch { it.copy(board = view, boardNew = false) }
            repo.refreshSoon()
            HomeActions.PostResult.Posted
        } catch (e: ApiException) {
            val current = if (e.code == "BOARD_CHANGED") backend.extra(e, "board", Board.serializer()) else null
            if (current != null) {
                repo.patch { it.copy(board = current) }
                HomeActions.PostResult.Conflict(current)
            } else {
                HomeActions.PostResult.Failed(if (e.isOffline) t("board.postingFailed") else e.message)
            }
        }
    }

    /** §R: one hand-drawn mark on the friend's doodle; null removes it. Returns a notice, or null. */
    override suspend fun react(board: Board, key: String?, friend: String): String? {
        if (key == board.reaction) return null
        repo.patch { h -> h.copy(board = h.board?.copy(reaction = key)) }
        return try {
            val view = backend.run { if (key != null) reactBoard(ReactionBody(board.version, key)) else unreactBoard(board.version) }
            repo.patch { it.copy(board = view) }
            null
        } catch (e: ApiException) {
            val current = if (e.code == "BOARD_CHANGED") backend.extra(e, "board", Board.serializer()) else null
            if (current != null) {
                repo.patch { it.copy(board = current) }
                t("board.friendJustPosted", "friend" to friend)
            } else {
                repo.patch { it.copy(board = board) }
                if (e.isOffline) t("board.reactionNotSaved") else e.message
            }
        }
    }

    /** §C: the friend's one sticky note. Returns a notice on failure, or null. */
    override suspend fun comment(board: Board, text: String, friend: String): String? = boardWrite(friend, t("board.commentNotPosted")) {
        commentBoard(BoardCommentBody(board.version, text))
    }

    override suspend fun uncomment(board: Board, friend: String): String? = boardWrite(friend, t("board.commentNotDeleted")) {
        uncommentBoard(board.version)
    }

    private suspend fun boardWrite(friend: String, fallback: String, call: suspend DelulutopiaApi.() -> Board): String? = try {
        val view = backend.run(call)
        repo.patch { it.copy(board = view) }
        null
    } catch (e: ApiException) {
        val current = if (e.code == "BOARD_CHANGED") backend.extra(e, "board", Board.serializer()) else null
        when {
            current != null -> {
                repo.patch { it.copy(board = current) }
                t("board.friendJustPosted", "friend" to friend)
            }
            e.code == "COMMENT_EXISTS" -> {
                repo.refreshSoon()
                e.message
            }
            e.isOffline -> fallback
            else -> e.message
        }
    }
}
