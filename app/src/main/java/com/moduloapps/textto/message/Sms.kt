package com.moduloapps.textto.message

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.utils.withFirst

/**
 * Created by hudson on 8/6/17.
 */
object Sms {

    private val MAX_MESSAGES = 400

    /**
     * Will push sms up to last id to server
     */
    fun syncSms(date: Long, id: Int, context: Context, apiService: ApiService) {
        val selection = "_id > $id"
        val cur = context.contentResolver.query(Uri.parse("content://sms"), null, selection, null, null)
        var counter = 0
        val messageCount = Math.min(cur.count, 1000)
        val messages = ArrayList<Message>()
        if (cur.moveToFirst()) {
            while (counter < messageCount) {
                getSmsForCursor(cur)?.let {
                    if (messages.size > MAX_MESSAGES) {
                        MessageController.postMessages(messages, context, apiService)
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
            MessageController.postMessages(messages, context, apiService)
        }
    }

    fun getSmsForId(context: Context, id: Int): Message? {
        var message: Message? = null
        val cur = context.contentResolver.query(Uri.parse("content://sms"), null, "_id=$id", null, null)
        cur.withFirst {
            message = getSmsForCursor(cur)
        }
        cur.close()
        return message
    }

    private fun getSmsForCursor(cur: Cursor): Message? {
        val addr = cur.getString(cur.getColumnIndex(Telephony.Sms.ADDRESS))
        val sender = if (cur.getInt(cur.getColumnIndex(Telephony.Sms.TYPE)) == 1) addr else "me"
        val body = cur.getString(cur.getColumnIndex(Telephony.Sms.BODY))
        if (addr == null || body == null)
            return null
        return Message(
                androidId = cur.getInt(cur.getColumnIndex("_id")),
                threadId = cur.getInt(cur.getColumnIndex(Telephony.Sms.THREAD_ID)),
                body = body,
                sender = sender,
                date = cur.getLong(cur.getColumnIndex(Telephony.Sms.DATE)),
                addresses = arrayListOf(addr),
                type = "sms")
    }

}