package com.virtualalertslider.app.domain.usecase

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.ThemePreference

class SaveSettingsUseCase(private val repository: AlertSliderRepository) {
    suspend fun saveDefaultMode(mode: AlertMode) {
        repository.saveDefaultMode(mode)
    }

    suspend fun saveHapticFeedbackEnabled(enabled: Boolean) {
        repository.saveHapticFeedbackEnabled(enabled)
    }

    suspend fun saveThemePreference(themePreference: ThemePreference) {
        repository.saveThemePreference(themePreference)
    }
}
