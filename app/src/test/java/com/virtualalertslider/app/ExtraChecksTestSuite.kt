package com.virtualalertslider.app

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExtraChecksTestSuite {

    @Test
    fun testNoOpTransitions() = runBlocking {
        // Fix 5: Selecting the currently active mode should be a no-op (0 system/DataStore writes)
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveSelectedMode(AlertMode.RING)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.RING)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            val observeJob = launch { repository.observeAlertMode().collect {} }

            // Ensure current mode is RING
            assertEquals(AlertMode.RING, repository.getCurrentAlertMode())
            val initialWrites = bridge.totalWriteOperations

            // Perform no-op transition (RING -> RING)
            val result = repository.setAlertMode(AlertMode.RING)
            assertTrue(result.isSuccess)

            val redundantWrites = bridge.totalWriteOperations - initialWrites
            println("No-Op Transition RING->RING performed $redundantWrites write operations to system bridge.")

            observeJob.cancel()

            // Desired behavior: No-op transitions must perform 0 write operations
            assertEquals("No-op transition must perform 0 system writes", 0, redundantWrites)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testPersistenceAndBootRecovery() = runBlocking {
        // Fix 3: Boot recovery for startup mode when bypass is enabled
        // 1. Verify Manifest declaration
        val manifestFile = File("src/main/AndroidManifest.xml").let {
            if (it.exists()) it else File("app/src/main/AndroidManifest.xml")
        }
        assertTrue("Manifest file should exist", manifestFile.exists())
        val manifestText = manifestFile.readText()

        val hasBootPermission = manifestText.contains("android.permission.RECEIVE_BOOT_COMPLETED")
        val hasBootReceiver = manifestText.contains("BOOT_COMPLETED")

        assertTrue("RECEIVE_BOOT_COMPLETED permission must be declared in AndroidManifest.xml", hasBootPermission)
        assertTrue("BootReceiver must be declared in AndroidManifest.xml", hasBootReceiver)

        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            // 2. Scenario: Bypass is ON -> BootReceiver applies saved startup mode
            val bridge1 = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager1 = AudioModeManager(dummyContext, bridge1)
            val notificationPolicyManager1 = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore1 = InMemoryDataStore()
            val preferencesManager1 = PreferencesManager(dummyContext, dataStore1)
            preferencesManager1.saveBypassStuckSlider(true)
            preferencesManager1.saveDefaultMode(AlertMode.VIBRATE)

            val systemFlow1 = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow1.tryEmit(AlertMode.RING)
            val ringerModeReceiver1 = RingerModeReceiver(dummyContext, audioModeManager1, customFlow = systemFlow1)

            val repository1 = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager1,
                notificationPolicyManager = notificationPolicyManager1,
                preferencesManager = preferencesManager1,
                ringerModeReceiver = ringerModeReceiver1
            )

            val receiver1 = com.virtualalertslider.app.system.BootReceiver(
                repositoryProvider = { repository1 },
                isBootAction = { true }
            )
            receiver1.onReceive(dummyContext, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
            delay(50) // allow async boot job to finish

            assertEquals(
                "When bypass is ON, boot receiver should apply saved startup mode (VIBRATE)",
                AlertMode.VIBRATE,
                repository1.getCurrentAlertMode()
            )

            // 3. Scenario: Bypass is OFF -> BootReceiver does nothing
            val bridge2 = FakeAudioSystemBridge(initialMode = AlertMode.SILENT)
            val audioModeManager2 = AudioModeManager(dummyContext, bridge2)
            val notificationPolicyManager2 = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore2 = InMemoryDataStore()
            val preferencesManager2 = PreferencesManager(dummyContext, dataStore2)
            preferencesManager2.saveBypassStuckSlider(false)
            preferencesManager2.saveDefaultMode(AlertMode.RING)

            val systemFlow2 = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow2.tryEmit(AlertMode.SILENT)
            val ringerModeReceiver2 = RingerModeReceiver(dummyContext, audioModeManager2, customFlow = systemFlow2)

            val repository2 = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager2,
                notificationPolicyManager = notificationPolicyManager2,
                preferencesManager = preferencesManager2,
                ringerModeReceiver = ringerModeReceiver2
            )

            val receiver2 = com.virtualalertslider.app.system.BootReceiver(
                repositoryProvider = { repository2 },
                isBootAction = { true }
            )
            receiver2.onReceive(dummyContext, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
            delay(50)

            assertEquals(
                "When bypass is OFF, boot receiver should do nothing (remain at SILENT)",
                AlertMode.SILENT,
                repository2.getCurrentAlertMode()
            )

            // 4. Scenario: DND permission missing -> BootReceiver handles gracefully without crashing
            val bridge3 = FakeAudioSystemBridge(dndGranted = false, initialMode = AlertMode.RING)
            val audioModeManager3 = AudioModeManager(dummyContext, bridge3)
            val notificationPolicyManager3 = NotificationPolicyManager(dummyContext, isGrantedProvider = { false })
            val dataStore3 = InMemoryDataStore()
            val preferencesManager3 = PreferencesManager(dummyContext, dataStore3)
            preferencesManager3.saveBypassStuckSlider(true)
            preferencesManager3.saveDefaultMode(AlertMode.SILENT) // requires DND

            val systemFlow3 = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow3.tryEmit(AlertMode.RING)
            val ringerModeReceiver3 = RingerModeReceiver(dummyContext, audioModeManager3, customFlow = systemFlow3)

            val repository3 = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager3,
                notificationPolicyManager = notificationPolicyManager3,
                preferencesManager = preferencesManager3,
                ringerModeReceiver = ringerModeReceiver3
            )

            val receiver3 = com.virtualalertslider.app.system.BootReceiver(
                repositoryProvider = { repository3 },
                isBootAction = { true }
            )
            receiver3.onReceive(dummyContext, android.content.Intent(android.content.Intent.ACTION_BOOT_COMPLETED))
            delay(50)

            assertEquals(
                "When DND is missing on boot, failure is handled gracefully and mode remains RING",
                AlertMode.RING,
                repository3.getCurrentAlertMode()
            )
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testRapidInputSpamRaceCondition() = runBlocking {
        // Fix 4: Rapid input race condition. Sequentialized writes must ensure the final state
        // always equals the LAST user intent, even if earlier operations suffer unexpected I/O latency.
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(true)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.RING)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            val observeJob = launch { repository.observeAlertMode().collect {} }

            // 10+ rapid mixed inputs dispatched from drag, chips, tile and widget
            val rapidInputs = listOf(
                AlertMode.VIBRATE, AlertMode.SILENT, AlertMode.RING,
                AlertMode.VIBRATE, AlertMode.SILENT, AlertMode.RING,
                AlertMode.VIBRATE, AlertMode.SILENT, AlertMode.VIBRATE,
                AlertMode.RING // 10th and LAST user intent
            )
            val expectedLastIntent = rapidInputs.last()

            // Dispatch rapid inputs in chronological order into background coroutines
            val jobs = rapidInputs.map { targetMode ->
                val job = launch(Dispatchers.Default) {
                    repository.setAlertMode(targetMode)
                }
                delay(25)
                job
            }

            jobs.forEach { it.join() }
            delay(20)

            val finalMode = repository.getCurrentAlertMode()
            val persistedMode = preferencesManager.rawSelectedModeFlow.first()
            println("Rapid input race condition final mode: $finalMode, persisted: $persistedMode (expected: $expectedLastIntent)")

            observeJob.cancel()

            // Desired behavior: Final state and persisted state must equal the LAST user intent
            assertEquals("Final mode must match the last user intent", expectedLastIntent, finalMode)
            assertEquals("Persisted mode must match the last user intent", expectedLastIntent, persistedMode)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testVibrateWithoutDndThrowsSecurityException() = runBlocking {
        // Fix 2: Switching to VIBRATE while DND Total Silence / Alarms is active and DND access is missing
        // must return Result.failure(SecurityException) and leave the alert mode unchanged.
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(dndGranted = false, initialMode = AlertMode.SILENT)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { false })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveSelectedMode(AlertMode.SILENT)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.SILENT)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            val observeJob = launch { repository.observeAlertMode().collect {} }

            assertEquals("Initial mode must be SILENT", AlertMode.SILENT, repository.getCurrentAlertMode())

            // Attempt transition to VIBRATE without DND permission
            val result = repository.setAlertMode(AlertMode.VIBRATE)

            println("Vibrate without DND result: isSuccess=${result.isSuccess}, exception=${result.exceptionOrNull()}")

            observeJob.cancel()

            // Desired behavior (Fix 2): Must return Result.failure with SecurityException and mode remains SILENT
            assertTrue("Result must be failure when DND is missing and switching to VIBRATE", result.isFailure)
            assertTrue(
                "Exception must be SecurityException, but was ${result.exceptionOrNull()}",
                result.exceptionOrNull() is SecurityException
            )
            assertEquals("Alert mode must remain SILENT when transition fails", AlertMode.SILENT, repository.getCurrentAlertMode())
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testThreePointSyncUnderBypass() = runBlocking {
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(true)
            preferencesManager.saveSelectedMode(AlertMode.RING)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.SILENT) // Hardware switch stuck at SILENT
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            var observedUiMode = AlertMode.RING
            val observeJob = launch {
                repository.observeAlertMode().collect { observedUiMode = it }
            }

            // Tile/Widget changes mode to VIBRATE via repository entry point
            val result = repository.setAlertMode(AlertMode.VIBRATE)
            assertTrue(result.isSuccess)

            delay(10)
            val widgetMode = repository.getCurrentAlertMode()
            val tileMode = repository.getCurrentAlertMode()

            println("3-Point Sync Under Bypass: UI=$observedUiMode, Widget=$widgetMode, Tile=$tileMode")

            observeJob.cancel()

            assertEquals("UI must match target mode", AlertMode.VIBRATE, observedUiMode)
            assertEquals("Widget must match target mode", AlertMode.VIBRATE, widgetMode)
            assertEquals("Tile must match target mode", AlertMode.VIBRATE, tileMode)
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testExternalSystemRingerBroadcast() = runBlocking {
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(true)
            preferencesManager.saveSelectedMode(AlertMode.RING)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.RING)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            var observedMode = AlertMode.RING
            val observeJob = launch {
                repository.observeAlertMode().collect { observedMode = it }
            }

            // When bypass is ON: Hardware slider fallback to SILENT should be ignored by the UI
            systemFlow.emit(AlertMode.SILENT)
            delay(10)
            assertEquals("When bypass is ON, UI ignores external hardware fallbacks", AlertMode.RING, observedMode)

            // When bypass is OFF: UI follows external system changes
            preferencesManager.saveBypassStuckSlider(false)
            delay(10)
            assertEquals("When bypass is OFF, UI follows external system changes", AlertMode.SILENT, observedMode)

            observeJob.cancel()
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testReapplyModeWhenSystemReMutedUnderBypass() = runBlocking {
        // Fix 5 regression: If system ringer drops to SILENT (stuck slider re-mutes),
        // lastKnownMode and DataStore say RING, selecting RING with bypass ON must unmute
        // and reapply the system mode, NOT treat it as a no-op.
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(true)
            preferencesManager.saveSelectedMode(AlertMode.RING)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.RING)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            // Prime repository and lastKnownMode to RING
            val observeJob = launch { repository.observeAlertMode().collect {} }
            delay(20)
            assertEquals(AlertMode.RING, repository.getCurrentAlertMode())

            // Hardware slider or volume re-mutes system to SILENT behind the scenes
            bridge.setRingerMode(android.media.AudioManager.RINGER_MODE_SILENT)
            bridge.adjustStreamVolume(android.media.AudioManager.STREAM_RING, android.media.AudioManager.ADJUST_MUTE, 0)
            bridge.adjustStreamVolume(android.media.AudioManager.STREAM_NOTIFICATION, android.media.AudioManager.ADJUST_MUTE, 0)
            systemFlow.emit(AlertMode.SILENT)
            delay(20)

            // Confirm that DataStore and observed mode still report RING because bypass is ON
            assertEquals(AlertMode.RING, repository.getCurrentAlertMode())
            assertEquals(android.media.AudioManager.RINGER_MODE_SILENT, bridge.getRingerMode())

            // User selects RING again to overcome the hardware re-mute
            val result = repository.setAlertMode(AlertMode.RING)
            assertTrue(result.isSuccess)

            // Verification: System ringer MUST be unmuted and restored to NORMAL, not skipped as a no-op
            assertEquals(
                "System ringer mode must be restored to NORMAL",
                android.media.AudioManager.RINGER_MODE_NORMAL,
                bridge.getRingerMode()
            )
            assertFalse(
                "Ring stream must be unmuted",
                bridge.streamRingMuted
            )

            observeJob.cancel()
        } finally {
            testScope.cancel()
        }
    }

    @Test
    fun testCancellationDuringWriteMaintainsConsistency() = runBlocking {
        // Fix 4: Apply-and-persist inside withContext(NonCancellable) ensures that
        // cancellation cannot leave system audio changed but DataStore unsaved.
        val dummyContext = DummyContext()
        val testScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        try {
            val bridge = FakeAudioSystemBridge(initialMode = AlertMode.RING)
            val audioModeManager = AudioModeManager(dummyContext, bridge)
            val notificationPolicyManager = NotificationPolicyManager(dummyContext, isGrantedProvider = { true })
            val dataStore = InMemoryDataStore()
            val preferencesManager = PreferencesManager(dummyContext, dataStore)
            preferencesManager.saveBypassStuckSlider(true)
            preferencesManager.saveSelectedMode(AlertMode.RING)

            val systemFlow = MutableSharedFlow<AlertMode>(replay = 1)
            systemFlow.tryEmit(AlertMode.RING)
            val ringerModeReceiver = RingerModeReceiver(dummyContext, audioModeManager, customFlow = systemFlow)

            val repository = AlertSliderRepositoryImpl(
                context = dummyContext,
                audioModeManager = audioModeManager,
                notificationPolicyManager = notificationPolicyManager,
                preferencesManager = preferencesManager,
                ringerModeReceiver = ringerModeReceiver
            )

            val observeJob = launch { repository.observeAlertMode().collect {} }
            delay(20)

            // Launch a mode change and cancel its job concurrently
            val job = launch(Dispatchers.Default) {
                repository.setAlertMode(AlertMode.VIBRATE)
            }
            job.cancel()
            job.join()
            delay(30)

            val systemMode = audioModeManager.getCurrentAlertMode()
            val persistedMode = preferencesManager.rawSelectedModeFlow.first()

            // Consistency invariant: System mode and Persisted mode MUST match (never desynchronized)
            assertEquals(
                "System mode and Persisted mode must stay consistent even under cancellation",
                systemMode,
                persistedMode
            )

            observeJob.cancel()
        } finally {
            testScope.cancel()
        }
    }
}
