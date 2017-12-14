package com.moduloapps.textto.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.crashlytics.android.Crashlytics

/**
 * Created by hudson on 10/2/17.
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private val TAG = "NotificationListener"
        val CLEAR_TEXT_NOTIFICATIONS = "clear_text"

        var isRunning = false

        fun isEnabled (context: Context): Boolean {
            val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return (listeners != null && listeners.contains(context.packageName))
        }

    }

    override fun onBind(intent: Intent): IBinder {
        val action = intent.action
        if (action == SERVICE_INTERFACE) {
            Log.d(TAG, "Bound by system")
        }
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        if (intent != null && intent.getBooleanExtra(CLEAR_TEXT_NOTIFICATIONS, false) && isEnabled(applicationContext)) {
            try {
                clearTextNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing notifications", e)
                Crashlytics.logException(e)
            }
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Destroying")
    }

    private fun clearTextNotifications () {
        Log.d(TAG, "Clearing text notifications")
        val defaultApp = Telephony.Sms.getDefaultSmsPackage(applicationContext)
        activeNotifications?.forEach {
            if (it.packageName == defaultApp) {
                if (Build.VERSION.SDK_INT >= 21) {
                    cancelNotification(it.key)
                } else {
                    cancelNotification(defaultApp, it.tag, it.id)
                }
            }
        }
    }


}