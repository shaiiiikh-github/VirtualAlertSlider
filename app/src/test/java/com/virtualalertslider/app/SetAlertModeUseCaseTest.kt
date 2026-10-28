package com.virtualalertslider.app

import com.virtualalertslider.app.data.repository.AlertSliderRepository
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.model.SliderSettings
import com.virtualalertslider.app.domain.model.ThemePreference
import com.virtualalertslider.app.domain.usecase.SetAlertModeUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeAlertSliderRepository : AlertSliderRepository {
    var activeMode: AlertMode = AlertMode.RING
    var shouldFail: Boolean = false

    override fun observeAlertMode(): Flow<AlertMode> = flowOf(activeMode)
    override fun getCurrentAlertMode(): AlertMode = activeMode

    override suspend fun setAlertMode(mode: AlertMode): Result<Unit> {
        return if (shouldFail) {
            Result.failure(SecurityException("Permission Denied"))
        } else {
            activeMode = mode
            Result.success(Unit)
        }
    }

    override fun observeSettings(): Flow<SliderSettings> = flowOf(SliderSettings())
    override suspend fun saveDefaultMode(mode: AlertMode) {}
    override suspend fun saveHapticFeedbackEnabled(enabled: Boolean) {}
    override suspend fun saveThemePreference(themePreference: ThemePreference) {}
    override suspend fun saveBypassStuckSlider(enabled: Boolean) {}
    override fun isNotificationPolicyAccessGranted(): Boolean = !shouldFail
}

class SetAlertModeUseCaseTest {

    @Test
    fun `invoke success updates repository mode`() = runBlocking {
        val fakeRepo = FakeAlertSliderRepository()
        val setAlertModeUseCase = SetAlertModeUseCase(fakeRepo)

        val result = setAlertModeUseCase(AlertMode.VIBRATE)

        assertTrue(result.isSuccess)
        org.junit.Assert.assertEquals(AlertMode.VIBRATE, fakeRepo.activeMode)
    }

    @Test
    fun `invoke failure returns SecurityException when access restricted`() = runBlocking {
        val fakeRepo = FakeAlertSliderRepository().apply { shouldFail = true }
        val setAlertModeUseCase = SetAlertModeUseCase(fakeRepo)

        val result = setAlertModeUseCase(AlertMode.SILENT)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is SecurityException)
    }
}
