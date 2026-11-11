package com.delulutopia.core.ui.media

import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.delulutopia.core.ui.httpClient

/**
 * One voice note or video plays at a time, everywhere in the app (FS-R9; web media/player.ts):
 * starting one stops the other. Media streams through the app's client so it carries the session.
 */
object OnePlayer {
    private var player: ExoPlayer? = null

    /** The url playing or paused mid-way, or null. */
    var url by mutableStateOf<String?>(null)
        private set
    var playing by mutableStateOf(false)
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
                        }
                    }
                },
            )
            player = p
        }

    /** Play [target] from where it was (or from the start), stopping anything else. */
    fun play(context: Context, target: String) {
        val p = player(context)
        if (url != target) {
            p.setMediaItem(MediaItem.fromUri(target))
            p.prepare()
            url = target
            positionMs = 0
        }
        p.play()
    }

    fun pause() {
        player?.pause()
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
        player?.stop()
        url = null
        playing = false
        positionMs = 0
    }

    /** Called every frame-ish by a visible player to move its scrub bar. */
    fun tick() {
        player?.let {
            positionMs = it.currentPosition
            if (it.duration > 0) durationMs = it.duration
        }
    }
}
