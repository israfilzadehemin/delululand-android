package com.delulutopia.core.ui.media

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.PowerManager

/**
 * Raise-to-listen (doc "Delululand — Earpiece Playback" EP1–EP4): while a voice note is about to
 * play or plays, the proximity sensor decides the route. Near the ear → earpiece, and the panel goes
 * off (proximity wake lock, so the cheek can't touch anything); away → loudspeaker.
 *
 * No permission prompt: `WAKE_LOCK` and `MODIFY_AUDIO_SETTINGS` are install-time permissions.
 */
internal object Earpiece {
    /** The proximity sensor reads "near" (the phone is at the ear). */
    var near: Boolean = false
        private set

    /** A session (listener + wake lock) is running. */
    var active: Boolean = false
        private set

    private var listener: SensorEventListener? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var onChange: ((Boolean) -> Unit)? = null

    private fun audio(context: Context) = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private fun proximity(context: Context): Sensor? =
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private val HEADSETS = setOf(
        AudioDeviceInfo.TYPE_WIRED_HEADSET,
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
        AudioDeviceInfo.TYPE_USB_HEADSET,
        // AudioDeviceInfo.TYPE_BLE_HEADSET (API 31), by value so minSdk 24 compiles.
        26,
    )

    /**
     * EP3/EP11: only on a phone with an earpiece and a proximity sensor, and only while no headset is
     * connected (the headset wins: no delay, no sensor).
     */
    fun usable(context: Context): Boolean {
        if (proximity(context) == null) return false
        val outputs = audio(context).getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        if (outputs.none { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }) return false
        return outputs.none { it.type in HEADSETS }
    }

    /** Starts listening; [change] gets every near/far flip. Idempotent. */
    fun start(context: Context, change: (Boolean) -> Unit) {
        onChange = change
        if (active) return
        val sensor = proximity(context) ?: return
        val manager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val now = event.values[0] < minOf(sensor.maximumRange, NEAR_CM)
                if (now != near) {
                    near = now
                    onChange?.invoke(now)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        manager.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        listener = l
        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (power.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
            wakeLock = power.newWakeLock(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK, "delulutopia:earpiece").apply {
                setReferenceCounted(false)
                acquire(MAX_HOLD_MS)
            }
        }
        active = true
    }

    /** Ends the session: sensor off, panel back, route back to the loudspeaker. */
    fun stop(context: Context) {
        if (!active) return
        (context.getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(listener)
        listener = null
        onChange = null
        wakeLock?.let { if (it.isHeld) it.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY) }
        wakeLock = null
        near = false
        active = false
        route(context, ear = false)
    }

    /** EP1: puts the phone's audio on the earpiece ([ear]) or back to normal. */
    fun route(context: Context, ear: Boolean) {
        val am = audio(context)
        if (ear) {
            am.mode = AudioManager.MODE_IN_COMMUNICATION
            if (Build.VERSION.SDK_INT >= 31) {
                am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE }
                    ?.let { am.setCommunicationDevice(it) }
            } else {
                @Suppress("DEPRECATION")
                am.isSpeakerphoneOn = false
            }
        } else if (am.mode == AudioManager.MODE_IN_COMMUNICATION) {
            if (Build.VERSION.SDK_INT >= 31) am.clearCommunicationDevice()
            am.mode = AudioManager.MODE_NORMAL
        }
    }

    private const val NEAR_CM = 5f

    /** Safety net: the lock frees itself even if a session is never closed (5:00 note at 0.5× + chain). */
    private const val MAX_HOLD_MS = 30 * 60 * 1000L
}
