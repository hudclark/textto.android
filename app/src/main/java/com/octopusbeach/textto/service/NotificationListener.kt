package com.octopusbeach.textto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Created by hudson on 10/2/17.
 */
class NotificationListener : NotificationListenerService() {

    private val TAG = "NotificationListener"

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        return
        Log.e(TAG, "notification received")
        Log.d(TAG, "${sbn.packageName} ")
        Log.d(TAG, "${sbn.notification.extras} ")

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
    }

}