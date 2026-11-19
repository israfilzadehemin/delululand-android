package com.delulutopia.app.notify

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.delulutopia.core.data.Time
import com.delulutopia.core.i18n.t
import com.delulutopia.core.network.model.Home
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow

/** A notification's target ("/letters/12"), handed from the intent to the signed-in shell. */
object PendingOpen {
    val path = MutableStateFlow<String?>(null)
    const val EXTRA = "open"
}

/**
 * The in-app fallback (web notify/inTab.ts; E7, FS-T10, FS-D26, Emails Q23): while the app is
 * running, each letter, discussion and email from the friend that arrives is announced with the
 * push's words, one per item (one per thread for emails), and only those that arrived since the
 * app started watching. The switches in Settings › Notifications decide which kinds.
 */
@Singleton
class Arrivals @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs = context.getSharedPreferences("arrivals", Context.MODE_PRIVATE)
    private val since = Time.now() - GRACE_MS

    private fun done(kind: String): MutableSet<String> = prefs.getStringSet(kind, emptySet())!!.toMutableSet()

    private fun remember(kind: String, ids: List<String>) {
        val all = (done(kind) + ids.map { it.toString() }).toList().takeLast(200).toSet()
        prefs.edit().putStringSet(kind, all).apply()
    }

    fun announce(home: Home) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val on: (String) -> Boolean = { key -> home.notifyPrefs?.let { key in it.enabled } ?: true }
        val friend = home.friend?.nickname ?: return
        Notices.rememberFriend(context, friend)

        if (on("letter.arrived")) {
            val seen = done(LETTERS)
            val rows = home.from.filter { it.state == "DELIVERED" && it.id.toString() !in seen && (Time.ms(it.arrivesAt) ?: 0) >= since }
            remember(LETTERS, rows.map { it.id })
            rows.forEach {
                post("letter-${it.id}", "letter.arrived", t("notify.inTab.letter", "name" to friend), t("notify.inTab.tapToRead"), "/letters/${it.id}")
            }
        }
        if (on("discussion.arrived")) {
            val seen = done(DISCUSSIONS)
            val rows = home.newDiscussions.filter { it.id.toString() !in seen && (Time.ms(it.arrivesAt) ?: 0) >= since }
            remember(DISCUSSIONS, rows.map { it.id })
            rows.forEach {
                post("discussion-${it.id}", "discussion.arrived", t("notify.inTab.discussion", "name" to friend), it.title, "/discussions/${it.id}")
            }
        }
        if (on("email.new")) {
            val seen = done(EMAILS)
            val rows = home.newEmails.filter { it.id.toString() !in seen && (Time.ms(it.sentAt) ?: 0) >= since }
            remember(EMAILS, rows.map { it.id })
            rows.distinctBy {
                it.threadId
            }.forEach {
                post(
                    "email-thread-${it.threadId}",
                    "email.new",
                    t("notify.inTab.email", "name" to friend),
                    t("notify.inTab.tapToRead"),
                    "/emails/${it.threadId}",
                )
            }
        }
    }

    /** Same tag and channel as the push for this item (B3), so the two are one notification. */
    private fun post(tag: String, event: String, title: String, body: String, path: String) = Notices.post(context, tag, event, title, body, path)

    private companion object {
        /** One poll interval of slack, so an item that arrived just before the first look still counts. */
        const val GRACE_MS = 60_000L
        const val LETTERS = "letters"
        const val DISCUSSIONS = "discussions"
        const val EMAILS = "emails"
    }
}
