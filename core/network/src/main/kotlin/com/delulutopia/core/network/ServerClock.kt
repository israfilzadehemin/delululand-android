package com.delulutopia.core.network

import javax.inject.Inject
import javax.inject.Singleton

/**
 * The API holds the only clock (Technical Design §3.1). Every response carries `X-Server-Time`;
 * `offset = serverTime − deviceTime` (median of the last five) lets countdowns show the right mm:ss
 * on a phone whose clock is minutes off.
 */
@Singleton
class ServerClock @Inject constructor() {
    private val samples = ArrayDeque<Long>()

    @Volatile
    var offsetMs: Long = 0
        private set

    @Synchronized
    fun record(serverEpochMs: Long, deviceEpochMs: Long = System.currentTimeMillis()) {
        samples.addLast(serverEpochMs - deviceEpochMs)
        while (samples.size > 5) samples.removeFirst()
        val sorted = samples.sorted()
        offsetMs = sorted[sorted.size / 2]
    }

    fun nowMs(): Long = System.currentTimeMillis() + offsetMs
}
