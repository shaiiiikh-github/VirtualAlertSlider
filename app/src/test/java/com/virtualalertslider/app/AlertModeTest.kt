package com.virtualalertslider.app

import android.app.NotificationManager
import android.media.AudioManager
import com.virtualalertslider.app.domain.model.AlertMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AlertModeTest {

    @Test
    fun `next mode cycles correctly from Ring to Vibrate to Silent to Ring`() {
        assertEquals(AlertMode.VIBRATE, AlertMode.RING.next())
        assertEquals(AlertMode.SILENT, AlertMode.VIBRATE.next())
        assertEquals(AlertMode.RING, AlertMode.SILENT.next())
    }

    @Test
    fun `fromRingerMode returns correct AlertMode for Normal, Vibrate, and Silent`() {
        val ringMode = AlertMode.fromRingerMode(AudioManager.RINGER_MODE_NORMAL, NotificationManager.INTERRUPTION_FILTER_ALL)
        assertEquals(AlertMode.RING, ringMode)

        val vibrateMode = AlertMode.fromRingerMode(AudioManager.RINGER_MODE_VIBRATE, NotificationManager.INTERRUPTION_FILTER_ALL)
        assertEquals(AlertMode.VIBRATE, vibrateMode)

        val silentMode = AlertMode.fromRingerMode(AudioManager.RINGER_MODE_SILENT, NotificationManager.INTERRUPTION_FILTER_PRIORITY)
        assertEquals(AlertMode.SILENT, silentMode)
    }

    @Test
    fun `fromPositionIndex returns correct AlertMode`() {
        assertEquals(AlertMode.RING, AlertMode.fromPositionIndex(0))
        assertEquals(AlertMode.VIBRATE, AlertMode.fromPositionIndex(1))
        assertEquals(AlertMode.SILENT, AlertMode.fromPositionIndex(2))
        assertEquals(AlertMode.RING, AlertMode.fromPositionIndex(99))
    }

    @Test
    fun `fromId returns correct AlertMode case-insensitively`() {
        assertEquals(AlertMode.RING, AlertMode.fromId("RING"))
        assertEquals(AlertMode.VIBRATE, AlertMode.fromId("vibrate"))
        assertEquals(AlertMode.SILENT, AlertMode.fromId("Silent"))
        assertEquals(AlertMode.RING, AlertMode.fromId("INVALID"))
    }
}
