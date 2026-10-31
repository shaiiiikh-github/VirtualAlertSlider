package com.virtualalertslider.app.system

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class NotificationPolicyManager(
    private val context: Context,
    private val isGrantedProvider: (() -> Boolean)? = null
) {

    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Checks whether the app has Notification Policy Access (Do Not Disturb access).
     * Required to set device to Silent/DND mode without SecurityException.
     */
    fun isNotificationPolicyAccessGranted(): Boolean {
        if (isGrantedProvider != null) {
            return isGrantedProvider.invoke()
        }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.isNotificationPolicyAccessGranted
        } else {
            true
        }
    }

    /**
     * Returns an intent to launch system Settings page where user can grant Notification Policy Access.
     */
    fun createNotificationPolicyAccessIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}
