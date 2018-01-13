package com.moduloapps.textto.message

import android.content.Context
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.utils.ImageUtils
import java.io.InputStream

/**
 * Created by hudson on 12/16/17.
 */
class MmsPdu(private val to: Array<String>) {

    // Constants
    companion object {
        private val TAG = "MmsPdu"
        private val UTF8: Int = 106
        private val MAX_MMS_IMAGE_SIZE = 300 * 1024 // 500kb
        private val EXPIRY_TIME: Long = 7 * 24 * 60 * 60
        private val PRIORITY = 0x81
        private val VALUE_NO = 0x81

        val PDU_FROM by lazy {
            Class.forName("com.google.android.mms.pdu.PduHeaders").getField("FROM").get(null) as Int
        }
    }

    // reflected classes used
    private val SendReq = Class.forName("com.google.android.mms.pdu.SendReq")
    private val EncodedStringValue = Class.forName("com.google.android.mms.pdu.EncodedStringValue")
    private val PduBody = Class.forName("com.google.android.mms.pdu.PduBody")
    private val PduPart = Class.forName("com.google.android.mms.pdu.PduPart")
    private val PduComposer = Class.forName("com.google.android.mms.pdu.PduComposer")
    private val GenericPdu = Class.forName("com.google.android.mms.pdu.GenericPdu")

    // State
    private val pduBody = PduBody.newInstance()

    private var size: Long = 0L

    fun addText(text: String) {
        size += addTextPart(text)
    }

    fun addImage(image: MmsImage) {
        size += addImagePart(image)
    }

    fun build(context: Context): ByteArray {
        val sendReq = SendReq.newInstance()

        val from = MessageController.getSimNumber(context)
        if (!TextUtils.isEmpty(from)) {
            val encodedSim = EncodedStringValue.getConstructor(String::class.java).newInstance(from)
            SendReq.invoke(sendReq, "setFrom", encodedSim)
        }

        val encodedNumbers = EncodedStringValue.invoke(null, "encodeStrings", to)
        if (encodedNumbers != null) {
            SendReq.invoke(sendReq, "setTo", encodedNumbers)
        }

        SendReq.invoke(sendReq, "setDate", (System.currentTimeMillis() / 1000))
        SendReq.invoke(sendReq, "setBody", pduBody)
        SendReq.invoke(sendReq, "setMessageSize", size)
        SendReq.invoke(sendReq, "setMessageClass", "personal".toByteArray())
        SendReq.invoke(sendReq, "setExpiry", EXPIRY_TIME)

        try {
            // priority
            SendReq.invoke(sendReq, "setPriority", PRIORITY)
            SendReq.invoke(sendReq, "setDeliveryReport", VALUE_NO)
            SendReq.invoke(sendReq, "setReadReport", VALUE_NO)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error setting properties on sendReq", e)
        }

        val pduComposer = PduComposer.getConstructor(Context::class.java, GenericPdu).newInstance(context, sendReq)
        return PduComposer.invoke(pduComposer, "make") as ByteArray
    }

    private fun addImagePart(image: MmsImage): Int {
        val part = PduPart.newInstance()

        val filename = "image_${System.currentTimeMillis()}" // needs to be unique
        val filenameBytes = filename.toByteArray()
        val imageBytes =
                if (image.getContentType() === "image/gif")
                    image.getBytes()
                else
                    ImageUtils.compressImage(image.getByteStream(), MAX_MMS_IMAGE_SIZE)

        PduPart.invoke(part, "setContentType", image.getContentType().toByteArray())
        PduPart.invoke(part, "setContentLocation", filenameBytes)
        PduPart.invoke(part, "setContentId", filenameBytes)
        PduPart.invoke(part, "setData", imageBytes)

        // add part to body
        PduBody.invoke(pduBody, "addPart", part)

        // add smil
        addSmil(getSmilText(image.getContentType(), filename))

        val bytes = PduPart.invoke(part, "getData") as ByteArray
        return bytes.size + filenameBytes.size
    }

    private fun addTextPart(text: String): Int {
        val part = PduPart.newInstance()

        PduPart.invoke(part, "setCharset", UTF8)
        PduPart.invoke(part, "setContentType", "text/plain".toByteArray())
        // TODO Should this be changed?
        PduPart.invoke(part, "setContentLocation", "text_0.txt".toByteArray())
        PduPart.invoke(part, "setContentId", "text_0".toByteArray())
        PduPart.invoke(part, "setData", text.toByteArray())

        // add to pduBody
        PduBody.invoke(pduBody, "addPart", part)

        addSmil(getSmilText("text/plain", "text_0.txt"))

        val bytes = PduPart.invoke(part, "getData") as ByteArray
        return bytes.size
    }

    private fun addSmil(smil: String) {
        val part = PduPart.newInstance()
        PduPart.invoke(part, "setCharset", UTF8)
        PduPart.invoke(part, "setContentId", "smil".toByteArray())
        PduPart.invoke(part, "setContentLocation", "smil.xml".toByteArray())
        PduPart.invoke(part, "setContentType", "application/smil".toByteArray())
        PduPart.invoke(part, "setData", smil.toByteArray())

        // Add to body
        PduBody.invoke(pduBody, "addPart", part)
    }

    private fun getSmilText(type: String, src: String): String {
        var smil = SMIL_OPEN
        if (type.contains("image")) {
            smil += "<img src=\"$src\" region=\"Text\"/>"
        }
        smil += "</par></body></smil>"
        return smil
    }

    private fun Class<*>.invoke(obj: Any?, methodName: String, vararg args: Any): Any? {
        val classes = args.map({
            return@map if (it.isPrimitive()) {
                it.javaClass.kotlin.javaPrimitiveType
            } else {
                it::class.java
            }
        }).toTypedArray()

        val method =
                try {
                    getDeclaredMethod(methodName, *classes)
                } catch (e: NoSuchMethodException) {
                    getMethod(methodName, *classes)
                }

        return method.invoke(obj, *args)
    }

    private fun Any.isPrimitive(): Boolean {
        return (this is Int ||
                this is Boolean ||
                this is Long ||
                this is Byte ||
                this is Char)
    }

    private val SMIL_OPEN =
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

    interface MmsImage {
        fun getContentType(): String
        fun getByteStream(): InputStream
        fun getBytes(): ByteArray
    }


}