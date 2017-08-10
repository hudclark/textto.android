package com.octopusbeach.textto.tasks

import android.content.Context
import android.content.SharedPreferences
import android.telephony.SmsManager
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.mms.Mms
import com.octopusbeach.textto.sms.Sms
import com.octopusbeach.textto.utils.ThreadUtils
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Created by hudson on 7/14/17.
 */
class MessageSyncTask(val apiService: ApiService,
                      val context: Context,
                      val prefs: SharedPreferences) : Runnable {

    private val TAG = "MessageSyncTask"

    companion object {
        val MESSAGES_LAST_SYNCED = "messages_last_synced"
    }

    private fun syncScheduledMessages() {
        return
        val messages = apiService.getScheduledMessages(failed = false).execute().body()["scheduledMessages"]
        if (messages != null) {
            val manager = SmsManager.getDefault()
            messages.forEach {
                try {
                    apiService.deleteScheduledMessage(it._id).execute()
                    // make sure to break up body if it is too long
                    if (it.body.length > 160 ) {
                        val smsParts = manager.divideMessage(it.body)
                        manager.sendMultipartTextMessage(it.address, null, smsParts, null, null)
                    } else
                        manager.sendTextMessage(it.address, null, it.body, null, null)
                } catch (e: Exception) {
                    Log.d(TAG, "Error deleting scheduled message ${it._id}")
                }
            }
        }
    }

    override fun run() {
        try {

            val updatedAt = apiService.getLastUpdated().execute().body()
            val sms = updatedAt["sms"] ?: System.currentTimeMillis()
            val mms = updatedAt["sms"] ?: System.currentTimeMillis()

            Sms.syncSmsToDate(sms, context, apiService)
            Mms.syncMmsToDate(mms, context, apiService)

            syncScheduledMessages()
            prefs.edit().putLong(MESSAGES_LAST_SYNCED, System.currentTimeMillis()).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating messages: $e")
        }
    }
}
