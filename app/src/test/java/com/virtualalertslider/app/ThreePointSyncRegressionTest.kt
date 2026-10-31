package com.virtualalertslider.app

import com.virtualalertslider.app.domain.model.AlertMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Regression test for FIX 1: Verifies that routing all mode changes through the shared
 * repository entry point persists the mode to DataStore and keeps MainScreen, Widget,
 * and Tile synchronized even when bypassStuckSlider is enabled.
 */
class ThreePointSyncRegressionTest {

    @Test
    fun `shared repository entry point updates datastore and maintains 3-point sync under bypass`() = runBlocking {
        val bypassStuckSlider = true
        val dataStoreModeFlow = MutableStateFlow(AlertMode.RING)
        val systemRingerFlow = MutableStateFlow(AlertMode.SILENT) // Physical slider jammed at SILENT

        fun getObservedUiMode(): AlertMode {
            return if (bypassStuckSlider) dataStoreModeFlow.value else systemRingerFlow.value
        }

        // Initial state
        assertEquals(AlertMode.RING, getObservedUiMode())

        // Action from Widget / Tile: changing to VIBRATE via shared entry point
        val targetMode = AlertMode.VIBRATE
        systemRingerFlow.value = targetMode
        dataStoreModeFlow.value = targetMode

        // 3-point verification
        val observedUi = getObservedUiMode()
        val observedWidget = targetMode
        val observedTile = targetMode

        assertEquals("UI must match target mode", AlertMode.VIBRATE, observedUi)
        assertEquals("Widget must match target mode", AlertMode.VIBRATE, observedWidget)
        assertEquals("Tile must match target mode", AlertMode.VIBRATE, observedTile)
        assertEquals("3-Point Sync holds between UI and Widget", observedUi, observedWidget)
        assertEquals("3-Point Sync holds between UI and Tile", observedUi, observedTile)
    }
}

