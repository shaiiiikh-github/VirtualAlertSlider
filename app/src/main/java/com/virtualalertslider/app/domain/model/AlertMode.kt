package com.virtualalertslider.app.domain.model

import android.media.AudioManager
import android.app.NotificationManager

enum class AlertMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val ringerMode: Int,
    val interruptionFilter: Int,
    val positionIndex: Int // 0: RING (Top), 1: VIBRATE (Middle), 2: SILENT (Bottom)
) {
    RING(
        id = "RING",
        title = "Ring",
        subtitle = "Calls & notifications will ring",
        ringerMode = AudioManager.RINGER_MODE_NORMAL,
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
        positionIndex = 0
    ),
    VIBRATE(
        id = "VIBRATE",
        title = "Vibrate",
        subtitle = "Calls & notifications will vibrate only",
        ringerMode = AudioManager.RINGER_MODE_VIBRATE,
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_ALL,
        positionIndex = 1
    ),
    SILENT(
        id = "SILENT",
        title = "Silent",
        subtitle = "Muted according to Do Not Disturb policy",
        ringerMode = AudioManager.RINGER_MODE_SILENT,
        interruptionFilter = NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        positionIndex = 2
    );

    fun next(): AlertMode = when (this) {
        RING -> VIBRATE
        VIBRATE -> SILENT
        SILENT -> RING
    }

    companion object {
        fun fromRingerMode(ringerMode: Int, interruptionFilter: Int = NotificationManager.INTERRUPTION_FILTER_ALL): AlertMode {
            return when {
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_NONE ||
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_PRIORITY ||
                interruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALARMS ||
                ringerMode == AudioManager.RINGER_MODE_SILENT -> SILENT
                ringerMode == AudioManager.RINGER_MODE_VIBRATE -> VIBRATE
                else -> RING
            }
        }

        fun fromPositionIndex(index: Int): AlertMode {
            return entries.find { it.positionIndex == index } ?: RING
        }

        fun fromId(id: String?): AlertMode {
            return entries.find { it.id.equals(id, ignoreCase = true) } ?: RING
        }
    }
}
