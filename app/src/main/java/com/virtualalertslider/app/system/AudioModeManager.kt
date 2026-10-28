package com.virtualalertslider.app.system

import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.os.Build
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
     * Returns Result.success(Unit) or Result.failure(Exception).
     */
    fun setAlertMode(mode: AlertMode): Result<Unit> {
        return try {
            val hasDndAccess = notificationPolicyManager.isNotificationPolicyAccessGranted()

            when (mode) {
                AlertMode.RING -> {
                    if (hasDndAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
                    Result.success(Unit)
                }

                AlertMode.VIBRATE -> {
                    if (hasDndAccess && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) {
                            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
                        }
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    Result.success(Unit)
                }

                AlertMode.SILENT -> {
                    if (!hasDndAccess) {
                        return Result.failure(
                            SecurityException("Do Not Disturb Access (Notification Policy) is required to enable Silent mode.")
                        )
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
                    }
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
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
}
