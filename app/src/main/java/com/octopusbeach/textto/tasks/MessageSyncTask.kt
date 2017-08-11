package com.octopusbeach.textto.tasks

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.message.MessageController
import com.octopusbeach.textto.message.Mms
import com.octopusbeach.textto.message.Sms

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
        apiService.getScheduledMessages(failed = false).execute().body()["scheduledMessages"]?.forEach {
            try {
                apiService.deleteScheduledMessage(it._id).execute()
                MessageController.sendMessage(it.body, it.addresses, context)
            } catch (e: Exception) {
                Log.d(TAG, "Error deleting scheduled message ${it._id}")
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
