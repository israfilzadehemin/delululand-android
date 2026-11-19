package com.delulutopia.core.data

/**
 * Doc "Delululand — Data Deletion" DD9: after a deletion (here or on another device) this phone keeps
 * nothing of the citizen or the universe — cookies, push token, fingerprint keys, cached media,
 * notifications, device-only state. Language and theme stay. Implemented by the app.
 */
fun interface DeviceWipe {
    suspend fun wipe()
}
