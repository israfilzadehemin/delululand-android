package com.delulutopia.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.delulutopia.app.MainActivity
import com.delulutopia.app.R
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

    private fun remember(kind: String, ids: List<Long>) {
        val all = (done(kind) + ids.map { it.toString() }).toList().takeLast(200).toSet()
        prefs.edit().putStringSet(kind, all).apply()
    }

    fun announce(home: Home) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val on: (String) -> Boolean = { key -> home.notifyPrefs?.let { key in it.enabled } ?: true }
        val friend = home.friend?.nickname ?: return

        if (on("letter.arrived")) {
            val seen = done(LETTERS)
            val rows = home.from.filter { it.state == "DELIVERED" && it.id.toString() !in seen && (Time.ms(it.arrivesAt) ?: 0) >= since }
            remember(LETTERS, rows.map { it.id })
            rows.forEach {
                post(manager, "letter-${it.id}", LETTERS, t("notify.inTab.letter", "name" to friend), t("notify.inTab.tapToRead"), "/letters/${it.id}")
            }
        }
        if (on("discussion.arrived")) {
            val seen = done(DISCUSSIONS)
            val rows = home.newDiscussions.filter { it.id.toString() !in seen && (Time.ms(it.arrivesAt) ?: 0) >= since }
            remember(DISCUSSIONS, rows.map { it.id })
            rows.forEach {
                post(manager, "discussion-${it.id}", DISCUSSIONS, t("notify.inTab.discussion", "name" to friend), it.title, "/discussions/${it.id}")
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
                    manager,
                    "email-thread-${it.threadId}",
                    EMAILS,
                    t("notify.inTab.email", "name" to friend),
                    t("notify.inTab.tapToRead"),
                    "/emails/${it.threadId}",
                )
            }
        }
    }

    private fun channel(id: String, name: String) {
        val m = context.getSystemService(NotificationManager::class.java) ?: return
        if (android.os.Build.VERSION.SDK_INT >= 26) m.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT))
    }

    @Suppress("MissingPermission")
    private fun post(manager: NotificationManagerCompat, tag: String, kind: String, title: String, body: String, path: String) {
        channel(kind, kind.replaceFirstChar { it.uppercase() })
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(PendingOpen.EXTRA, path)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(context, tag.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(context, kind)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        runCatching { manager.notify(tag, 0, n) }
    }

    private companion object {
        /** One poll interval of slack, so an item that arrived just before the first look still counts. */
        const val GRACE_MS = 60_000L
        const val LETTERS = "letters"
        const val DISCUSSIONS = "discussions"
        const val EMAILS = "emails"
    }
}
