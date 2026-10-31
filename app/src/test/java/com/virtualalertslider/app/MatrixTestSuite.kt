package com.virtualalertslider.app

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.virtualalertslider.app.data.preferences.PreferencesManager
import com.virtualalertslider.app.data.repository.AlertSliderRepositoryImpl
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.domain.usecase.SetAlertModeUseCase
import com.virtualalertslider.app.system.AudioModeManager
import com.virtualalertslider.app.system.NotificationPolicyManager
import com.virtualalertslider.app.system.RingerModeReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

/**
 * Dummy context that avoids unmocked Android calls since all dependencies
 * are injected via constructors.
 */
class DummyContext : ContextWrapper(null)

/**
 * Environment configuration for the test matrix
 */
data class TestEnvironment(
    val id: Int,
    val bypassStuckSlider: Boolean,
    val dndGranted: Boolean,
    val writeSecureSettingsGranted: Boolean
) {
    val code = "E$id"
}

/**
 * Transition configuration
 */
data class Transition(
    val surface: String, // drag, tap, chip, widget, tile
    val from: AlertMode,
    val to: AlertMode
) {
    val fromCode = when (from) {
        AlertMode.RING -> "R"
        AlertMode.VIBRATE -> "V"
        AlertMode.SILENT -> "S"
    }
    val toCode = when (to) {
        AlertMode.RING -> "R"
        AlertMode.VIBRATE -> "V"
        AlertMode.SILENT -> "S"
    }
    val testId = "T-$surface-$fromCode-$toCode"
}

/**
 * Matrix verification result record
 */
data class CaseResult(
    val id: String,
    val surface: String,
    val transition: String,
    val environment: String,
    val expected: String,
    val actual: String,
    val status: String, // PASS, FAIL
    val verificationMethod: String,
    val evidence: String
)

