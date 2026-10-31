package com.virtualalertslider.app.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.virtualalertslider.app.VirtualAlertSliderApp
import com.virtualalertslider.app.data.repository.AlertSliderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver(
    private val repositoryProvider: ((Context) -> AlertSliderRepository)? = null,
    private val isBootAction: ((Intent?) -> Boolean)? = null
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isBoot = isBootAction?.invoke(intent) ?: try {
            intent.action == Intent.ACTION_BOOT_COMPLETED
        } catch (_: Throwable) {
            false
        }
        if (!isBoot) return

        val pendingResult = try {
            goAsync()
        } catch (_: Throwable) {
            null
        }

        val repository = repositoryProvider?.invoke(context)
            ?: VirtualAlertSliderApp.getRepository(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = repository.observeSettings().first()
                if (settings.bypassStuckSlider) {
                    val startupMode = settings.defaultMode
                    // Apply saved startup mode through the same shared repository entry point
                    repository.setAlertMode(startupMode)
                }
            } catch (_: Throwable) {
                // Handle failure gracefully without crash
            } finally {
                try {
                    pendingResult?.finish()
                } catch (_: Throwable) {}
            }
        }
    }
}
