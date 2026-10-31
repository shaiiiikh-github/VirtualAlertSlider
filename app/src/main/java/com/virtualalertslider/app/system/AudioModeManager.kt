package com.virtualalertslider.app.system

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import com.virtualalertslider.app.domain.model.AlertMode

interface AudioSystemBridge {
    fun getRingerMode(): Int
    fun setRingerMode(mode: Int)
    fun getInterruptionFilter(): Int
    fun setInterruptionFilter(filter: Int)
    fun isNotificationPolicyAccessGranted(): Boolean
    fun getStreamMaxVolume(streamType: Int): Int
    fun adjustStreamVolume(streamType: Int, direction: Int, flags: Int)
    fun setStreamVolume(streamType: Int, index: Int, flags: Int)
    fun isStreamMute(streamType: Int): Boolean
    fun putGlobalSetting(key: String, value: Int)
}

class DefaultAudioSystemBridge(private val context: Context) : AudioSystemBridge {
    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
    private val notificationPolicyManager by lazy {
        NotificationPolicyManager(context)
    }

    override fun getRingerMode(): Int = audioManager.ringerMode
    override fun setRingerMode(mode: Int) {
        audioManager.ringerMode = mode
    }
    override fun getInterruptionFilter(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.currentInterruptionFilter
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
    override fun setInterruptionFilter(filter: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.setInterruptionFilter(filter)
        }
    }
    override fun isNotificationPolicyAccessGranted(): Boolean =
        notificationPolicyManager.isNotificationPolicyAccessGranted()

    override fun getStreamMaxVolume(streamType: Int): Int =
        audioManager.getStreamMaxVolume(streamType)
    override fun adjustStreamVolume(streamType: Int, direction: Int, flags: Int) =
        audioManager.adjustStreamVolume(streamType, direction, flags)
    override fun setStreamVolume(streamType: Int, index: Int, flags: Int) =
        audioManager.setStreamVolume(streamType, index, flags)
    override fun isStreamMute(streamType: Int): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            audioManager.isStreamMute(streamType)
        } else {
            false
        }
    override fun putGlobalSetting(key: String, value: Int) {
        Settings.Global.putInt(context.contentResolver, key, value)
    }
}

