package com.moduloapps.textto.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.utils.ThreadUtils
import java.io.File

/**
 * Created by hudson on 8/11/17.
 */
class DeliveryBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private val TAG = "DeliveryReciever"
        val MESSAGE_ID = "id"
        val FILENAME = "filename"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(MESSAGE_ID)
        if (TextUtils.isEmpty(id)) return
        Log.d(TAG, "Received delivered intent: $id")
        val apiService = (context.applicationContext as BaseApplication).appComponent.getApiService()
        val filename = intent.getStringExtra(FILENAME)
        ThreadUtils.runSingleThreadTask(Runnable {
            try {
                apiService.deleteScheduledMessage(id).execute()
                if (!TextUtils.isEmpty(filename)) {
                    Log.d(TAG, "Deleting $filename")
                    File(context.cacheDir, filename).delete()
                }
            } catch (e: Exception) {
                Crashlytics.logException(e)
                Log.e(TAG, "Error marking message as sent", e)
            }
        })
    }

}