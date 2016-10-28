package com.octopusbeach.textto.service

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.util.Log

/**
 * Created by hudson on 9/6/16.
 */
class SmsListenerService: Service() {

    override fun onCreate() {
        contentResolver
                .registerContentObserver(Uri.parse("content://sms"), true, SmsObserver(Handler(), this.applicationContext))
    }

    override fun onBind(intent: Intent?) = null

}