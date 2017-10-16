package com.octopusbeach.textto.service

import android.content.Context
import android.content.Intent
import android.os.Build
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

        fun isEnabled (context: Context): Boolean {
            val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return (listeners != null && listeners.contains(context.packageName))
        }

    }

    override fun onBind(intent: Intent?) = super.onBind(intent)

    override fun onNotificationPosted(sbn: StatusBarNotification) {}
    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.getBooleanExtra(CLEAR_TEXT_NOTIFICATIONS, false) && isEnabled(applicationContext)) {
            try {
                clearTextNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing notifications", e)
                Crashlytics.logException(e)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun clearTextNotifications () {
        val defaultApp = Telephony.Sms.getDefaultSmsPackage(applicationContext)
        activeNotifications.forEach {
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