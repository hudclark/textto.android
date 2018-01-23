package com.moduloapps.textto.message

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.MAX_MESSAGES_PER_REQUEST
import com.moduloapps.textto.api.MAX_MMS_PARTS_PER_REQUEST
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.utils.forEach
import com.moduloapps.textto.utils.whileUnder
import java.util.*

/**
 * Created by hudson on 8/10/17.
 */
object MessageController {

    private val TAG = "MessageController"

    fun syncRecentThreads(context: Context, apiService: ApiService, messagesPerThread: Int) {
        val threads = getTwentyRecentThreads(context)
        val messages = ArrayList<Message>()

        threads.forEach {
            messages.addAll(getMessagesForThread(context, it, messagesPerThread))
            if (messages.size > MAX_MESSAGES_PER_REQUEST) {
                postMessages(messages, context, apiService)
                messages.clear()
            }

        }

        // Leftovers.
        if (messages.isNotEmpty()) {
            postMessages(messages, context, apiService)
        }
    }

    private fun getMessagesForThread(context: Context, threadId: Int, limit: Int): ArrayList<Message> {
        val uri = Uri.parse("content://mms-sms/conversations/$threadId?simple=true")
        val projection = arrayOf("_id", "type", "date")
        val cur = context.contentResolver.query(uri, projection, null, null, "_id DESC LIMIT $limit")
        val messages = ArrayList<Message>()

        cur.forEach {
            val contentType = cur.getString(cur.getColumnIndex("type"))
            val id = cur.getInt(cur.getColumnIndex("_id"))
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
        val cur = context.contentResolver.query(uri, threadIdProjection, null, null, "${Telephony.Threads.DATE} desc")
        val threads = ArrayList<Int>(15)

        cur.whileUnder(15, {
            val id = it.getInt(it.getColumnIndex("_id"))
            threads.add(id)
        })

        cur.close()
        return threads
    }

    fun postMessages(messages: List<Message>, context: Context, apiService: ApiService) {

        // post messages
        apiService.createMessages(messages).execute()

        // post mms parts
        messages.filter { it.type == "mms" }
                .flatMap { Mms.getPartsForMms(it.androidId, context) }
                .chunked(MAX_MMS_PARTS_PER_REQUEST)
                .forEach { Mms.postParts(it, apiService, context) }
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