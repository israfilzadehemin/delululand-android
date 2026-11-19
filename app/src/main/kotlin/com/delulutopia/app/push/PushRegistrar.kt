package com.delulutopia.app.push

import android.content.Context
import com.delulutopia.core.data.PushDevice
import com.delulutopia.core.data.SessionRepository
import com.delulutopia.core.data.SessionState
import com.delulutopia.core.network.ApiCaller
import com.delulutopia.core.network.DelulutopiaApi
import com.delulutopia.core.network.model.PushDeviceBody
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Background push (Android Build B3): tells the API which FCM token this install has, for the
 * citizen signed in on it. Registered on every sign-in and app start and when FCM rotates the token
 * (idempotent on the server); removed by the session before log-out ([SessionRepository.logout]).
 *
 * A build without `google-services.json` has no Firebase app: push stays off and the in-app notices
 * of [com.delulutopia.app.notify.Arrivals] are the only ones.
 */
@Singleton
class PushRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val session: SessionRepository,
    private val api: DelulutopiaApi,
    private val caller: ApiCaller,
    private val device: PushDevice,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val available: Boolean get() = FirebaseApp.getApps(context).isNotEmpty()

    /** Whether a citizen is signed in here; a push that lands on a signed-out phone is dropped. */
    val signedIn: Boolean get() = prefs.contains(OWNER)

    fun start() {
        scope.launch {
            session.state.collectLatest { state ->
                when (state) {
                    is SessionState.SignedIn -> {
                        prefs.edit().putString(OWNER, state.session.me.id.toString()).apply()
                        if (available) currentToken()?.let { register(it) }
                    }
                    SessionState.SignedOut -> prefs.edit().remove(OWNER).apply()
                    else -> Unit
                }
            }
        }
    }

    fun onNewToken(token: String) {
        device.token = token
        if (signedIn) scope.launch { register(token) }
    }

    private suspend fun currentToken(): String? = device.token ?: runCatching { FirebaseMessaging.getInstance().token.await() }
        .getOrNull()
        ?.also { device.token = it }

    private suspend fun register(token: String) {
        runCatching { caller.call { api.registerPushDevice(PushDeviceBody(PushDevice.PLATFORM, token, context.packageName)) } }
    }

    private companion object {
        const val PREFS = "push"
        const val OWNER = "owner"
    }
}

/** A Play services task as a suspend call (no extra coroutines artifact for one call). */
private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { cont ->
    addOnCompleteListener { task -> cont.resume(if (task.isSuccessful) task.result else null) }
}
