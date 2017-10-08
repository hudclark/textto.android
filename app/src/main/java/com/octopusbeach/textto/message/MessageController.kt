package com.octopusbeach.textto.message

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Telephony
import android.support.v4.content.FileProvider
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.model.Message
import com.octopusbeach.textto.model.MmsPart
import com.octopusbeach.textto.service.DeliveryBroadcastReceiver
import java.io.File
import java.io.FileOutputStream
import java.util.*

/**
 * Created by hudson on 8/10/17.
 */
object MessageController {

    private val TAG = "MessageController"

    private val FILE_PROVIDER = "com.octopusbeach.fileprovider"
    private val TEXT_PLAIN = "text/plain"
    private val EXPIRY_TIME: Long = 7 * 24 * 60 * 60
    private val PRIORITY = 0x81
    private val VALUE_NO = 0x81

    private val smilText =
            "<smil>" +
                    "<head>" +
                    "<layout>" +
                    "<root-layout/>" +
                    "<region height=\"100%%\" id=\"Text\" left=\"0%%\" top=\"0%%\" width=\"100%%\"/>" +
                    "</layout>" +
                    "</head>" +
                    "<body>" +
                    "<par dur=\"8000ms\">" +
                    "<text src=\"%s\" region=\"Text\"/>" +
                    "</par>" +
                    "</body>" +
                    "</smil>"

    fun sendMessage(text: String?, recipients: Array<String>, context: Context, messageId: String) {
        // TODO right now images are not allowed
        if (text == null || recipients.isEmpty()) return
        if (recipients.size > 1 || text.length > 160) {
            sendMmsMessage(text, recipients, context, messageId)
        } else {
            sendSmsMessage(text, recipients[0], context, messageId)
        }
    }

    private fun sendSmsMessage(text: String, recipient: String, context: Context, messageId: String) {
        Log.d(TAG, "Sending sms message to $recipient...")
        val manager = SmsManager.getDefault()
        val intent = Intent(context, DeliveryBroadcastReceiver::class.java)
        intent.putExtra(DeliveryBroadcastReceiver.MESSAGE_ID, messageId)
        val pendingIntent = PendingIntent.getBroadcast(context, messageId.toInt(), intent, 0)
        manager.sendTextMessage(recipient, null, text, pendingIntent, null)
    }

    private fun sendMmsMessage(text: String, recipients: Array<String>, context: Context, messageId: String) {
        if (Build.VERSION.SDK_INT < 21) return
        Log.d(TAG, "Sending mms message to $recipients...")
        val pdu = buildPdu(context, recipients, text)
        val filename = "text_$messageId.txt"
        val file = File(context.cacheDir, filename) // FILE_NAME should be unique
        if (!file.exists()) {
            file.createNewFile()
        }
        try {
            val stream = FileOutputStream(file)
            stream.write(pdu)
            stream.close()
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER, file)

            val intent = Intent(context, DeliveryBroadcastReceiver::class.java)
            intent.putExtra(DeliveryBroadcastReceiver.MESSAGE_ID, messageId)
            intent.putExtra(DeliveryBroadcastReceiver.FILENAME, filename)
            val pendingIntent = PendingIntent.getBroadcast(context, messageId.toInt(), intent, 0)

            SmsManager.getDefault().sendMultimediaMessage(context, uri, null, null, pendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing mms to file", e)
        }
    }

