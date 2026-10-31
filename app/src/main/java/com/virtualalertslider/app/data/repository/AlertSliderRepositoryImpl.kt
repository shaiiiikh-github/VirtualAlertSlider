package com.virtualalertslider.app.data.repository

import android.content.Context
import com.virtualalertslider.app.data.preferences.PreferencesManager
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import com.virtualalertslider.app.system.AudioModeManager
import com.virtualalertslider.app.system.NotificationPolicyManager
import com.virtualalertslider.app.system.RingerModeReceiver
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AlertSliderRepositoryImpl(
    private val context: Context,
    private val audioModeManager: AudioModeManager = AudioModeManager(context),
    private val notificationPolicyManager: NotificationPolicyManager = NotificationPolicyManager(context),
    private val preferencesManager: PreferencesManager = PreferencesManager(context),
    private val ringerModeReceiver: RingerModeReceiver = RingerModeReceiver(context)
) : AlertSliderRepository {

    @Volatile
    private var lastKnownMode: AlertMode? = null
    private val setModeMutex = Mutex()

    override fun observeAlertMode(): Flow<AlertMode> {
        return combine(
            ringerModeReceiver.observeRingerModeChanges(),
            preferencesManager.rawSelectedModeFlow,
            preferencesManager.settingsFlow
        ) { systemMode, userSelectedMode, settings ->
            val effectiveMode = if (settings.bypassStuckSlider && userSelectedMode != null) {
                userSelectedMode
            } else {
                systemMode
            }
            lastKnownMode = effectiveMode
            effectiveMode
        }
    }

    override fun getCurrentAlertMode(): AlertMode {
        return lastKnownMode ?: audioModeManager.getCurrentAlertMode().also {
            lastKnownMode = it
        }
    }

    override suspend fun setAlertMode(mode: AlertMode): Result<Unit> {
        return setModeMutex.withLock {
            // Fix 5: Return early on no-op transitions (target == current REAL system state)
            if (audioModeManager.isSystemInAlertMode(mode)) {
                return@withLock Result.success(Unit)
            }

            withContext(NonCancellable) {
                val result = audioModeManager.setAlertMode(mode)
                if (result.isSuccess) {
                    preferencesManager.saveSelectedMode(mode)
                    lastKnownMode = mode
                }
                result
            }
        }
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

    override suspend fun saveBypassStuckSlider(enabled: Boolean) {
        preferencesManager.saveBypassStuckSlider(enabled)
    }

    override fun isNotificationPolicyAccessGranted(): Boolean {
        return notificationPolicyManager.isNotificationPolicyAccessGranted()
    }
}
