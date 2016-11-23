package com.octopusbeach.textto.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler

/**
 * Created by hudson on 9/6/16.
 */
class SmsListenerService: Service() {

    private lateinit var observer: SmsObserver

    companion object {
        var running = false
    }

    override fun onCreate() {
        observer = SmsObserver(Handler(), applicationContext)
        contentResolver.registerContentObserver(Uri.parse("content://sms"), true, observer)
        running = true
    }

    override fun onDestroy() {
        running = false
        contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

}