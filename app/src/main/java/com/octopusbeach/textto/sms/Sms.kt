package com.octopusbeach.textto.sms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.model.Message

/**
 * Created by hudson on 8/6/17.
 */
object Sms {

    private val MAX_MESSAGES = 75

    /**
     * Will push sms up to last id to server
     */
    fun syncSmsToDate(date: Long, context: Context, apiService: ApiService) {
        val selection = "date > $date"
        val cur = context.contentResolver.query(Uri.parse("content://sms"), null, selection, null, null)
        var counter = 0
        val messageCount = Math.min(cur.count, 1000)
        Log.d("Sms", "$messageCount")
        val messages = ArrayList<Message>()
        if (cur.moveToFirst()) {
            while (counter < messageCount) {
                getSmsForCursor(cur)?.let {
                    if (messages.size > MAX_MESSAGES) {
                        postMessages(messages, apiService)
                        messages.clear()
                    }
                    messages.add(it)
                }
                counter++
                cur.moveToNext()
            }
        }
        cur.close()

        if (messages.isNotEmpty()) {
            postMessages(messages, apiService)
        }
    }

    private fun getSmsForCursor(cur: Cursor): Message? {
        val addr = cur.getString(cur.getColumnIndex(Telephony.Sms.ADDRESS))
        val sender = if (cur.getInt(cur.getColumnIndex(Telephony.Sms.TYPE)) == 1) addr else "me"
        val body = cur.getString(cur.getColumnIndex(Telephony.Sms.BODY))
        if (addr == null || body == null)
            return null
        val msg = Message(
                androidId = cur.getInt(cur.getColumnIndex("_id")),
                threadId = cur.getInt(cur.getColumnIndex(Telephony.Sms.THREAD_ID)),
                body = body,
                sender = sender,
                date = cur.getLong(cur.getColumnIndex(Telephony.Sms.DATE)),
                addresses = arrayListOf(addr),
                type = "sms"
        )
        return msg
    }

    private fun postMessages(messages: List<Message>, apiService: ApiService) {
        apiService.createMessages(messages).execute()
    }


}