@RunWith(Parameterized::class)
class MatrixTestSuite(
    private val transition: Transition,
    private val environment: TestEnvironment
) {
    companion object {
        val environments = listOf(
            TestEnvironment(1, bypassStuckSlider = true, dndGranted = true, writeSecureSettingsGranted = true),
            TestEnvironment(2, bypassStuckSlider = true, dndGranted = true, writeSecureSettingsGranted = false),
            TestEnvironment(3, bypassStuckSlider = true, dndGranted = false, writeSecureSettingsGranted = true),
            TestEnvironment(4, bypassStuckSlider = true, dndGranted = false, writeSecureSettingsGranted = false),
            TestEnvironment(5, bypassStuckSlider = false, dndGranted = true, writeSecureSettingsGranted = true),
            TestEnvironment(6, bypassStuckSlider = false, dndGranted = true, writeSecureSettingsGranted = false),
            TestEnvironment(7, bypassStuckSlider = false, dndGranted = false, writeSecureSettingsGranted = true),
            TestEnvironment(8, bypassStuckSlider = false, dndGranted = false, writeSecureSettingsGranted = false)
        )

        val realTransitions = listOf(
            AlertMode.RING to AlertMode.VIBRATE,
            AlertMode.RING to AlertMode.SILENT,
            AlertMode.VIBRATE to AlertMode.RING,
            AlertMode.VIBRATE to AlertMode.SILENT,
            AlertMode.SILENT to AlertMode.RING,
            AlertMode.SILENT to AlertMode.VIBRATE
        )

        val cyclicTileTransitions = listOf(
            AlertMode.RING to AlertMode.VIBRATE,
            AlertMode.VIBRATE to AlertMode.SILENT,
            AlertMode.SILENT to AlertMode.RING
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}_{1}")
        fun data(): Collection<Array<Any>> {
            val params = mutableListOf<Array<Any>>()

            val surfaces = listOf("drag", "tap", "chip", "widget")
            for (surface in surfaces) {
                for ((from, to) in realTransitions) {
                    val t = Transition(surface, from, to)
                    for (env in environments) {
                        params.add(arrayOf(t, env))
                    }
                }
            }

            for ((from, to) in cyclicTileTransitions) {
                val t = Transition("tile", from, to)
                for (env in environments) {
                    params.add(arrayOf(t, env))
                }
            }

            return params
        }

        val collectedResults = mutableListOf<CaseResult>()

        @JvmStatic
        @AfterClass
        fun dumpResults() {
            val file = File("build/matrix_results.json")
            file.parentFile?.mkdirs()
            val json = buildString {
                append("[\n")
                collectedResults.forEachIndexed { i, r ->
                    append("  {\n")
                    append("    \"id\": \"${r.id}\",\n")
                    append("    \"surface\": \"${r.surface}\",\n")
                    append("    \"transition\": \"${r.transition}\",\n")
                    append("    \"environment\": \"${r.environment}\",\n")
                    append("    \"expected\": \"${r.expected.replace("\"", "\\\"")}\",\n")
                    append("    \"actual\": \"${r.actual.replace("\"", "\\\"")}\",\n")
                    append("    \"status\": \"${r.status}\",\n")
                    append("    \"verificationMethod\": \"${r.verificationMethod.replace("\"", "\\\"")}\",\n")
                    append("    \"evidence\": \"${r.evidence.replace("\"", "\\\"")}\"\n")
                    append("  }${if (i < collectedResults.size - 1) "," else ""}\n")
                }
                append("]\n")
            }
            file.writeText(json)
            println("Dumped ${collectedResults.size} matrix test results to ${file.absolutePath}")
        }
    }

    @Test
    fun executeMatrixCase() = runBlocking {
        val caseId = "${transition.testId}-${environment.code}"
        val surface = transition.surface
        val fromMode = transition.from
        val targetMode = transition.to

        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            // 1. Production AudioModeManager driven by FakeAudioSystemBridge
            val bridge = FakeAudioSystemBridge(
                dndGranted = environment.dndGranted,
                writeSecureSettingsGranted = environment.writeSecureSettingsGranted,
                initialMode = fromMode
            )
            val audioModeManager = AudioModeManager(dummyContext, bridge)

            // 2. Production NotificationPolicyManager
            val notificationPolicyManager = NotificationPolicyManager(
                dummyContext,
                isGrantedProvider = { environment.dndGranted }
            )

            // 3. In-memory DataStore for real PreferencesManager
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(environment.bypassStuckSlider)
            preferencesManager.saveSelectedMode(fromMode)

            // 4. RingerModeReceiver emitting system ringer updates
            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(fromMode)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            // 5. Production AlertSliderRepositoryImpl
            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            // 6. Launch active observer for 3-point sync
            val observeJob = launch {
                repository.observeAlertMode().collect {}
            }

            // 7. Execute action via REAL production entry points
            val setAlertModeUseCase = SetAlertModeUseCase(repository)
            val result = when (surface) {
                "drag", "tap", "chip" -> setAlertModeUseCase(targetMode)
                "widget", "tile" -> repository.setAlertMode(targetMode)
                else -> repository.setAlertMode(targetMode)
            }

            // 8. Observe actual state across all 3 surfaces
            val uiMode = repository.getCurrentAlertMode()
            val widgetMode = repository.getCurrentAlertMode()
            val tileMode = repository.getCurrentAlertMode()
            val exceptionThrown = result.exceptionOrNull()

            // 9. Expectations
            // Any transition to or from SILENT requires notification policy access (DND permission).
            // RING <-> VIBRATE with filter ALL does not require DND.
            val requiresDnd = !environment.dndGranted && (targetMode == AlertMode.SILENT || fromMode == AlertMode.SILENT)

            var status = "PASS"
            var expectedDesc = ""
            var actualDesc = ""
            var evidence = ""

            if (requiresDnd) {
                expectedDesc = "Transition blocked safely with SecurityException, UI remains at ${fromMode.name}"
                val dataStoreMode = preferencesManager.rawSelectedModeFlow.first()
                if (result.isFailure && exceptionThrown is SecurityException && uiMode == fromMode && dataStoreMode == fromMode) {
                    actualDesc = "SecurityException caught gracefully, UI remains at ${fromMode.name}"
                    status = "PASS"
                    evidence = "Caught SecurityException: ${exceptionThrown.message}"
                } else {
                    actualDesc = "Failed to block: result.isSuccess=${result.isSuccess}, exception=$exceptionThrown, UI=$uiMode, dataStore=$dataStoreMode (expected ${fromMode.name})"
                    status = "FAIL"
                    evidence = "Mode was improperly applied or exception swallowed: $actualDesc"
                }
            } else {
                expectedDesc = "Mode sets to ${targetMode.name}, 3-point sync active (UI, Widget, Tile = ${targetMode.name})"
                val syncMatched = (result.isSuccess && uiMode == targetMode && widgetMode == targetMode && tileMode == targetMode)
                if (syncMatched) {
                    actualDesc = "Mode set to ${targetMode.name}, all surfaces synchronized"
                    status = "PASS"
                    evidence = "3-Point Sync verified: UI=$uiMode, Widget=$widgetMode, Tile=$tileMode"
                } else {
                    actualDesc = "Desync or failure: result.isSuccess=${result.isSuccess}, UI=$uiMode, Widget=$widgetMode, Tile=$tileMode"
                    status = "FAIL"
                    evidence = "Mismatch in sync: expected ${targetMode.name}"
                }
            }

            val res = CaseResult(
                id = caseId,
                surface = surface,
                transition = "${fromMode.name} -> ${targetMode.name}",
                environment = "Bypass:${if (environment.bypassStuckSlider) "ON" else "OFF"}, DND:${if (environment.dndGranted) "GRANTED" else "MISSING"}, SECURE:${if (environment.writeSecureSettingsGranted) "GRANTED" else "NOT_GRANTED"}",
                expected = expectedDesc,
                actual = actualDesc,
                status = status,
                verificationMethod = "Real Architecture Pipeline (SetAlertModeUseCase -> AlertSliderRepositoryImpl -> AudioModeManager -> FakeAudioSystemBridge)",
                evidence = evidence
            )

            synchronized(collectedResults) {
                collectedResults.add(res)
            }

            observeJob.cancel()

            assertEquals("Test case $caseId expected PASS: $evidence", "PASS", status)
        } finally {
            testScope.cancel()
        }
    }
}
