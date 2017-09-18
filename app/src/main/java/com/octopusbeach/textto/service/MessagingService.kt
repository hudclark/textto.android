package com.octopusbeach.textto.service

import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.model.Contact
import com.octopusbeach.textto.tasks.MessageSyncTask
import com.octopusbeach.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences

    private val TAG = "MessagingService"

    val TYPE_REFRESH = "refresh"
    val TYPE_SYNC_CONTACTS = "syncContacts"
    val TYPE_START_SESSION = "startSession"
    val TYPE_STOP_SESSION = "endSession"

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        val message = msg.data?.get("type") ?: return
        Log.d(TAG, "Received message: $message")
        when (message) {

            TYPE_REFRESH -> {
                // just want to ensure started
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.START_FOREGROUND)
                applicationContext.startService(intent)
                ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext, prefs))
            }

            TYPE_SYNC_CONTACTS -> {
                applicationContext.startService(Intent(applicationContext, ContactSyncService::class.java))
            }

            TYPE_START_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.START_FOREGROUND)
                applicationContext.startService(intent)
            }

            TYPE_STOP_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.STOP_FOREGROUND)
                applicationContext.startService(intent)
            }

        }

    }
}
