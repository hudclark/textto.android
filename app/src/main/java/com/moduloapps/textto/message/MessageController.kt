package com.moduloapps.textto.message

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import java.util.*

/**
 * Created by hudson on 8/10/17.
 */
object MessageController {

    private val TAG = "MessageController"

    fun syncRecentThreads(context: Context, apiService: ApiService, messagesPerThread: Int) {
        val threads = getTwentyRecentThreads(context)
        threads.forEach {
            syncMessagesForThread(context, apiService, it, messagesPerThread)
        }
    }

    private fun syncMessagesForThread(context: Context, apiService: ApiService, threadId: Int, limit: Int) {
        val uri = Uri.parse("content://mms-sms/conversations/$threadId?simple=true")
        val projection = arrayOf("_id", "type", "date")
        val cur = context.contentResolver.query(uri, projection, null, null, "date DESC LIMIT $limit")
        val messages = ArrayList<Message>()
        if (cur.moveToFirst()) {
            do {
                val contentType = cur.getString(cur.getColumnIndex("type"))
                val id = cur.getInt(cur.getColumnIndex("_id"))
                if (contentType == null) {
                    Mms.getMmsForId(context, id)?.let { messages.add(it) }
                } else {
                    Sms.getSmsForId(context, id)?.let { messages.add(it) }
                }
                if (messages.size > 100) {
                    postMessages(messages, context, apiService)
                }
            } while (cur.moveToNext())
        }
        cur.close()

        if (messages.isNotEmpty()) {
            postMessages(messages, context, apiService)
        }
    }

    private fun getTwentyRecentThreads(context: Context): List<Int> {
        val threadIdProjection = arrayOf(Telephony.Threads._ID, Telephony.Threads.DATE)
        val uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val cur = context.contentResolver.query(uri, threadIdProjection, null, null, null)
        val threads = ArrayList<Int>(15)
        var counter = 0
        if (cur.moveToFirst()) {
            do {
                val id = cur.getInt(cur.getColumnIndex("_id"))
                threads.add(id)
                counter++
            } while (cur.moveToNext() && counter < 15)
        }
        cur.close()
        return threads
    }

    fun postMessages(messages: List<Message>, context: Context, apiService: ApiService) {
        apiService.createMessages(messages).execute()
        val parts = ArrayList<MmsPart>()
        messages.forEach {
            if (it.type == "mms") {
                parts.addAll(Mms.getPartsForMessage(it, context))
                if (parts.size > 10) {
                    Mms.postParts(parts, apiService, context)
                    parts.clear()
                }
            }
        }
        if (parts.isNotEmpty()) {
            Mms.postParts(parts, apiService, context)
        }
    }

    fun isMyAddress (address: String, context: Context): Boolean {
        if (address == "insert-address-token") return true
        val result = PhoneNumberUtil.getInstance().isNumberMatch(getSimNumber(context), address)
        return (result == PhoneNumberUtil.MatchType.EXACT_MATCH ||
                result == PhoneNumberUtil.MatchType.NSN_MATCH)
    }

    fun getSimNumber(context: Context) =
            (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).line1Number


}