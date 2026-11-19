package com.delulutopia.core.data

import javax.inject.Inject
import javax.inject.Singleton

/**
 * This install's push token (Android Build B3), set by the app's registrar once Firebase hands it
 * over. The session removes it from the server before it logs out, so a signed-out phone stops
 * getting the citizen's notifications.
 */
@Singleton
class PushDevice @Inject constructor() {
    @Volatile var token: String? = null

    companion object {
        const val PLATFORM = "fcm"
    }
}
