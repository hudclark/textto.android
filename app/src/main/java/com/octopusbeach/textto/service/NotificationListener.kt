package com.octopusbeach.textto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.octopusbeach.textto.utils.MessageUtils

/**
 * Created by hudson on 12/12/16.
 */
class NotificationListener : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        Log.d("NotificationService", "Notification received")
        // TODO maybe attempt to narrow this down a bit
        MessageUtils.updateMessages(applicationContext)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

}