class AudioModeManager(
    private val context: Context,
    private val bridge: AudioSystemBridge = DefaultAudioSystemBridge(context)
) {

    /**
     * Reads current hardware/system ringer mode and DND interruption filter.
     */
    fun getCurrentAlertMode(): AlertMode {
        val ringerMode = bridge.getRingerMode()
        val interruptionFilter = bridge.getInterruptionFilter()

        val mode = AlertMode.fromRingerMode(ringerMode, interruptionFilter)
        if (mode == AlertMode.RING && (bridge.isStreamMute(AudioManager.STREAM_RING) || bridge.isStreamMute(AudioManager.STREAM_NOTIFICATION))) {
            return AlertMode.SILENT
        }
        return mode
    }

    /**
     * Checks if the REAL hardware/system state matches the given alert mode.
     * Evaluates ringer mode, interruption filter, and stream mute states.
     */
    fun isSystemInAlertMode(mode: AlertMode): Boolean {
        val ringerMode = bridge.getRingerMode()
        val interruptionFilter = bridge.getInterruptionFilter()
        val baseMode = AlertMode.fromRingerMode(ringerMode, interruptionFilter)
        if (baseMode != mode) return false

        // For RING, verify that streams are not muted
        if (mode == AlertMode.RING) {
            if (bridge.isStreamMute(AudioManager.STREAM_RING) || bridge.isStreamMute(AudioManager.STREAM_NOTIFICATION)) {
                return false
            }
        }
        return true
    }

    /**
     * Applies the requested AlertMode directly to system audio settings.
     * Overcomes stuck/broken physical alert sliders by forcing volume streams,
     * resetting DND filters, and updating OEM system settings where permitted.
     */
    fun setAlertMode(mode: AlertMode): Result<Unit> {
        return try {
            val hasDndAccess = bridge.isNotificationPolicyAccessGranted()

            when (mode) {
                AlertMode.RING -> {
                    // 1. Attempt to override OnePlus proprietary hardware slider global settings
                    tryOverrideOnePlusHardwareSlider(3)

                    // 2. Clear Do Not Disturb so calls and notifications are permitted
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            if (bridge.getInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) {
                                bridge.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                            }
                        } catch (e: SecurityException) {
                            return Result.failure(e)
                        } catch (e: Exception) {
                            logError("AudioModeManager", "Failed to clear DND filter", e)
                            return Result.failure(e)
                        }
                    }

                    // 3. Set standard Android ringer mode
                    try {
                        bridge.setRingerMode(AudioManager.RINGER_MODE_NORMAL)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to set normal ringer mode", e)
                        return Result.failure(e)
                    }

                    // 4. Force unmute and restore volume streams (The hardware bypass trick for stuck slider)
                    try {
                        val maxRing = bridge.getStreamMaxVolume(AudioManager.STREAM_RING)
                        val targetRing = (maxRing * 0.75f).toInt().coerceAtLeast(1)
                        val maxNotif = bridge.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
                        val targetNotif = (maxNotif * 0.75f).toInt().coerceAtLeast(1)

                        bridge.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)
                        bridge.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_UNMUTE, 0)
                        bridge.setStreamVolume(AudioManager.STREAM_RING, targetRing, 0)
                        bridge.setStreamVolume(AudioManager.STREAM_NOTIFICATION, targetNotif, 0)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to adjust volume streams", e)
                    }

                    Result.success(Unit)
                }

                AlertMode.VIBRATE -> {
                    // 1. Attempt to override OnePlus hardware slider to Vibrate
                    tryOverrideOnePlusHardwareSlider(2)

                    // 2. Clear DND
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        try {
                            if (bridge.getInterruptionFilter() != NotificationManager.INTERRUPTION_FILTER_ALL) {
                                bridge.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                            }
                        } catch (e: SecurityException) {
                            return Result.failure(e)
                        } catch (e: Exception) {
                            logError("AudioModeManager", "Failed to clear DND filter", e)
                            return Result.failure(e)
                        }
                    }

                    // 3. Set ringer mode to Vibrate
                    try {
                        bridge.setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to set vibrate ringer mode", e)
                        return Result.failure(e)
                    }

                    // 4. Silence ring volume stream so it vibrates only
                    try {
                        bridge.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to mute ring stream for vibrate", e)
                    }

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
                            bridge.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS)
                        } catch (e: SecurityException) {
                            return Result.failure(e)
                        } catch (e: Exception) {
                            logError("AudioModeManager", "Failed to set DND alarms filter", e)
                            return Result.failure(e)
                        }
                    }

                    try {
                        bridge.setRingerMode(AudioManager.RINGER_MODE_SILENT)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to set silent ringer mode", e)
                        return Result.failure(e)
                    }

                    // Mute streams
                    try {
                        bridge.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0)
                        bridge.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, AudioManager.ADJUST_MUTE, 0)
                    } catch (e: SecurityException) {
                        return Result.failure(e)
                    } catch (e: Exception) {
                        logError("AudioModeManager", "Failed to mute streams for silent", e)
                    }

                    Result.success(Unit)
                }
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            logError("AudioModeManager", "Unexpected error setting alert mode", e)
            Result.failure(e)
        }
    }

    private fun logError(tag: String, message: String, throwable: Throwable? = null) {
        try {
            android.util.Log.e(tag, message, throwable)
        } catch (_: Throwable) {
            System.err.println("[$tag] $message ${throwable?.message ?: ""}")
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
                bridge.putGlobalSetting(key, modeValue)
            } catch (_: Exception) {
                // If secure settings permission is not yet granted, fallback gracefully
            }
        }
    }
}
