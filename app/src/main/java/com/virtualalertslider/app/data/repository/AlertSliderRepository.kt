package com.virtualalertslider.app.data.repository

import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface AlertSliderRepository {
    fun observeAlertMode(): Flow<AlertMode>
    fun getCurrentAlertMode(): AlertMode
    suspend fun setAlertMode(mode: AlertMode): Result<Unit>
    fun observeSettings(): Flow<SliderSettings>
    suspend fun saveDefaultMode(mode: AlertMode)
    suspend fun saveHapticFeedbackEnabled(enabled: Boolean)
    suspend fun saveThemePreference(themePreference: ThemePreference)
    fun isNotificationPolicyAccessGranted(): Boolean
}
