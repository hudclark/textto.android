package com.moduloapps.textto.service

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import com.crashlytics.android.Crashlytics
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
    private val TYPE_PING_SESSION = "pingSession"

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
                startServiceCompat(intent)
                ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, applicationContext as BaseApplication, prefs))
            }

            TYPE_SYNC_CONTACTS -> {
                val intent = Intent(applicationContext, ContactSyncService::class.java)
                startServiceCompat(intent)
            }

            TYPE_START_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.START_FOREGROUND)
                startServiceCompat(intent)
            }

            TYPE_STOP_SESSION -> {
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.STOP_FOREGROUND)
                startService(intent)
            }

            TYPE_PING_SESSION -> {
                val timestamp = msg.data["timestamp"]?.toLong() ?: return
                val intent = Intent(applicationContext, SmsObserverService::class.java)
                intent.putExtra(SmsObserverService.FOREGROUND_EXTRA, SmsObserverService.PING_FOREGROUND)
                intent.putExtra(SmsObserverService.PING_TIMESTAMP, timestamp)
                startServiceCompat(intent)
            }

        }
    }

    private fun isOreo() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private fun startServiceCompat(service: Intent) {
        try {
            if (isOreo()) {
                applicationContext.startForegroundService(service)
            } else {
                applicationContext.startService(service)
            }
        } catch (e: Exception) {
            Crashlytics.logException(e)
        }
    }
}
