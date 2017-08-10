package com.octopusbeach.textto.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Created by hudson on 7/20/17.
 */
class MessageBroadcastReceiver: BroadcastReceiver() {

    private val TAG = MessageBroadcastReceiver::class.java.simpleName

    override fun onReceive(p0: Context?, p1: Intent?) {
        Log.e(TAG, "ON RECEIVE")
    }

}