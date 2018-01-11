/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.message

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.gson.JsonObject
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.utils.ThreadUtils
import java.io.File

/**
 * Created by hudson on 1/11/18.
 */
class MessageSentReceiver: BroadcastReceiver () {

    companion object {
        private val TAG = "MessageSentReceiver"
        const val MESSAGE_ID = "id"
        const val FILENAME = "filename"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(MESSAGE_ID)
        val filename = intent.getStringExtra(FILENAME)

        if (TextUtils.isEmpty(id)) {
            Crashlytics.logException(RuntimeException("Intent id for message $id was empty"))
            return
        }

        ThreadUtils.runSingleThreadTask(Runnable {
            val apiService = (context.applicationContext as BaseApplication).appComponent.getApiService()
            try {
                if (!TextUtils.isEmpty(filename)) File(context.cacheDir, filename).delete()
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Message has been sent.")
                    apiService.deleteScheduledMessage(id).execute()
                } else {
                    Log.d(TAG, "Message failed with code $resultCode")
                    val body = JsonObject()
                    body.addProperty("failureCode", resultCode)
                    apiService.reportFailed(id, body).execute()
                }
            } catch (e: Exception) {
                Crashlytics.logException(e)
            }

        })


    }


}