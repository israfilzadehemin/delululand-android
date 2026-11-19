package com.delulutopia.app.deletion

import android.content.Context
import androidx.core.app.NotificationManagerCompat
import com.delulutopia.core.data.DeviceWipe
import com.delulutopia.core.network.cookie.PersistentCookieJar
import com.delulutopia.core.ui.fingerprint.Fingerprint
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The device half of a deletion (doc "Delululand — Data Deletion" DD9, design DD-G*): cookies (session
 * and entry pass), the FCM token, fingerprint keys, every notification, cached media and the
 * device-only state go. The language ("dl-lang") and the theme (DataStore "device_settings") stay.
 */
@Singleton
class AppDeviceWipe @Inject constructor(
    @ApplicationContext private val context: Context,
    private val cookies: PersistentCookieJar,
) : DeviceWipe {
    override suspend fun wipe() = withContext(Dispatchers.IO) {
        cookies.clear()
        NotificationManagerCompat.from(context).cancelAll()
        Fingerprint.forgetAll(context)
        WIPED_PREFS.forEach { context.getSharedPreferences(it, Context.MODE_PRIVATE).edit().clear().commit() }
        context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
        if (FirebaseApp.getApps(context).isNotEmpty()) runCatching { FirebaseMessaging.getInstance().deleteToken() }
        Unit
    }

    private companion object {
        /** Every SharedPreferences file the app writes except the language. */
        val WIPED_PREFS = listOf("device", "notices", "arrivals", "push", "fingerprint")
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class DeletionModule {
    @Binds abstract fun deviceWipe(impl: AppDeviceWipe): DeviceWipe
}
