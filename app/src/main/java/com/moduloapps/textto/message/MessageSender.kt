package com.moduloapps.textto.message

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.content.FileProvider
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.ScheduledMessage
import com.moduloapps.textto.service.DeliveryBroadcastReceiver
import com.moduloapps.textto.utils.ImageUtils
import com.moduloapps.textto.utils.ThreadUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Created by hudson on 10/11/17.
 */
object MessageSender {

    private val TAG = "MessageSender"

    private val FILE_PROVIDER = "com.moduloapps.fileprovider"
    private val TEXT_PLAIN = "text/plain"
    private val EXPIRY_TIME: Long = 7 * 24 * 60 * 60
    private val PRIORITY = 0x81
    private val VALUE_NO = 0x81

    private val MAX_MMS_IMAGE_SIZE = 300 * 1024 // 500kb

    fun sendMessage(scheduledMessage: ScheduledMessage, context: BaseApplication) {


        // Nothing to send
        if (scheduledMessage.addresses.isEmpty()) {
            return
        }

        val textContent = scheduledMessage.body
        val fileUrl = scheduledMessage.fileUrl

        // Nothing to send
        if (fileUrl == null && textContent == null) return

        val isMms = fileUrl != null || scheduledMessage.addresses.size > 1 || scheduledMessage.body?.length ?: 0 > 160

        // analytics
        val type = if (isMms) "mms" else "sms"
        Answers.getInstance().logCustom(CustomEvent("Send Message")
                .putCustomAttribute("type", type))

        if (isMms) {
            sendMmsMessage(scheduledMessage, context)
        } else {
            sendSmsMessage(scheduledMessage, context)
        }
    }

    private fun sendSmsMessage(scheduledMessage: ScheduledMessage, context: Context) {
        val recipient = scheduledMessage.addresses[0]
        Log.d(TAG, "Sending sms message to $recipient...")
        val intent = Intent(context, DeliveryBroadcastReceiver::class.java)
        intent.putExtra(DeliveryBroadcastReceiver.MESSAGE_ID, scheduledMessage._id)
        val pendingIntent = PendingIntent.getBroadcast(context, scheduledMessage._id.toInt(), intent, 0)

        SmsManager.getDefault().sendTextMessage(recipient, null, scheduledMessage.body, pendingIntent, null)
    }

