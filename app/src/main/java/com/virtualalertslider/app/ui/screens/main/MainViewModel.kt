package com.virtualalertslider.app.ui.screens.main

import android.app.Application
import android.content.ComponentName
import android.os.Build
import android.service.quicksettings.TileService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.virtualalertslider.app.VirtualAlertSliderApp
import com.virtualalertslider.app.data.repository.AlertSliderRepositoryImpl
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.usecase.GetSettingsUseCase
import com.virtualalertslider.app.domain.usecase.ObserveAlertModeUseCase
import com.virtualalertslider.app.domain.usecase.SetAlertModeUseCase
import com.virtualalertslider.app.system.NotificationPolicyManager
import com.virtualalertslider.app.tile.VirtualAlertSliderTile
import com.virtualalertslider.app.widget.VirtualAlertSliderWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val currentMode: AlertMode = AlertMode.RING,
    val settings: SliderSettings = SliderSettings(),
    val errorMessage: String? = null,
    val isDndGranted: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VirtualAlertSliderApp.getRepository(application)
    private val observeAlertModeUseCase = ObserveAlertModeUseCase(repository)
    private val setAlertModeUseCase = VirtualAlertSliderApp.getSetAlertModeUseCase(application)
    private val getSettingsUseCase = GetSettingsUseCase(repository)
    private val notificationPolicyManager = NotificationPolicyManager(application)

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private var setModeJob: kotlinx.coroutines.Job? = null

    init {
        // Observe system ringer mode changes
        viewModelScope.launch {
            observeAlertModeUseCase().collect { mode ->
                _uiState.update { currentState ->
                    currentState.copy(currentMode = mode)
                }
            }
        }

        // Observe settings changes
        viewModelScope.launch {
            getSettingsUseCase().collect { settings ->
                _uiState.update { currentState ->
                    currentState.copy(settings = settings)
                }
            }
        }

        refreshDndStatus()
    }

    fun refreshDndStatus() {
        val granted = notificationPolicyManager.isNotificationPolicyAccessGranted()
        _uiState.update { it.copy(isDndGranted = granted) }
    }

    fun selectMode(mode: AlertMode) {
        if (mode == _uiState.value.currentMode) return

        setModeJob?.cancel()
        setModeJob = viewModelScope.launch {
            val result = setAlertModeUseCase(mode)
            if (result.isSuccess) {
                _uiState.update { it.copy(currentMode = mode, errorMessage = null) }
                // Synchronize widget and QS tile
                VirtualAlertSliderWidget.updateAllWidgets(getApplication())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    TileService.requestListeningState(
                        getApplication(),
                        ComponentName(getApplication(), VirtualAlertSliderTile::class.java)
                    )
                }
            } else {
                val exception = result.exceptionOrNull()
                val message = exception?.localizedMessage ?: "Failed to set alert mode"
                _uiState.update { it.copy(errorMessage = message) }
                refreshDndStatus()
            }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun getNotificationPolicyIntent() = notificationPolicyManager.createNotificationPolicyAccessIntent()
}
