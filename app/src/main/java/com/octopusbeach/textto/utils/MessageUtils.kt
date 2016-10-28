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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.Executors

/**
 * Created by hudson on 9/6/16.
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
    private var lastId = 0
    private val executer = Executors.newSingleThreadExecutor()
    private var updating = false

    fun sendScheduledMessage(id: String) {
        if (!updating) {
            updating = true
            executer.execute {
                try {
                    // get our message
                    val msg = client.getScheduledMessage(id).execute().body()["scheduledMessage"]
                    // now delete the message.
                    client.deleteScheduledMessage(id).execute()
                    if (msg != null) {
                        SmsManager.getDefault().sendTextMessage(msg.address, null, msg.body, null, null)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                } finally {
                    updating = false
                }
            }
        }
    }

    fun getSmsForCursor(cur: Cursor): Message? {
        val status: String
        if (cur.getInt(TYPE_INDEX) == 1) {
            status = "received"
        } else {
            Log.e(TAG, cur.getString(DATE_SENT_INDEX))
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

    fun updateMessages(context: Context) {
        if (!updating) {
            updating = true
            executer.execute {
                try {
                    // update scheduled messages
                    val manager = SmsManager.getDefault()
                    val messages = client.getScheduledMessages().execute().body()["scheduledMessages"]
                    messages?.forEach {
                        try {
                            client.deleteScheduledMessage(it._id).execute()
                        } catch (e: Exception) {
                            Log.d(TAG, "Error deleting scheduled message ${it._id}")
                        }
                        manager.sendTextMessage(it.address, null, it.body, null, null)
                    }

                    // post messages
                    val id = client.getLastId().execute().body()["id"]
                    if (id != null) {
                        syncToLastestMessage(id, context)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                } finally {
                    updating = false
                }
            }
        }
    }

    private fun syncToLastestMessage(lastId: Int, context: Context) {
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
            while (i < count && cur.getInt(0) != lastId) {
                val msg = getSmsForCursor(cur)
                if (msg != null) {
                    val name = threadMap.get(msg.threadId)
                    if (name == null) {
                        msg.name = getContactInfo(msg.address, context)
                        threadMap.put(msg.threadId, msg.name)
                    } else
                        msg.name = name
                    getContactInfo(msg.address, context)
                    postMessage(msg)
                }
                i++
                cur.moveToNext()
            }
        }
        cur.close()
    }

    private fun postMessage(msg: Message) {
        if (msg.androidId == lastId) return
        client.createMessage(msg).enqueue(object: Callback<Map<String,   Message>> {
            override fun onResponse(call: Call<Map<String, Message>>?, response: Response<Map<String, Message>>?) {
                Log.d(TAG, "Posted message: ${response?.body()?.get("message")?.body ?: "null"}")
                lastId = msg.androidId!!
            }

            override fun onFailure(call: Call<Map<String, Message>>?, t: Throwable?) {
                Log.e(TAG, t.toString())
            }
        })
    }

    fun getContactInfo(address: String, context: Context): String {
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