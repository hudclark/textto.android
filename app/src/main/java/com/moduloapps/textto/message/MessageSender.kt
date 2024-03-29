package com.moduloapps.textto.message

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.FileProvider
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Base64
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.message.MmsPdu.Companion.MAX_MMS_IMAGE_SIZE
import com.moduloapps.textto.model.ScheduledMessage
import com.moduloapps.textto.utils.ImageUtils
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Created by hudson on 10/11/17.
 */
object MessageSender {

    private val TAG = "MessageSender"

    private val FILE_PROVIDER = "com.moduloapps.fileprovider"

    fun sendMessage(scheduledMessage: ScheduledMessage, context: BaseApplication) {

        // Nothing to send
        if (scheduledMessage.addresses.isEmpty()) {
            return
        }

        val textContent = scheduledMessage.body
        val fileUrl = scheduledMessage.fileUrl

        // Nothing to send
        if (fileUrl == null && textContent == null) return

        // TODO change this
        val isMms = fileUrl != null || scheduledMessage.addresses.size > 1 || (SmsManager.getDefault().divideMessage(scheduledMessage.body ?: "").size > 1)

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
        val intent = Intent(context, MessageSentReceiver::class.java)
        intent.putExtra(MessageSentReceiver.MESSAGE_ID, scheduledMessage._id)
        val pendingIntent = PendingIntent.getBroadcast(context, getUniqueRequestCode(scheduledMessage), intent, 0)

        SmsManager.getDefault().sendTextMessage(recipient, null, scheduledMessage.body, pendingIntent, null)
    }

    private fun sendMmsMessage(scheduledMessage: ScheduledMessage, context: BaseApplication) {
        Log.d(TAG, "Sending mms message to ${scheduledMessage.addresses.joinToString(",")}...")
        val filename = "text_${scheduledMessage._id}.txt"
        try {

            val pdu = MmsPdu(scheduledMessage.addresses)

            // add text part
            if (!TextUtils.isEmpty(scheduledMessage.body)) {
                pdu.addText(scheduledMessage.body as String)
            }
            // Add file. NOTE: cannot currently send multi part messages
            else if (!TextUtils.isEmpty(scheduledMessage.fileUrl)) {
                val image = getMmsImage(scheduledMessage.fileUrl as String, context, scheduledMessage.encrypted)
                pdu.addImage(image)
            }

            val file = File(context.cacheDir, filename) // FILE_NAME should be unique
            if (!file.exists()) {
                file.createNewFile()
            }
            val stream = FileOutputStream(file)
            stream.write(pdu.build(context))
            stream.close()
            val uri = FileProvider.getUriForFile(context, FILE_PROVIDER, file)

            val intent = Intent(context, MessageSentReceiver::class.java)
            intent.putExtra(MessageSentReceiver.MESSAGE_ID, scheduledMessage._id)
            intent.putExtra(MessageSentReceiver.FILENAME, filename)
            val pendingIntent = PendingIntent.getBroadcast(context, getUniqueRequestCode(scheduledMessage), intent, 0)

            SmsManager.getDefault().sendMultimediaMessage(context, uri, null, null, pendingIntent)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error writing mms to file", e)
        }
    }

    private fun getMmsImage(url: String, context: BaseApplication, encrypted: Boolean): MmsPdu.MmsImage {
        Log.d(TAG, "Fetching file...")
        val response = context.appComponent.getApiService().getFile(url).execute().body()
        Log.d(TAG, "Successfully fetched file")

        var contentType = response.contentType().toString()

        /*
        If the message was encrypted, decrypt the base64 data
        It is formatted as such:
        data:image/jpeg;base64,<image data>

         */

        var imageBytes: ByteArray


        if (encrypted) {
            val encryptionHelper = context.appComponent.getEncryptionHelper()
            val plain = encryptionHelper.decrypt(response.string())
            val encoded = plain.substring(plain.indexOf(",") + 1)
            imageBytes = Base64.decode(encoded, Base64.NO_WRAP)
            contentType = plain.substring(plain.indexOf(":") + 1, plain.indexOf(";"))
        }

        /*
        If the message was not encrypted, simply read the binary data from the image file.
         */
        else {
            imageBytes = response.bytes()
        }

        // Compress the image if it is not a gif
        if (contentType != "image/gif") {
            imageBytes = ImageUtils.compressImage(ByteArrayInputStream(imageBytes), MAX_MMS_IMAGE_SIZE)
        }

        return MmsPdu.MmsImage(contentType, imageBytes)
    }

    private fun getUniqueRequestCode(scheduledMessage: ScheduledMessage): Int {
        val retries = scheduledMessage.retries ?: 0
        return scheduledMessage._id.toInt() + retries
    }

}
