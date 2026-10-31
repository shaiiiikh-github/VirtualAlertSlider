package com.virtualalertslider.app.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.virtualalertslider.app.R
import com.virtualalertslider.app.VirtualAlertSliderApp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.widget.VirtualAlertSliderWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@RequiresApi(Build.VERSION_CODES.N)
class VirtualAlertSliderTile : TileService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private val repository by lazy {
        VirtualAlertSliderApp.getRepository(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val currentMode = repository.getCurrentAlertMode()
        val nextMode = currentMode.next()

        serviceScope.launch {
            val result = repository.setAlertMode(nextMode)
            if (result.isFailure) {
                // Permission needed or system block
                showDialogAndPromptPermission()
                updateTileState()
            } else {
                updateTileState()
                // Refresh widget
                VirtualAlertSliderWidget.updateAllWidgets(this@VirtualAlertSliderTile)
            }
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val currentMode = repository.getCurrentAlertMode()

        tile.label = currentMode.title
        tile.subtitle = currentMode.subtitle

        val iconRes = when (currentMode) {
            AlertMode.RING -> R.drawable.ic_ring
            AlertMode.VIBRATE -> R.drawable.ic_vibrate
            AlertMode.SILENT -> R.drawable.ic_silent
        }
        tile.icon = Icon.createWithResource(this, iconRes)
        tile.state = Tile.STATE_ACTIVE
        tile.updateTile()
    }

    private fun showDialogAndPromptPermission() {
        val dndIntent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    this,
                    0,
                    dndIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                @Suppress("DEPRECATION")
                startActivityAndCollapse(dndIntent)
            }
        } catch (_: Exception) {
            val appIntent = packageManager.getLaunchIntentForPackage(packageName)
            if (appIntent != null) {
                appIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val pendingIntent = android.app.PendingIntent.getActivity(
                        this,
                        0,
                        appIntent,
                        android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pendingIntent)
                } else {
                    @Suppress("DEPRECATION")
                    startActivityAndCollapse(appIntent)
                }
            }
        }
    }
}
