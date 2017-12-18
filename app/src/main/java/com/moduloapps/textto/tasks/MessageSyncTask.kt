package com.moduloapps.textto.tasks

import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.message.MessageController
import com.moduloapps.textto.message.MessageSender
import com.moduloapps.textto.message.Mms
import com.moduloapps.textto.message.Sms
import com.moduloapps.textto.model.ScheduledMessage
import com.moduloapps.textto.service.NotificationListener

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
        var isInitialSync = false
        try {
            val status = apiService.getStatusUpdate().execute().body() ?: return

            val sms = status.sms
            val mms = status.mms
            Log.d(TAG, sms.toString())
            Log.d(TAG, mms.toString())

            if (sms == null && mms == null) {
                // first sync
                Log.d(TAG, "Syncing recent threads")
                isInitialSync = true
                apiService.startInitialSync().execute()
                MessageController.syncRecentThreads(context, apiService, 20)
            } else {
                // We need to make sure to sync anything 'recent' -- however that is defined.
                // This should only ever be used once for sms/mms
                val withinFive = System.currentTimeMillis() - (1000 * 60 * 5)

                Sms.syncSms(sms?.get("date") ?: withinFive, sms?.get("id")?.toInt() ?: -1, context, apiService)
                Mms.syncMms(mms?.get("date") ?: withinFive, mms?.get("id")?.toInt() ?: -1, context, apiService)
            }

            Log.d(TAG, status.scheduledMessages.size.toString() + " texts need to be sent")
            syncScheduledMessages(status.scheduledMessages)

            prefs.edit().putLong(MESSAGES_LAST_SYNCED, System.currentTimeMillis()).commit()
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error updating messages: $e")
        } finally {
            if (isInitialSync) {
                try {
                    apiService.endInitialSync().execute()
                } catch (e: Exception) {
                    Log.e(TAG, "Error ending initial sync", e)
                    Crashlytics.logException(e)
                }
            }
        }
        Log.d(TAG, "Finished sync task")
    }

    private fun syncScheduledMessages(scheduledMessages: Array<ScheduledMessage>) {
        if (scheduledMessages.isNotEmpty()) {
            // Mark messages for this thread as read
            val intent = Intent(context, NotificationListener::class.java)
            intent.putExtra(NotificationListener.CLEAR_TEXT_NOTIFICATIONS, true)
            context.startService(intent)
        }
        scheduledMessages.forEach {
            try {
                it.sent = true
                apiService.updateScheduledMessage(it._id, it).execute()
                MessageSender.sendMessage(it, context)
            } catch (e: Exception) {
                Log.d(TAG, "Error deleting scheduled message ${it._id}")
                Crashlytics.logException(e)
            }
        }
    }

}
