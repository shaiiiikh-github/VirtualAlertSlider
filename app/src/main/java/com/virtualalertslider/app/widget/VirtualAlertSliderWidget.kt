package com.virtualalertslider.app.widget

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.os.Build
import android.service.quicksettings.TileService
import android.widget.RemoteViews
import com.virtualalertslider.app.MainActivity
import com.virtualalertslider.app.R
import com.virtualalertslider.app.VirtualAlertSliderApp
import com.virtualalertslider.app.domain.model.AlertMode
import com.virtualalertslider.app.tile.VirtualAlertSliderTile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VirtualAlertSliderWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action ?: return

        if (action.startsWith(ACTION_SET_MODE_PREFIX)) {
            val modeId = action.removePrefix(ACTION_SET_MODE_PREFIX)
            val mode = AlertMode.fromId(modeId)
            val pendingResult = goAsync()
            val repository = VirtualAlertSliderApp.getRepository(context)

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result = repository.setAlertMode(mode)
                    if (result.isFailure) {
                        withContext(Dispatchers.Main) {
                            try {
                                android.widget.Toast.makeText(
                                    context,
                                    "Do Not Disturb access required to switch ringer mode",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } catch (_: Throwable) {}
                            updateAllWidgets(context)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            updateAllWidgets(context)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                TileService.requestListeningState(
                                    context,
                                    ComponentName(context, VirtualAlertSliderTile::class.java)
                                )
                            }
                        }
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == AudioManager.RINGER_MODE_CHANGED_ACTION ||
            (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) ||
            action == AppWidgetManager.ACTION_APPWIDGET_UPDATE
        ) {
            updateAllWidgets(context)
        }
    }

    companion object {
        private const val ACTION_SET_MODE_PREFIX = "com.virtualalertslider.app.SET_MODE_"

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, VirtualAlertSliderWidget::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = VirtualAlertSliderApp.getRepository(context)
            val currentMode = repository.getCurrentAlertMode()
            val views = RemoteViews(context.packageName, R.layout.widget_alert_slider)

            // Header state
            views.setTextViewText(R.id.widget_mode_title, currentMode.title.uppercase())
            val modeIconRes = when (currentMode) {
                AlertMode.RING -> R.drawable.ic_ring
                AlertMode.VIBRATE -> R.drawable.ic_vibrate
                AlertMode.SILENT -> R.drawable.ic_silent
            }
            views.setImageViewResource(R.id.widget_mode_icon, modeIconRes)

            // Set button highlight background images
            val activeBg = R.drawable.bg_widget_btn_active
            val inactiveBg = R.drawable.bg_widget_btn_inactive

            views.setImageViewResource(R.id.bg_widget_ring, if (currentMode == AlertMode.RING) activeBg else inactiveBg)
            views.setImageViewResource(R.id.bg_widget_vibrate, if (currentMode == AlertMode.VIBRATE) activeBg else inactiveBg)
            views.setImageViewResource(R.id.bg_widget_silent, if (currentMode == AlertMode.SILENT) activeBg else inactiveBg)

            // Set button text colors (iOS style active white, inactive muted gray)
            views.setTextColor(R.id.txt_widget_ring, if (currentMode == AlertMode.RING) Color.WHITE else Color.parseColor("#8E8E93"))
            views.setTextColor(R.id.txt_widget_vibrate, if (currentMode == AlertMode.VIBRATE) Color.WHITE else Color.parseColor("#8E8E93"))
            views.setTextColor(R.id.txt_widget_silent, if (currentMode == AlertMode.SILENT) Color.WHITE else Color.parseColor("#8E8E93"))

            // Pending intents for button clicks
            views.setOnClickPendingIntent(R.id.btn_widget_ring, createModePendingIntent(context, AlertMode.RING, 101))
            views.setOnClickPendingIntent(R.id.btn_widget_vibrate, createModePendingIntent(context, AlertMode.VIBRATE, 102))
            views.setOnClickPendingIntent(R.id.btn_widget_silent, createModePendingIntent(context, AlertMode.SILENT, 103))

            // Container tap opens app main screen
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                `package` = context.packageName
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingMainIntent = PendingIntent.getActivity(
                context,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingMainIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createModePendingIntent(context: Context, mode: AlertMode, requestCode: Int): PendingIntent {
            val intent = Intent(context, VirtualAlertSliderWidget::class.java).apply {
                action = ACTION_SET_MODE_PREFIX + mode.id
            }
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
