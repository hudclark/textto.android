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
        apiService.getSendableMessages().execute().body()["scheduledMessages"]?.forEach {
            try {
                it.sent = true
                apiService.updateScheduledMessage(it._id, it).execute()
                MessageController.sendMessage(it.body, it.addresses, context, it._id)
            } catch (e: Exception) {
                Log.d(TAG, "Error deleting scheduled message ${it._id}")
            }
        }
    }

    override fun run() {
        Log.d(TAG, "Starting sync task")
        try {
            val updatedAt = apiService.getLastUpdated().execute().body()

            val sms = updatedAt["sms"]
            val mms = updatedAt["mms"]
            Log.e(TAG, sms.toString())
            Log.e(TAG, mms.toString())

            if (sms == null && mms == null) {
                // first sync
                Log.d(TAG, "Syncing recent threads")
                MessageController.syncRecentThreads(context, apiService, 20)
            } else {
                // TODO what happens when mms/sms has never been updated, and then this is called.
                // We need to make sure to sync anything 'recent' -- however that is defined.
                // This should only ever be used once for sms/mms
                val withinFive = System.currentTimeMillis() - (1000 * 60 * 5)

                Sms.syncSms(sms?.get("date") ?: withinFive, sms?.get("id")?.toInt() ?: -1, context, apiService)
                Mms.syncMms(mms?.get("date") ?: withinFive, mms?.get("id")?.toInt() ?: -1, context, apiService)
            }

            syncScheduledMessages()

            prefs.edit().putLong(MESSAGES_LAST_SYNCED, System.currentTimeMillis()).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating messages: $e")
        }
        Log.d(TAG, "Finished sync task")
    }
}
