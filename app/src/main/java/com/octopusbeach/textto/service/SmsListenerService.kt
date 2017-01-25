package com.octopusbeach.textto.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler

/**
 * Created by hudson on 9/6/16.
 */
class SmsListenerService: Service() {

    private var observer: SmsObserver? = null

    companion object {
        var running = false
    }

    override fun onDestroy() {
        running = false
        if (observer != null)
            contentResolver.unregisterContentObserver(observer)
        observer = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (observer == null) {
            observer = SmsObserver(Handler(), applicationContext)
            contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
        }
        running = true
        return START_STICKY
    }

    override fun onBind(intent: Intent?) = null

}