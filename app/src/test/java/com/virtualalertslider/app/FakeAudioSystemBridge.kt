package com.virtualalertslider.app

import android.app.NotificationManager
import android.media.AudioManager
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.system.AudioSystemBridge

class FakeAudioSystemBridge(
    var dndGranted: Boolean = true,
    var writeSecureSettingsGranted: Boolean = true,
    initialMode: AlertMode = AlertMode.RING
) : AudioSystemBridge {

    private var _ringerMode: Int = when (initialMode) {
        AlertMode.RING -> AudioManager.RINGER_MODE_NORMAL
        AlertMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
        AlertMode.SILENT -> AudioManager.RINGER_MODE_SILENT
    }

    private var _interruptionFilter: Int = when (initialMode) {
        AlertMode.RING, AlertMode.VIBRATE -> NotificationManager.INTERRUPTION_FILTER_ALL
        AlertMode.SILENT -> NotificationManager.INTERRUPTION_FILTER_ALARMS
    }

    var streamRingMuted: Boolean = initialMode != AlertMode.RING
    var streamNotificationMuted: Boolean = initialMode != AlertMode.RING
    var ringVolume: Int = if (initialMode == AlertMode.RING) 7 else 0
    var notifVolume: Int = if (initialMode == AlertMode.RING) 7 else 0
    val globalSettings = mutableMapOf<String, Int>()
    var totalWriteOperations: Int = 0

    override fun getRingerMode(): Int = _ringerMode

    override fun setRingerMode(mode: Int) {
        totalWriteOperations++
        // On real Android (AudioService.java isChangeToOrFromSilent):
        // Any change to or from RINGER_MODE_SILENT requires notification policy access
        val isChangeToOrFromSilent = (_ringerMode == AudioManager.RINGER_MODE_SILENT && mode != AudioManager.RINGER_MODE_SILENT) ||
                (mode == AudioManager.RINGER_MODE_SILENT && _ringerMode != AudioManager.RINGER_MODE_SILENT)

        val isDndActive = _interruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL

        if (!dndGranted && (isChangeToOrFromSilent || isDndActive)) {
            throw SecurityException("Not allowed to change ringer mode to or from silent / DND without notification policy access")
        }
        _ringerMode = mode
    }

    override fun getInterruptionFilter(): Int = _interruptionFilter

    override fun setInterruptionFilter(filter: Int) {
        totalWriteOperations++
        if (!dndGranted && filter != _interruptionFilter) {
            throw SecurityException("Do Not Disturb Access (Notification Policy) is required to set interruption filter.")
        }
        _interruptionFilter = filter
    }

    override fun isNotificationPolicyAccessGranted(): Boolean = dndGranted

    override fun getStreamMaxVolume(streamType: Int): Int = 10

    override fun adjustStreamVolume(streamType: Int, direction: Int, flags: Int) {
        totalWriteOperations++
        if (direction == AudioManager.ADJUST_UNMUTE) {
            if (streamType == AudioManager.STREAM_RING) streamRingMuted = false
            if (streamType == AudioManager.STREAM_NOTIFICATION) streamNotificationMuted = false
        } else if (direction == AudioManager.ADJUST_MUTE) {
            if (streamType == AudioManager.STREAM_RING) streamRingMuted = true
            if (streamType == AudioManager.STREAM_NOTIFICATION) streamNotificationMuted = true
        }
    }

    override fun setStreamVolume(streamType: Int, index: Int, flags: Int) {
        totalWriteOperations++
        if (streamType == AudioManager.STREAM_RING) ringVolume = index
        if (streamType == AudioManager.STREAM_NOTIFICATION) notifVolume = index
    }

    override fun isStreamMute(streamType: Int): Boolean =
        when (streamType) {
            AudioManager.STREAM_RING -> streamRingMuted
            AudioManager.STREAM_NOTIFICATION -> streamNotificationMuted
            else -> false
        }

    override fun putGlobalSetting(key: String, value: Int) {
        totalWriteOperations++
        if (!writeSecureSettingsGranted) {
            throw SecurityException("Permission denial: writing to Settings.Global requires WRITE_SECURE_SETTINGS")
        }
        globalSettings[key] = value
    }
}
