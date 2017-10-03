package com.octopusbeach.textto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.utils.ThreadUtils

/**
 * Created by hudson on 8/11/17.
 */
class DeliveryBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "DeliveryReciever"
        val MESSAGE_ID = "id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(MESSAGE_ID)
        if (TextUtils.isEmpty(id)) return
        Log.d(TAG, "Received delivered intent: $id")
        val apiService = (context.applicationContext as BaseApplication).appComponent.getApiService()
        ThreadUtils.runSingleThreadTask(Runnable {
            try {
                // Guess we'll just delete the message for now.
                // Could definitely decide to mark as delivered instead.
                apiService.deleteScheduledMessage(id).execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error marking message as sent", e)
            }
        })
    }

}