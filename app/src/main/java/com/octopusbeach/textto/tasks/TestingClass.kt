package com.octopusbeach.textto.tasks

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.content.FileProvider
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val pendingIntent: PendingIntent) : Runnable {

    private interface LengthContainer {
        var length: Int
    }

    private val TAG = TestingClass::class.java.simpleName

    private val TEXT_PLAIN = "text/plain"
    private val UTF_8 = 106
    private val EXPIRY_TIME: Long = 7 * 24 * 60 * 60
    private val PRIORITY = 0x81
    private val VALUE_NO = 0x81

    private val FILE_NAME = "text_0.txt"

    private val sSmilText =
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

    override fun run() {

        if (Build.VERSION.SDK_INT > 20) {
            val pdu = buildPdu(context, arrayOf("(512) 991-4096", "(512) 763-5885"), "test")

            val file = File(context.cacheDir, FILE_NAME)
            if (!file.exists()) {
                file.createNewFile()
            }
            try {
                val stream = FileOutputStream(file)
                stream.write(pdu)
                stream.close()
                val uri = FileProvider.getUriForFile(context, "com.octopusbeach.fileprovider", file)
                Log.d(TAG, uri.encodedPath)
                SmsManager.getDefault().sendMultimediaMessage(context, uri, null, null, pendingIntent)
            } catch (e: Exception) {
                Log.e(TAG, "2", e)
            }
        }
    }


    fun buildPdu(context: Context, recipients: Array<String>, text: String): ByteArray {

        val SendReq = Class.forName("com.google.android.mms.pdu.SendReq")
        val EncodedStringValue = Class.forName("com.google.android.mms.pdu.EncodedStringValue")
        val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
        val PduComposer = Class.forName("com.google.android.mms.pdu.PduComposer")

        val sendReq = SendReq.newInstance()

        // set from
        val sim = getSimNumber(context)
        if (!TextUtils.isEmpty(sim)) {
            val encodedSim = EncodedStringValue.getConstructor(String::class.java).newInstance(sim)
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
        val size = addTextPart(pduBody, text, true)
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
            Log.e(TAG, "1", e)
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
        PduPart.getDeclaredMethod("setContentType", ByteArray::class.java).invoke(pduPart, "text/plain".toByteArray())

        // set content location
        PduPart.getDeclaredMethod("setContentLocation", ByteArray::class.java).invoke(pduPart, FILE_NAME.toByteArray())
        PduPart.getDeclaredMethod("setContentId", ByteArray::class.java).invoke(pduPart, "text_0".toByteArray())

        // set data
        PduPart.getDeclaredMethod("setData", ByteArray::class.java).invoke(pduPart, text.toByteArray())
        PduBody.getDeclaredMethod("addPart", pduPart::class.java).invoke(pduBody, pduPart)

        if (includeSmil) {
            val smil = String.format(sSmilText, FILE_NAME)
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

    fun getSimNumber(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.line1Number
    }


}