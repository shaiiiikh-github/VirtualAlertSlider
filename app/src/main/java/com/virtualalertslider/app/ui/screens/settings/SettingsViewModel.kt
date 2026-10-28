package com.virtualalertslider.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualalertslider.app.data.repository.AlertSliderRepositoryImpl
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import com.virtualalertslider.app.domain.usecase.GetSettingsUseCase
import com.virtualalertslider.app.domain.usecase.SaveSettingsUseCase
import com.virtualalertslider.app.system.NotificationPolicyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlertSliderRepositoryImpl(application)
    private val getSettingsUseCase = GetSettingsUseCase(repository)
    private val saveSettingsUseCase = SaveSettingsUseCase(repository)
    private val notificationPolicyManager = NotificationPolicyManager(application)

    private val _settings = MutableStateFlow(SliderSettings())
    val settings: StateFlow<SliderSettings> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _settings.value = settings
            }
        }
    }

    fun refreshDndStatus() {
        val granted = notificationPolicyManager.isNotificationPolicyAccessGranted()
        _settings.update { it.copy(isNotificationPolicyAccessGranted = granted) }
    }

    fun setDefaultMode(mode: AlertMode) {
        viewModelScope.launch {
            saveSettingsUseCase.saveDefaultMode(mode)
        }
    }

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        viewModelScope.launch {
            saveSettingsUseCase.saveHapticFeedbackEnabled(enabled)
        }
    }

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            saveSettingsUseCase.saveThemePreference(theme)
        }
    }

    fun getNotificationPolicyIntent() = notificationPolicyManager.createNotificationPolicyAccessIntent()
}
