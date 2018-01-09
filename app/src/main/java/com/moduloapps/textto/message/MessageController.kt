package com.moduloapps.textto.message

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import com.moduloapps.textto.utils.forEach
import java.util.*

/**
 * Created by hudson on 8/10/17.
 */
object MessageController {

    private val TAG = "MessageController"
    private val MAX_MESSAGES_FOR_REQUEST = 400
    private val MAX_PARTS_FOR_REQUEST = 150

    fun syncRecentThreads(context: Context, apiService: ApiService, messagesPerThread: Int) {
        val threads = getTwentyRecentThreads(context)

        val messages = ArrayList<Message>()
        val parts = ArrayList<MmsPart>()

        threads.forEach {
            val threadMessages = getMessagesForThread(context, it, messagesPerThread)
            threadMessages.filter { it.type == "mms" }
                    .forEach {
                        parts.addAll(Mms.getPartsForMms(it.androidId, context))
                    }

            messages.addAll(threadMessages)

            if (messages.size > MAX_MESSAGES_FOR_REQUEST) {
                apiService.createMessages(messages).execute()
                messages.clear()
            }

            if (parts.size > MAX_PARTS_FOR_REQUEST) {
                Mms.postParts(parts, apiService, context)
                parts.clear()
            }
        }

        if (messages.isNotEmpty()) {
            apiService.createMessages(messages).execute()
        }

        if (parts.isNotEmpty()) {
            Mms.postParts(parts, apiService, context)
        }

    }

    private fun getMessagesForThread(context: Context, threadId: Int, limit: Int): ArrayList<Message> {
        val uri = Uri.parse("content://mms-sms/conversations/$threadId?simple=true")
        val projection = arrayOf("_id", "type", "date")
        val cur = context.contentResolver.query(uri, projection, null, null, "date DESC LIMIT $limit")
        val messages = ArrayList<Message>()

        cur.forEach {
            val contentType = cur.getString(cur.getColumnIndex("type"))
            val id = cur.getInt(cur.getColumnIndex("_id"))
            Log.e("TEST", "Found message with id $id")
            if (contentType == null) {
                Mms.getMmsForId(context, id)?.let { messages.add(it) }
            } else {
                Sms.getSmsForId(context, id)?.let { messages.add(it) }
            }
        }

        cur.close()

        return messages
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
                parts.addAll(Mms.getPartsForMms(it.androidId, context))
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

    fun getSimNumber(context: Context): String? {
        if (ContextCompat.checkSelfPermission(context, Context.TELECOM_SERVICE) == PackageManager.PERMISSION_GRANTED) {
            return (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).line1Number
        }
        return null
    }


}