package com.virtualalertslider.app.domain.usecase

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.AlertMode

class SetAlertModeUseCase(private val repository: AlertSliderRepository) {
    suspend operator fun invoke(mode: AlertMode): Result<Unit> {
        return repository.setAlertMode(mode)
    }
}
