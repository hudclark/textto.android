package com.octopusbeach.textto.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.tasks.MessageSyncTask
import com.octopusbeach.textto.di.DaggerApiComponent
import com.octopusbeach.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 12/12/16.
 */
class NotificationListener : NotificationListenerService() {

    @Inject lateinit var apiService: ApiService

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

}
