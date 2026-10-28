package com.virtualalertslider.app.domain.usecase

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.AlertMode
import kotlinx.coroutines.flow.Flow

class ObserveAlertModeUseCase(private val repository: AlertSliderRepository) {
    operator fun invoke(): Flow<AlertMode> {
        return repository.observeAlertMode()
    }
}
