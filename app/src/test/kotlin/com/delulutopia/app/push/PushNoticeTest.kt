package com.delulutopia.app.push

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.delulutopia.app.notify.Notices
import com.delulutopia.app.notify.PendingOpen
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** B3: what an FCM data message becomes on the phone. */
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class PushNoticeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val manager = context.getSystemService(NotificationManager::class.java)

    private val letter = mapOf(
        "event" to "letter.arrived",
        "letterId" to "6f1c",
        "title" to "A letter from Leyla has arrived",
        "body" to "Tap to read",
        "url" to "/letters/6f1c",
        "tag" to "letter-6f1c",
    )

    @Test
    fun `a push carries its event, words, path and tag`() {
        val n = PushNotice.of(letter)!!
        assertThat(n).isEqualTo(PushNotice("letter.arrived", "A letter from Leyla has arrived", "Tap to read", "/letters/6f1c", "letter-6f1c"))
    }

    @Test
    fun `foreign or broken data is not shown, and only the app's own paths open`() {
        assertThat(PushNotice.of(mapOf("title" to "x"))).isNull()
        assertThat(PushNotice.of(mapOf("event" to "letter.arrived"))).isNull()
        assertThat(PushNotice.of(letter + ("url" to "https://evil.example/x"))!!.path).isEqualTo("/home")
        assertThat(PushNotice.of(letter + ("url" to "//evil.example/x"))!!.path).isEqualTo("/home")
        assertThat(PushNotice.of(letter - "tag")!!.tag).isEqualTo("letter.arrived")
    }

    @Test
    fun `every event has its channel in its section, and the old three are gone`() {
        val legacy = android.app.NotificationChannel("letters", "Letters", NotificationManager.IMPORTANCE_DEFAULT)
        manager.createNotificationChannel(legacy)
        Notices.rememberFriend(context, "Leyla")
        Notices.ensureChannels(context)

        val ids = manager.notificationChannels.map { it.id }
        assertThat(ids).containsExactlyElementsIn(Notices.EVENTS)
        assertThat(Notices.EVENTS).hasSize(12)
        assertThat(manager.notificationChannelGroups.map { it.id }).containsExactly("letters", "discussions", "emails", "board", "home")
        assertThat(manager.getNotificationChannel("mood.changed").group).isEqualTo("home")
        assertThat(manager.getNotificationChannel("mood.changed").name.toString()).contains("Leyla")
        assertThat(manager.getNotificationChannel("letter.read").lockscreenVisibility).isEqualTo(android.app.Notification.VISIBILITY_PRIVATE)
    }

    @Test
    fun `an unknown event falls back to its section's channel`() {
        assertThat(Notices.channelOf("email.reaction")).isEqualTo("email.reaction")
        assertThat(Notices.channelOf("letter.someday")).isEqualTo("letter.arrived")
        assertThat(Notices.channelOf("board.later")).isEqualTo("board.doodle")
        assertThat(Notices.channelOf("weird")).isEqualTo("letter.arrived")
    }

    @Test
    fun `a push is one notification per tag, in its channel, opening its path`() {
        shadowOf(context as Application).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        val n = PushNotice.of(letter)!!
        Notices.post(context, n.tag, n.event, n.title, n.body, n.path)
        // The in-app notice for the same letter replaces it rather than adding a second one.
        Notices.post(context, n.tag, n.event, "Leyla's letter has arrived", n.body, n.path)

        val shown = shadowOf(manager).allNotifications
        assertThat(shown).hasSize(1)
        val posted = shown.single()
        assertThat(posted.channelId).isEqualTo("letter.arrived")
        assertThat(shadowOf(posted).contentTitle.toString()).isEqualTo("Leyla's letter has arrived")
        val intent = shadowOf(posted.contentIntent).savedIntent
        assertThat(intent.getStringExtra(PendingOpen.EXTRA)).isEqualTo("/letters/6f1c")
    }
}
