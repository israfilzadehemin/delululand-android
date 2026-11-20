package com.delulutopia.app.push

import com.delulutopia.app.notify.Notices
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * FCM's entry point (B3). The API sends data-only messages with the web push's fields plus
 * `event`; the app draws them itself, so each lands in its event's channel, in the citizen's
 * language (the server wrote it in the recipient's language), and opens the item on tap.
 */
@AndroidEntryPoint
class PushService : FirebaseMessagingService() {

    @Inject lateinit var registrar: PushRegistrar

    override fun onNewToken(token: String) {
        registrar.onNewToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!registrar.signedIn) return
        // Read Ticks TK2: the email reached this phone, even with the app closed.
        message.data["emailId"]?.takeIf { it.isNotBlank() }?.let(registrar::delivered)
        val notice = PushNotice.of(message.data) ?: return
        Notices.post(applicationContext, notice.tag, notice.event, notice.title, notice.body, notice.path)
    }
}

/** One push as the app shows it; null when the data is not a Delulutopia push. */
data class PushNotice(val event: String, val title: String, val body: String, val path: String, val tag: String) {
    companion object {
        fun of(data: Map<String, String>): PushNotice? {
            val event = data["event"]?.takeIf { it.isNotBlank() } ?: return null
            val title = data["title"]?.takeIf { it.isNotBlank() } ?: return null
            // Only the app's own paths; anything else opens Home.
            val path = data["url"]?.takeIf { it.startsWith("/") && !it.startsWith("//") } ?: "/home"
            return PushNotice(event, title, data["body"].orEmpty(), path, data["tag"]?.takeIf { it.isNotBlank() } ?: event)
        }
    }
}
