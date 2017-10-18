package com.moduloapps.textto.service

import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.tasks.MessageSyncTask
import com.moduloapps.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var sessionController: SessionController

    private val TAG = "MessagingService"

    private val TYPE_REFRESH = "refresh"
    private val TYPE_SYNC_CONTACTS = "syncContacts"
    private val TYPE_START_SESSION = "startSession"
    private val TYPE_STOP_SESSION = "endSession"

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
    }

    override fun onMessageReceived(msg: RemoteMessage) {
        if (!sessionController.isLoggedIn()) return
        val message = msg.data?.get("type") ?: return
        Log.d(TAG, "Received message: $message")
        when (message) {

            TYPE_REFRESH -> {
                // just want to ensure started
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.START_FOREGROUND)
                applicationContext.startService(intent)
                ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext as BaseApplication, prefs))
            }

            TYPE_SYNC_CONTACTS -> {
                applicationContext.startService(Intent(applicationContext, ContactSyncService::class.java))
            }

            TYPE_START_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.START_FOREGROUND)
                applicationContext.startService(intent)
                // On session start, update messages
                ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext as BaseApplication, prefs))
            }

            TYPE_STOP_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.STOP_FOREGROUND)
                applicationContext.startService(intent)
            }

        }

    }
}
