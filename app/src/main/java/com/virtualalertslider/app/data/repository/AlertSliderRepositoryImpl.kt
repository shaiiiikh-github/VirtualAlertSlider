package com.virtualalertslider.app.data.repository

import android.content.Context
import com.virtualalertslider.app.data.preferences.PreferencesManager
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import com.virtualalertslider.app.system.AudioModeManager
import com.virtualalertslider.app.system.NotificationPolicyManager
import com.virtualalertslider.app.system.RingerModeReceiver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AlertSliderRepositoryImpl(
    private val context: Context,
    private val audioModeManager: AudioModeManager = AudioModeManager(context),
    private val notificationPolicyManager: NotificationPolicyManager = NotificationPolicyManager(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val ringerModeReceiver: RingerModeReceiver = RingerModeReceiver(context)
) : AlertSliderRepository {

    override fun observeAlertMode(): Flow<AlertMode> {
        return ringerModeReceiver.observeRingerModeChanges()
    }

    override fun getCurrentAlertMode(): AlertMode {
        return audioModeManager.getCurrentAlertMode()
    }

    override suspend fun setAlertMode(mode: AlertMode): Result<Unit> {
        val result = audioModeManager.setAlertMode(mode)
        if (result.isSuccess) {
            preferencesManager.saveSelectedMode(mode)
        }
        return result
    }

    override fun observeSettings(): Flow<SliderSettings> {
        return preferencesManager.settingsFlow.map { settings ->
            settings.copy(
                isNotificationPolicyAccessGranted = notificationPolicyManager.isNotificationPolicyAccessGranted()
            )
        }
    }

    override suspend fun saveDefaultMode(mode: AlertMode) {
        preferencesManager.saveDefaultMode(mode)
    }

    override suspend fun saveHapticFeedbackEnabled(enabled: Boolean) {
        preferencesManager.saveHapticFeedbackEnabled(enabled)
    }

    override suspend fun saveThemePreference(themePreference: ThemePreference) {
        preferencesManager.saveThemePreference(themePreference)
    }

    override fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationPolicyManager.isNotificationPolicyAccessGranted()
    }
}
