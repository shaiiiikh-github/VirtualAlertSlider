package com.virtualalertslider.app.domain.usecase

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.AlertMode

class GetAlertModeUseCase(private val repository: AlertSliderRepository) {
    operator fun invoke(): AlertMode {
        return repository.getCurrentAlertMode()
    }
}
