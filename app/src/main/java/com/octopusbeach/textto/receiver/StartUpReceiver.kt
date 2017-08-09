package com.octopusbeach.textto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Created by hudson on 9/6/16.
 */

class StartUpReceiver: BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        SmsObserverService.ensureStarted(context)
    }
}