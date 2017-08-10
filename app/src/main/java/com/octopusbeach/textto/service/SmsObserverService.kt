package com.octopusbeach.textto.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.octopusbeach.textto.BaseApplication

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserverService: Service() {

    private var observer: SmsObserver? = null

    companion object {
        private val TAG = "SmsObserverService"

        private var running = false

        fun ensureStarted(context: Context) {
            if (!running) {
                context.startService(Intent(context, SmsObserverService::class.java))
            }
        }
    }

    override fun onDestroy() {
        running = false
        if (observer != null)
            contentResolver.unregisterContentObserver(observer)
        observer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val baseApp = applicationContext as BaseApplication

        if (observer == null) {
            Log.d(TAG, "Starting service")
            observer = SmsObserver(applicationContext, baseApp.appComponent.getApiService(), baseApp.appComponent.getSharedPrefs())
            contentResolver.registerContentObserver(Uri.parse("content://mms-sms"), true, observer)
        }
        running = true
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

}