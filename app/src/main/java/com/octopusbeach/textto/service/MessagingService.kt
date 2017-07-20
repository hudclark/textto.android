package com.octopusbeach.textto.service

import android.content.SharedPreferences
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.tasks.MessageSyncTask
import com.octopusbeach.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences


    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext, prefs))
    }
}
