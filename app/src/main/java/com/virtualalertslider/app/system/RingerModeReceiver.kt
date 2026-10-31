package com.virtualalertslider.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.app.NotificationManager
import android.os.Build
import com.virtualalertslider.app.domain.model.AlertMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

open class RingerModeReceiver(
    private val context: Context,
    private val audioModeManager: AudioModeManager = AudioModeManager(context),
    private val customFlow: Flow<AlertMode>? = null
) {

    open fun observeRingerModeChanges(): Flow<AlertMode> {
        if (customFlow != null) {
            return customFlow
        }
        return callbackFlow {
        // Send initial state immediately
        trySend(audioModeManager.getCurrentAlertMode())

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val action = intent?.action
                if (action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
                ) {
                    trySend(audioModeManager.getCurrentAlertMode())
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            }
        }

        context.registerReceiver(receiver, filter)

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore if unregistering fails
            }
        }
    }
}
}
