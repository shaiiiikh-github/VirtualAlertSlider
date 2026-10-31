package com.virtualalertslider.app

import android.app.Application
import android.content.Context
import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.data.repository.AlertSliderRepositoryImpl
import com.virtualalertslider.app.domain.usecase.SetAlertModeUseCase

class VirtualAlertSliderApp : Application() {

    val repository: AlertSliderRepository by lazy {
        AlertSliderRepositoryImpl(applicationContext)
    }

    val setAlertModeUseCase: SetAlertModeUseCase by lazy {
        SetAlertModeUseCase(repository)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: VirtualAlertSliderApp
            private set

        fun getRepository(context: Context): AlertSliderRepository {
            return (context.applicationContext as? VirtualAlertSliderApp)?.repository
                ?: AlertSliderRepositoryImpl(context.applicationContext)
        }

        fun getSetAlertModeUseCase(context: Context): SetAlertModeUseCase {
            return (context.applicationContext as? VirtualAlertSliderApp)?.setAlertModeUseCase
                ?: SetAlertModeUseCase(getRepository(context))
        }
    }
}

