package com.octopusbeach.textto.utils

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Log
import com.octopusbeach.textto.api.ApiClient
import com.octopusbeach.textto.api.MessageEndpointInterface
import com.octopusbeach.textto.model.Message
import java.util.*
import java.util.concurrent.Executors

/**
 * Handle sending and saving sms messages
 */
object MessageUtils {
    private val TAG = "MessageUtils"
    private var BODY_INDEX = 0
    private var DATE_INDEX = 0
    private var ADDRESS_INDEX = 0
    private var ID_INDEX = 0
    private var THREAD_ID_INDEX = 0
    private var TYPE_INDEX = 0
    private var DATE_SENT_INDEX = 0
    private val client = ApiClient.getInstance().create(MessageEndpointInterface::class.java)
    private val executer = Executors.newSingleThreadExecutor()

    /**
     * Sends all scheduled Messages, then syncs messages
     */
    fun updateMessages(context: Context) {
        // only have one update task going at one time (don't double send messages)
        executer.execute {
            try {
                syncScheduledMessages()
                val id = client.getLastId().execute().body()["id"]
                if (id != null)
                    syncToMessageId(id, context)
            } catch (e: Exception) {
                Log.e(TAG, "Error updating messages: $e")
            }
        }
    }

    /**
     * Returns a sms object for a cursor, or null if message is malformed
     */
    private fun getSmsForCursor(cur: Cursor): Message? {
        val status: String
        if (cur.getInt(TYPE_INDEX) == 1) {
            status = "received"
        } else {
            status = "sent"
        }
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
                name = "",
                address = addr
        )
        return msg
    }

    /**
     * Send all scheduled messages
     */
    private fun syncScheduledMessages() {
        val messages = client.getScheduledMessages().execute().body()["scheduledMessages"]
        if (messages != null) {
            val manager = SmsManager.getDefault()
            messages.forEach {
                try {
                    client.deleteScheduledMessage(it._id).execute()
                } catch (e: Exception) {
                    Log.d(TAG, "Error deleting scheduled message ${it._id}")
                }
                // make sure to break up body if it is too long
                if (it.body.length > 160 ) {
                    val smsParts = manager.divideMessage(it.body)
                    manager.sendMultipartTextMessage(it.address, null, smsParts, null, null)
                } else
                    manager.sendTextMessage(it.address, null, it.body, null, null)
            }
        }
    }

    /**
     * Will push sms up to last id to server
     */
    private fun syncToMessageId(id: Int, context: Context) {
        val cur = context.contentResolver.query(Uri.parse("content://sms"), null, null, null, null)

        BODY_INDEX = cur.getColumnIndex("body")
        DATE_INDEX = cur.getColumnIndex("date")
        DATE_SENT_INDEX = cur.getColumnIndex("date_sent")
        THREAD_ID_INDEX = cur.getColumnIndex("thread_id")
        ID_INDEX = cur.getColumnIndex("_id")
        TYPE_INDEX = cur.getColumnIndex("type")
        ADDRESS_INDEX = cur.getColumnIndex("address")

        val threadMap = HashMap<Int, String>()

        var i = 0
        var count = cur.count
        if (count > 100) count = 400
        if (cur.moveToFirst()) {
            while (i < count && cur.getInt(0) != id) {
                val msg = getSmsForCursor(cur)
                if (msg != null) {
                    val name = threadMap.get(msg.threadId)
                    if (name == null) {
                        msg.name = getContactInfo(msg.address, context)
                        threadMap.put(msg.threadId, msg.name)
                    } else
                        msg.name = name
                    getContactInfo(msg.address, context)
                    // note this should be run on a background thread
                    postMessage(msg)
                }
                i++
                cur.moveToNext()
            }
        }
        cur.close()
    }

    private fun postMessage(msg: Message) {
        try {
            val id = client.createMessage(msg).execute()?.body()?.get("message")?.androidId
            Log.v(TAG, "Posted Message: $id")
        } catch (e: Exception) {
            Log.e(TAG, "Error posting message: $e")
        }
    }

    /**
     *  Just gets contact name for now
     */
    private fun getContactInfo(address: String, context: Context): String {
        var name: String = ""
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(address))
        val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME, ContactsContract.PhoneLookup._ID), null, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst())
                name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
        }
        cursor?.close()
        return name
    }
}