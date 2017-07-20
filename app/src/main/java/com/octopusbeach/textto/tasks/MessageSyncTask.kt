package com.octopusbeach.textto.tasks

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.net.Uri
import android.telephony.SmsManager
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.model.Message

/**
 * Created by hudson on 7/14/17.
 */
class MessageSyncTask(val apiService: ApiService,
                      val context: Context,
                      val prefs: SharedPreferences) : Runnable {

    private val TAG = "MessageSyncTask"

    companion object {
        val CONTACTS_LAST_SYNCED = "contacts_last_synced"
        val MESSAGES_LAST_SYNCED = "messages_last_synced"
    }

    private var BODY_INDEX = 0
    private var DATE_INDEX = 0
    private var ADDRESS_INDEX = 0
    private var ID_INDEX = 0
    private var THREAD_ID_INDEX = 0
    private var TYPE_INDEX = 0
    private var DATE_SENT_INDEX = 0

    /**
     * Will push sms up to last id to server
     */
    private fun syncToMessageId(lastId: Int) {
        val cur = context.contentResolver.query(Uri.parse("content://sms"), null, null, null, null)

        BODY_INDEX = cur.getColumnIndex("body")
        DATE_INDEX = cur.getColumnIndex("date")
        DATE_SENT_INDEX = cur.getColumnIndex("date_sent")
        THREAD_ID_INDEX = cur.getColumnIndex("thread_id")
        ID_INDEX = cur.getColumnIndex("_id")
        TYPE_INDEX = cur.getColumnIndex("type")
        ADDRESS_INDEX = cur.getColumnIndex("address")

        var counter = 0
        var messageCount = Math.min(cur.count, 200) // save up to 200 messages

        if (cur.moveToFirst()) {
            while (counter < messageCount && cur.getInt(0) != lastId) {
                getSmsForCursor(cur)?.let {
                    postMessage(it)
                }
                counter++
                cur.moveToNext()
            }
        }

        cur.close()
    }

    private fun getSmsForCursor(cur: Cursor): Message? {
        val status: String
        if (cur.getInt(TYPE_INDEX) == 1)
            status = "received"
        else
            status = "sent"
        val addr = cur.getString(ADDRESS_INDEX)
        val body = cur.getString(BODY_INDEX)
        if (addr == null || body == null)
            return null
        val msg = Message(
                androidId = cur.getInt(ID_INDEX),
                threadId = cur.getInt(THREAD_ID_INDEX),
                body = body,
                status = status,
                date = cur.getLong(DATE_INDEX),
                address = addr
        )
        return msg
    }

    private fun postMessage(message: Message) {
        // Note that this is sync.
        try {
            val id = apiService.createMessage(message).execute().body()?.get("message")?.androidId
            Log.d(TAG, "Posted message: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting message: $e")
        }
    }

    private fun syncScheduledMessages() {
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
            val id = apiService.getLastId().execute().body()["id"]
            id?.let {
                syncToMessageId(id)
            }
            syncScheduledMessages()
            prefs.edit().putLong(MESSAGES_LAST_SYNCED, System.currentTimeMillis()).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating messages: $e")
        }
    }
}
