package com.delulutopia.core.ui.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.delulutopia.core.ui.httpClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * One voice note or video plays at a time, everywhere in the app (FS-R9; web media/player.ts):
 * starting one stops the other. Media streams through the app's client so it carries the session.
 *
 * Voice notes from the players ([playVoice]) also follow doc "Delululand — Earpiece Playback":
 * a 0.5 s wait before the start (EP6), the proximity sensor picks earpiece or loudspeaker (EP1),
 * taking the phone off the ear pauses (EP2), and the run's next notes follow after 0.5 s each (EP7–EP9).
 */
object OnePlayer {
    /** EP6/EP9: the wait before a voice note starts, and between the notes of a run. Config-only. */
    const val VOICE_DELAY_MS = 500L

    private var player: ExoPlayer? = null
    private var app: Context? = null
    private val scope = MainScope()
    private var job: Job? = null
    private var queue: List<String> = emptyList()
    private var onEar = false

    private val VOICE_CALL: AudioAttributes = AudioAttributes.Builder()
        .setUsage(C.USAGE_VOICE_COMMUNICATION)
        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
        .build()

    /** The url playing or paused mid-way, or null. */
    var url by mutableStateOf<String?>(null)
        private set
    var playing by mutableStateOf(false)
        private set

    /** EP6: the voice note waiting out its 0.5 s before it starts, or null. */
    var pending by mutableStateOf<String?>(null)
        private set
    var positionMs by mutableLongStateOf(0)
        private set
    var durationMs by mutableLongStateOf(0)
        private set

    @OptIn(UnstableApi::class)
    fun player(context: Context): ExoPlayer = player ?: ExoPlayer.Builder(context.applicationContext)
        .setMediaSourceFactory(DefaultMediaSourceFactory(OkHttpDataSource.Factory(context.httpClient())))
        .build()
        .also { p ->
            app = context.applicationContext
            p.addListener(
                object : Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        playing = isPlaying
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) durationMs = p.duration.coerceAtLeast(0)
                        if (state == Player.STATE_ENDED) {
                            p.seekTo(0)
                            p.pause()
                            positionMs = 0
                            url = null
                            ended()
                        }
                    }
                },
            )
            player = p
        }

    /**
     * Play [target] from where it was (or from the start), stopping anything else, at once and on the
     * loudspeaker (draft pills). [speed] is the voice note speed (doc "Delululand — Voice Note Speed");
     * draft pills play at 1×. Pitch stays 1 (VS6).
     */
    fun play(context: Context, target: String, speed: Float = 1f) {
        cancelRun()
        endSession(context.applicationContext)
        load(context, target, speed)
    }

    /**
     * A voice-note player's play (EP1–EP9): [next] are the run's notes that follow [target]. With an
     * earpiece and a proximity sensor and no headset, the note starts after [VOICE_DELAY_MS] on the
     * route the sensor says at that moment; otherwise it starts at once on the loudspeaker (EP3).
     */
    fun playVoice(context: Context, target: String, next: List<String> = emptyList()) {
        val ctx = context.applicationContext
        app = ctx
        cancelRun()
        queue = next
        if (url != null && url != target) player?.pause()
        if (!Earpiece.usable(ctx)) {
            endSession(ctx)
            start(ctx, target)
            return
        }
        Earpiece.start(ctx, ::proximity)
        later(ctx, target)
    }

    /** VS5: a new speed applies at once, from the current position, to whatever is loaded. */
    fun setSpeed(speed: Float) {
        player?.playbackParameters = PlaybackParameters(speed)
    }

    /** Pauses and ends the run and any earpiece session; the next play waits 0.5 s again (EP2). */
    fun pause() {
        cancelRun()
        player?.pause()
        app?.let { endSession(it) }
    }

    fun seek(context: Context, target: String, ms: Long) {
        val p = player(context)
        if (url != target) {
            p.setMediaItem(MediaItem.fromUri(target))
            p.prepare()
            url = target
        }
        p.seekTo(ms)
        positionMs = ms
    }

    /** Stop whatever plays (a screen with its own player, a recorder starting). */
    fun stop() {
        cancelRun()
        player?.stop()
        url = null
        playing = false
        positionMs = 0
        app?.let { endSession(it) }
    }

    /** Called every frame-ish by a visible player to move its scrub bar. */
    fun tick() {
        player?.let {
            positionMs = it.currentPosition
            if (it.duration > 0) durationMs = it.duration
        }
    }

    /** True while an earpiece session runs (a player pauses it when the app leaves the screen). */
    val earpieceSession: Boolean get() = Earpiece.active

    private fun load(context: Context, target: String, speed: Float) {
        val p = player(context)
        p.playbackParameters = PlaybackParameters(speed)
        if (url != target) {
            p.setMediaItem(MediaItem.fromUri(target))
            p.prepare()
            url = target
            positionMs = 0
        }
        p.play()
    }

    private fun later(ctx: Context, target: String) {
        pending = target
        job = scope.launch {
            delay(VOICE_DELAY_MS)
            pending = null
            start(ctx, target)
        }
    }

    /** EP1: the route at the start is the sensor's word; without a session, the loudspeaker. */
    private fun start(ctx: Context, target: String) {
        ear(ctx, Earpiece.active && Earpiece.near)
        load(ctx, target, VoiceSpeed.speed)
    }

    /** EP7–EP9: the next note of the run after 0.5 s, on the same session; else the session ends. */
    private fun ended() {
        val ctx = app ?: return
        val next = queue.firstOrNull()
        if (next == null) {
            endSession(ctx)
            return
        }
        queue = queue.drop(1)
        later(ctx, next)
    }

    /** EP1: raised mid-note → earpiece in place; EP2: taken off the ear → pause. */
    private fun proximity(near: Boolean) {
        val ctx = app ?: return
        if (near && !onEar && (playing || pending != null)) {
            ear(ctx, true)
        } else if (!near && onEar) {
            pause()
        }
    }

    private fun ear(ctx: Context, on: Boolean) {
        if (onEar == on) return
        onEar = on
        player(ctx).setAudioAttributes(if (on) VOICE_CALL else AudioAttributes.DEFAULT, false)
        Earpiece.route(ctx, on)
    }

    private fun endSession(ctx: Context) {
        if (player != null) ear(ctx, false)
        Earpiece.stop(ctx)
    }

    private fun cancelRun() {
        job?.cancel()
        job = null
        pending = null
        queue = emptyList()
    }
}
