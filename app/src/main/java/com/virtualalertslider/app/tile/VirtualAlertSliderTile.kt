package com.virtualalertslider.app.tile

import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.virtualalertslider.app.R
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.system.AudioModeManager

@RequiresApi(Build.VERSION_CODES.N)
class VirtualAlertSliderTile : TileService() {

    private val audioModeManager by lazy { AudioModeManager(this) }

    override fun onStartListening() {
        super.onStartListening()
        updateTileState()
    }

    override fun onClick() {
        super.onClick()
        val currentMode = audioModeManager.getCurrentAlertMode()
        val nextMode = currentMode.next()

        val result = audioModeManager.setAlertMode(nextMode)
        if (result.isFailure) {
            // Permission needed or system block
            showDialogAndPromptPermission()
        } else {
            updateTileState()
            // Notify widget update as well
            com.virtualalertslider.app.widget.VirtualAlertSliderWidget.updateAllWidgets(this)
        }
    }

    private fun updateTileState() {
        val tile = qsTile ?: return
        val currentMode = audioModeManager.getCurrentAlertMode()

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
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivityAndCollapse(intent)
        }
    }
}
