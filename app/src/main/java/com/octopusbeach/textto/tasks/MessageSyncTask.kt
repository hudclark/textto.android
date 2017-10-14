package com.octopusbeach.textto.tasks

import android.content.SharedPreferences
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.message.MessageController
import com.octopusbeach.textto.message.MessageSender
import com.octopusbeach.textto.message.Mms
import com.octopusbeach.textto.message.Sms
import com.octopusbeach.textto.model.ScheduledMessage

/**
 * Created by hudson on 7/14/17.
 */
class MessageSyncTask(val apiService: ApiService,
                      val context: BaseApplication,
                      val prefs: SharedPreferences) : Runnable {

    private val TAG = "MessageSyncTask"

    companion object {
        val MESSAGES_LAST_SYNCED = "messages_last_synced"
    }

    override fun run() {
        Log.d(TAG, "Starting sync task")
        try {

            val status = apiService.getStatusUpdate().execute().body() ?: return

            val sms = status.sms
            val mms = status.mms
            Log.d(TAG, sms.toString())
            Log.d(TAG, mms.toString())

            if (sms == null && mms == null) {
                // first sync
                Log.d(TAG, "Syncing recent threads")
                MessageController.syncRecentThreads(context, apiService, 20)
            } else {
                // We need to make sure to sync anything 'recent' -- however that is defined.
                // This should only ever be used once for sms/mms
                val withinFive = System.currentTimeMillis() - (1000 * 60 * 5)

                Sms.syncSms(sms?.get("date") ?: withinFive, sms?.get("id")?.toInt() ?: -1, context, apiService)
                Mms.syncMms(mms?.get("date") ?: withinFive, mms?.get("id")?.toInt() ?: -1, context, apiService)
            }

            syncScheduledMessages(status.scheduledMessages)

            prefs.edit().putLong(MESSAGES_LAST_SYNCED, System.currentTimeMillis()).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating messages: $e")
        }
        Log.d(TAG, "Finished sync task")
    }

    private fun syncScheduledMessages(scheduledMessages: Array<ScheduledMessage>) {
        scheduledMessages.forEach {
            try {
                it.sent = true
                apiService.updateScheduledMessage(it._id, it).execute()
                MessageSender.sendMessage(it, context)
            } catch (e: Exception) {
                Log.d(TAG, "Error deleting scheduled message ${it._id}")
            }
        }
    }

}
