package com.delulutopia.app.notify

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.delulutopia.app.MainActivity
import com.delulutopia.app.R
import com.delulutopia.core.i18n.t
import com.delulutopia.feature.you.eventLabel

/**
 * The system notifications of both paths: FCM pushes (B3) and the in-app arrival notices. One
 * channel per settings event (design §3 "one notification channel per event", 12), grouped by the
 * five sections of Settings › Notifications and named in the app's language; lock-screen
 * visibility is private (P6). A notice's tag is the push's tag ("letter-<id>", "email-thread-<id>"),
 * so a push and the in-app notice for the same item are one notification.
 */
object Notices {
    /** Settings sections and their events, as the API's NotifyCatalog. */
    val SECTIONS: Map<String, List<String>> = linkedMapOf(
        "letters" to listOf("letter.arrived", "letter.incoming", "letter.read"),
        "discussions" to listOf("discussion.arrived", "discussion.comment", "discussion.reaction"),
        "emails" to listOf("email.new", "email.reaction"),
        "board" to listOf("board.doodle", "board.reaction", "board.note"),
        "home" to listOf("mood.changed"),
    )

    val EVENTS: List<String> = SECTIONS.values.flatten()

    /** The three channels of the in-app notices before B3; replaced by the per-event ones. */
    internal val LEGACY_CHANNELS = listOf("letters", "discussions", "emails")

    private const val PREFS = "notices"
    private const val FRIEND = "friend"

    /** Two channel names carry the friend's name ("Leyla read your letter"); kept from the last Home. */
    fun rememberFriend(context: Context, name: String?) {
        if (name.isNullOrBlank()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(FRIEND, null) != name) prefs.edit().putString(FRIEND, name).apply()
    }

    /** Creates or renames the channels (a language change renames them on the next notice). */
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val friend = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(FRIEND, null).orEmpty()
        SECTIONS.forEach { (section, events) ->
            manager.createNotificationChannelGroup(NotificationChannelGroup(section, t("notifySection.$section")))
            events.forEach { key ->
                val channel = NotificationChannel(key, eventLabel(key, friend).first.trim(), NotificationManager.IMPORTANCE_DEFAULT)
                channel.group = section
                channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                manager.createNotificationChannel(channel)
            }
        }
        LEGACY_CHANNELS.forEach { manager.deleteNotificationChannel(it) }
    }

    /** The channel for a push's event; an event this build does not know yet goes to its section's first. */
    fun channelOf(event: String): String = when {
        event in EVENTS -> event
        else -> SECTIONS.entries.firstOrNull { (_, events) -> events.first().substringBefore('.') == event.substringBefore('.') }
            ?.value?.first() ?: EVENTS.first()
    }

    /** Posts (or replaces, by [tag]) one notification; a tap opens [path] once Home is up. */
    @SuppressLint("MissingPermission")
    fun post(context: Context, tag: String, event: String, title: String, body: String, path: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(PendingOpen.EXTRA, path)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val tap = PendingIntent.getActivity(context, tag.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(context, channelOf(event))
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()
        runCatching { manager.notify(tag, 0, notification) }
    }
}
