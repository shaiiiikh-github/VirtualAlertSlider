package com.virtualalertslider.app.system

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.virtualalertslider.app.domain.model.AlertMode

class AudioModeManager(private val context: Context) {

    private val audioManager: AudioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private val notificationPolicyManager = NotificationPolicyManager(context)

    /**
     * Reads current hardware/system ringer mode and DND interruption filter.
     */
    fun getCurrentAlertMode(): AlertMode {
        val ringerMode = audioManager.ringerMode
        val interruptionFilter = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }

        return AlertMode.fromRingerMode(ringerMode, interruptionFilter)
    }

    /**
     * Applies the requested AlertMode directly to system audio settings.
     * Overcomes stuck/broken physical alert sliders by forcing volume streams,
     * resetting DND filters, and updating OEM system settings where permitted.
     */
    fun setAlertMode(mode: AlertMode): Result<Unit> {
        return try {
            val hasDndAccess = notificationPolicyManager.isNotificationPolicyAccessGranted()

            when (mode) {
                AlertMode.RING -> {
                    // 1. Attempt to override OnePlus proprietary hardware slider global settings
                    tryOverrideOnePlusHardwareSlider(3)

                    // 2. Clear Do Not Disturb so calls and notifications are permitted
                    if (hasDndAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                            }
                        } catch (_: Exception) {}
                    }

                    // 3. Set standard Android ringer mode
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    } catch (_: Exception) {}

                    // 4. Force unmute and restore volume streams (The hardware bypass trick for stuck slider)
                    try {
                        val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
                        val targetRing = (maxRing * 0.75f).toInt().coerceAtLeast(1)
                        val maxNotif = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                        val targetNotif = (maxNotif * 0.75f).toInt().coerceAtLeast(1)

                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                        audioManager.setStreamVolume(AudioManager.STREAM_RING, targetRing, 0)
                        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetNotif, 0)
                    } catch (_: Exception) {}

                    Result.success(Unit)
                }

                AlertMode.VIBRATE -> {
                    // 1. Attempt to override OnePlus hardware slider to Vibrate
                    tryOverrideOnePlusHardwareSlider(2)

                    // 2. Clear DND
                    if (hasDndAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                                notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                            }
                        } catch (_: Exception) {}
                    }

                    // 3. Set ringer mode to Vibrate
                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    } catch (_: Exception) {}

                    // 4. Silence ring volume stream so it vibrates only
                    try {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                    } catch (_: Exception) {}

                    Result.success(Unit)
                }

                AlertMode.SILENT -> {
                    // 1. Attempt to override OnePlus hardware slider to Silent
                    tryOverrideOnePlusHardwareSlider(1)

                    if (!hasDndAccess) {
                        return Result.failure(
                            SecurityException("Do Not Disturb Access (Notification Policy) is required to enable Silent mode. Please grant access in Settings.")
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                        } catch (_: Exception) {}
                    }

                    try {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } catch (_: Exception) {}

                    // Mute streams
                    try {
                        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    } catch (_: Exception) {}

                    Result.success(Unit)
                }
            }
        } catch (e: SecurityException) {
            Result.failure(
                SecurityException("Android permission error: Do Not Disturb policy access needed. (${e.localizedMessage})")
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Attempts to override OnePlus and OEM hardware alert slider state keys.
     * Works seamlessly if WRITE_SECURE_SETTINGS is granted via ADB.
     */
    private fun tryOverrideOnePlusHardwareSlider(modeValue: Int) {
        val keys = listOf("three_Key_mode", "three_key_mode", "alert_slider_mode", "oplus_mode_ringer")
        for (key in keys) {
            try {
                Settings.Global.putInt(context.contentResolver, key, modeValue)
            } catch (_: Exception) {
                // If secure settings permission is not yet granted, fallback gracefully
            }
        }
    }
}