    private fun sendMmsMessage(scheduledMessage: ScheduledMessage, context: BaseApplication) {
        if (Build.VERSION.SDK_INT < 21) return
        Log.d(TAG, "Sending mms message to ${scheduledMessage.addresses.joinToString(",")}...")
        val filename = "text_${scheduledMessage._id}.txt"
        try {
            val pdu = buildPdu(scheduledMessage, filename, context)
            val file = File(context.cacheDir, filename) // FILE_NAME should be unique
            if (!file.exists()) {
                file.createNewFile()
            }
            val stream = FileOutputStream(file)
            stream.write(pdu)
            stream.close()
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER, file)

            val intent = Intent(context, DeliveryBroadcastReceiver::class.java)
            intent.putExtra(DeliveryBroadcastReceiver.MESSAGE_ID, scheduledMessage._id)
            intent.putExtra(DeliveryBroadcastReceiver.FILENAME, filename)
            val pendingIntent = PendingIntent.getBroadcast(context, scheduledMessage._id.toInt(), intent, 0)

            SmsManager.getDefault().sendMultimediaMessage(context, uri, null, null, pendingIntent)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error writing mms to file", e)
        }
    }

    private fun buildPdu(scheduledMessage: ScheduledMessage, filename: String, context: BaseApplication): ByteArray {

        val SendReq = Class.forName("com.google.android.mms.pdu.SendReq")
        val EncodedStringValue = Class.forName("com.google.android.mms.pdu.EncodedStringValue")
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduComposer = Class.forName("com.google.android.mms.pdu.PduComposer")

        val sendReq = SendReq.newInstance()

        // set from
        val sim = MessageController.getSimNumber(context)
        if (!TextUtils.isEmpty(sim)) {
            val encodedSim = EncodedStringValue.getConstructor(String::class.java).newInstance(sim)
            SendReq.getMethod("setFrom", encodedSim::class.java).invoke(sendReq, encodedSim)
        }

        // set to
        val encodedNumbers = EncodedStringValue.getDeclaredMethod("encodeStrings", Array<String>::class.java).invoke(null, scheduledMessage.addresses)
        if (encodedNumbers != null) {
            SendReq.getDeclaredMethod("setTo", encodedNumbers::class.java).invoke(sendReq, encodedNumbers)
        }

        // date
        SendReq.getMethod("setDate", Long::class.java).invoke(sendReq, System.currentTimeMillis() / 1000)

        // body
        val pduBody = PduBody.newInstance()

        // message size
        var size = 0

        // add text part
        if (!TextUtils.isEmpty(scheduledMessage.body)) {
            val text = scheduledMessage.body as String
            size = addTextPart(pduBody, text)
        }
        // Add file. NOTE: cannot currently send multi part messages
        else if (!TextUtils.isEmpty(scheduledMessage.fileUrl)) {
            val url = scheduledMessage.fileUrl as String
            size = addFilePart(pduBody, url, context)
        }
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
            Crashlytics.logException(e)
            Log.e(TAG, "Error setting properties on sendReq", e)
        }

        val pduComposer = PduComposer.getConstructor(Context::class.java, Class.forName("com.google.android.mms.pdu.GenericPdu")).newInstance(context, sendReq)
        return PduComposer.getDeclaredMethod("make").invoke(pduComposer) as ByteArray

    }

    private fun addFilePart (pduBody: Any, url: String, context: BaseApplication): Int {

        // Fetch image
        Log.d(TAG, "Fetching file...")
        val response = context.appComponent.getApiService().getFile(url).execute().body()
        Log.d(TAG, "Successfully fetched file")

        val contentType = response.contentType().toString()
        if (!(contentType == "image/jpeg" || contentType == "image/png" || contentType == "image/jpg" || contentType == "gif")) {
            Log.e(TAG, "Invalid file type for mms")
            throw IllegalArgumentException("Invalid filetype")
        }

        Log.d(TAG, "Sending image with contentType $contentType")

        // Begin pdu
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduPart = Class.forName("com.google.android.mms.pdu.PduPart")
        val pduPart = PduPart.newInstance()

        val filename = "image_${System.currentTimeMillis()}"
        val fileNameBytes = filename.toByteArray()
        val fileBytes =
            if (contentType == "image/gif")
                response.bytes()
            else
                ImageUtils.compressImage(response.byteStream(), MAX_MMS_IMAGE_SIZE)

        // set content type
        PduPart.getDeclaredMethod("setContentType", ByteArray::class.java).invoke(pduPart, contentType.toByteArray())

        // set content location
        PduPart.getDeclaredMethod("setContentLocation", ByteArray::class.java).invoke(pduPart, fileNameBytes)
        PduPart.getDeclaredMethod("setContentId", ByteArray::class.java).invoke(pduPart, fileNameBytes)

        // set data
        PduPart.getDeclaredMethod("setData", ByteArray::class.java).invoke(pduPart, fileBytes)
        PduBody.getDeclaredMethod("addPart", pduPart::class.java).invoke(pduBody, pduPart)

        // add smil
        addSmil(pduBody, getSmilText(contentType, filename))

        // get length
        val byteArray = PduPart.getDeclaredMethod("getData").invoke(pduPart) as ByteArray

        return byteArray.size + fileNameBytes.size
    }

    private fun addTextPart(pduBody: Any /*PdyBody*/, text: String): Int {
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

        addSmil(pduBody, getSmilText("text/plain", "text_0.txt"))
        val byteArray = PduPart.getDeclaredMethod("getData").invoke(pduPart) as ByteArray
        return byteArray.size
    }


    private fun addSmil(pduBody: Any, smil: String) {
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduPart = Class.forName("com.google.android.mms.pdu.PduPart")
        val pduPart = PduPart.newInstance()
        val utf8 = 106
        PduPart.getDeclaredMethod("setCharset", utf8::class.java).invoke(pduPart, utf8)
        PduPart.getDeclaredMethod("setContentId", ByteArray::class.java).invoke(pduPart, "smil".toByteArray())
        PduPart.getDeclaredMethod("setContentLocation", ByteArray::class.java).invoke(pduPart, "smil.xml".toByteArray())
        PduPart.getDeclaredMethod("setContentType", ByteArray::class.java).invoke(pduPart, "application/smil".toByteArray())
        PduPart.getDeclaredMethod("setData", ByteArray::class.java).invoke(pduPart, smil.toByteArray())
        PduBody.getDeclaredMethod("addPart", pduPart::class.java).invoke(pduBody, pduPart)
    }

    private fun getSmilText(type: String, src: String): String {
        var smil = smilOpen
        if (type.contains("image")) {
            smil += "<img src=\"" + src + "\" region=\"Text\"/>"
        }
        smil += "</par></body></smil>"
        Log.d(TAG, smil)
        return smil
    }

    private val smilOpen =
            "<smil>" +
                    "<head>" +
                    "<layout>" +
                    "<root-layout/>" +
                    "<region height=\"100%%\" id=\"Text\" left=\"0%%\" top=\"0%%\" width=\"100%%\"/>" +
                    "</layout>" +
                    "</head>" +
                    "<body>" +
                    "<par dur=\"8000ms\">" +
                    "<text src=\"text_0.txt\" region=\"Text\"/>"

}