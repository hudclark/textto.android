package com.moduloapps.textto.message

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.support.v4.content.ContextCompat
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.MAX_MESSAGES_PER_REQUEST
import com.moduloapps.textto.api.MAX_MMS_PARTS_PER_REQUEST
import com.moduloapps.textto.encryption.EncryptionHelper
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import com.moduloapps.textto.utils.forEach
import com.moduloapps.textto.utils.whileUnder
import java.util.*

/**
 * Created by hudson on 8/10/17.
 */
object MessageController {

    private val TAG = "MessageController"

    fun syncRecentThreads(context: Application, apiService: ApiService, numberOfThreads: Int, messagesPerThread: Int) {
        val threads = getRecentThreads(numberOfThreads, context)
        syncThreads(threads, context, apiService, messagesPerThread)
    }

    fun syncThreads (threads: List<Int>, context: Application, apiService: ApiService, messagesPerThread: Int) {

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

    fun getRecentThreads(numberOfThreads: Int, context: Context): List<Int> {
        val threadIdProjection = arrayOf(Telephony.Threads._ID, Telephony.Threads.DATE)
        val uri = Uri.parse("content://mms-sms/conversations?simple=true")
        val cur = context.contentResolver.query(uri, threadIdProjection, null, null, "${Telephony.Threads.DATE} desc")
        val threads = ArrayList<Int>(numberOfThreads)

        cur.whileUnder(numberOfThreads, {
            val id = it.getInt(it.getColumnIndex("_id"))
            threads.add(id)
        })

        cur.close()
        return threads
    }

    fun postMessages(messages: List<Message>, context: Application, apiService: ApiService) {
        val encryptionHelper = (context as BaseApplication).appComponent.getEncryptionHelper()

        if (encryptionHelper.enabled()) {
            encryptMessages(messages, encryptionHelper)
        }

        // post messages
        apiService.createMessages(messages).execute()

        // post mms parts
        messages.filter { it.type == "mms" }
                .flatMap { Mms.getPartsForMms(it.androidId, context) }
                .let { // encrypt
                    if (encryptionHelper.enabled()) {
                        encryptMmsParts(it, encryptionHelper)
                    }
                    it
                }
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

    fun encryptMessages (messages: List<Message>, encryptionHelper: EncryptionHelper) {
        messages.forEach {
            try {
                it.encrypt(encryptionHelper)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }

    fun encryptMmsParts (parts: List<MmsPart>, encryptionHelper: EncryptionHelper) {
        parts.forEach {
            try {
                it.encrypt(encryptionHelper)
            } catch (e: Exception) {
                Log.e(TAG, e.toString())
            }
        }
    }



}