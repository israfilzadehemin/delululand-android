package com.delulutopia.core.data

import com.delulutopia.core.network.ApiException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cancel ("Take it back?", FS-S5, FS-S6, C1) from Home or a letter: the letter moves into the draft;
 * after the window the server refuses with WINDOW_CLOSED.
 */
@Singleton
class LetterActions @Inject constructor(private val backend: Backend, private val home: HomeRepository) {
    enum class CancelResult { Done, TooLate, Failed }

    suspend fun cancel(id: Long): CancelResult = try {
        backend.run { cancelLetter(id) }
        home.load()
        CancelResult.Done
    } catch (e: ApiException) {
        home.refreshSoon()
        if (e.code == "WINDOW_CLOSED") CancelResult.TooLate else CancelResult.Failed
    }

    companion object {
        const val TOO_LATE = "Too late: it's on its way."
        const val NOT_CANCELLED = "Not cancelled. Try again."
    }
}
