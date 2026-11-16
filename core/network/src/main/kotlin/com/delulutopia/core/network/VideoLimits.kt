package com.delulutopia.core.network

/**
 * The universe's video limits (doc "Delululand — Subscription Tiers" ST4, CM-6; web
 * src/media/limits.ts): Free 15 s / 100 MB, Premium 60 s / 400 MB. HomeRepository sets them from
 * Home's plan; until then (or from an API before V24) the old 15 s / 100 MB hold.
 */
object VideoLimits {
    @Volatile var seconds: Int = 15
        private set

    @Volatile var mb: Int = 100
        private set

    val maxMs: Long get() = seconds * 1000L
    val maxBytes: Long get() = mb * 1024L * 1024L

    fun set(seconds: Int, mb: Int) {
        this.seconds = seconds
        this.mb = mb
    }
}