    private fun buildPdu(context: Context, recipients: Array<String>, text: String): ByteArray {

        val SendReq = Class.forName("com.google.android.mms.pdu.SendReq")
        val EncodedStringValue = Class.forName("com.google.android.mms.pdu.EncodedStringValue")
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduComposer = Class.forName("com.google.android.mms.pdu.PduComposer")

        val sendReq = SendReq.newInstance()

        // set from
        val sim = null//getSimNumber(context)  // For whatever reason this is different than what textra does.
        if (!TextUtils.isEmpty(sim)) {
            val encodedSim = EncodedStringValue.getConstructor(String::class.java).newInstance("+$sim")
            SendReq.getMethod("setFrom", encodedSim::class.java).invoke(sendReq, encodedSim)
        }

        // set to
        val encodedNumbers = EncodedStringValue.getDeclaredMethod("encodeStrings", Array<String>::class.java).invoke(null, recipients)
        if (encodedNumbers != null) {
            SendReq.getDeclaredMethod("setTo", encodedNumbers::class.java).invoke(sendReq, encodedNumbers)
        }

        // date
        SendReq.getMethod("setDate", Long::class.java).invoke(sendReq, System.currentTimeMillis() / 1000)

        // body
        val pduBody = PduBody.newInstance()

        // add text part
        val size = addTextPart(pduBody, text, false)
        SendReq.getMethod("setBody", pduBody::class.java).invoke(sendReq, pduBody)

        // set message size
        SendReq.getDeclaredMethod("setMessageSize", Long::class.java).invoke(sendReq, size)

        // set message class
        SendReq.getDeclaredMethod("setMessageClass", ByteArray::class.java).invoke(sendReq, "personal".toByteArray())

        // expiry
        SendReq.getDeclaredMethod("setExpiry", Long::class.java).invoke(sendReq, EXPIRY_TIME)

        try {
            // priority
            SendReq.getMethod("setPriority", Int::class.java).invoke(sendReq, PRIORITY)
            SendReq.getDeclaredMethod("setDeliveryReport", Int::class.java).invoke(sendReq, VALUE_NO)
            SendReq.getDeclaredMethod("setReadReport", Int::class.java).invoke(sendReq, VALUE_NO)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting properties on sendReq", e)
        }

        val pduComposer = PduComposer.getConstructor(Context::class.java, Class.forName("com.google.android.mms.pdu.GenericPdu")).newInstance(context, sendReq)
        return PduComposer.getDeclaredMethod("make").invoke(pduComposer) as ByteArray

    }

    fun addTextPart(pduBody: Any /*PdyBody*/, text: String, includeSmil: Boolean): Int {
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduPart = Class.forName("com.google.android.mms.pdu.PduPart")

        val pduPart = PduPart.newInstance()
        // set char set
        val utf8 = 106
        PduPart.getDeclaredMethod("setCharset", utf8::class.java).invoke(pduPart, utf8)

        // set content type
        PduPart.getDeclaredMethod("setContentType", ByteArray::class.java).invoke(pduPart, TEXT_PLAIN.toByteArray())

        // set content location
        PduPart.getDeclaredMethod("setContentLocation", ByteArray::class.java).invoke(pduPart, "text_0.txt".toByteArray())
        PduPart.getDeclaredMethod("setContentId", ByteArray::class.java).invoke(pduPart, "text_0".toByteArray())

        // set data
        PduPart.getDeclaredMethod("setData", ByteArray::class.java).invoke(pduPart, text.toByteArray())
        PduBody.getDeclaredMethod("addPart", pduPart::class.java).invoke(pduBody, pduPart)

        if (includeSmil) {
            val smil = String.format(smilText, "text_0.txt")
            addSmil(pduBody, smil)
        }
        val byteArray = PduPart.getDeclaredMethod("getData").invoke(pduPart) as ByteArray
        return byteArray.size
    }

    fun addSmil(pduBody: Any, smil: String) {
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduPart = Class.forName("com.google.android.mms.pdu.PduPart")
        val pduPart = PduPart.newInstance()
        PduPart.getDeclaredMethod("setContentId", ByteArray::class.java).invoke(pduPart, "smil".toByteArray())
        PduPart.getDeclaredMethod("setContentLocation", ByteArray::class.java).invoke(pduPart, "smil.xml".toByteArray())
        PduPart.getDeclaredMethod("setContentType", ByteArray::class.java).invoke(pduPart, "application/smil".toByteArray())
        PduPart.getDeclaredMethod("setData", ByteArray::class.java).invoke(pduPart, smil.toByteArray())
        PduBody.getDeclaredMethod("addPart", pduPart::class.java).invoke(pduBody, pduPart)
    }

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

}