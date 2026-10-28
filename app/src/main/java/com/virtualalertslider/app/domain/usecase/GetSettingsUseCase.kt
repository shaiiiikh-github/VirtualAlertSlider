package com.virtualalertslider.app.domain.usecase

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.SliderSettings
import kotlinx.coroutines.flow.Flow

class GetSettingsUseCase(private val repository: AlertSliderRepository) {
    operator fun invoke(): Flow<SliderSettings> {
        return repository.observeSettings()
    }
}
