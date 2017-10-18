package com.moduloapps.textto.message

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Telephony
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import com.moduloapps.textto.utils.ImageUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by hudson on 8/6/17.
 */
object Mms {

    private val TAG = "Mms"

    private val ME = "insert-address-token"
    private val MAX_MESSAGES = 400

    fun syncMms(date: Long, id: Int, context: Context, apiService: ApiService) {
        // mms date bug
        val mmsDate = date / 1000
        val selection = "date > $mmsDate AND _id > $id"
        val uri = Uri.parse("content://mms")
        val cur = context.contentResolver.query(uri, null, selection, null, null)
        var counter = 0
        val messageCount = Math.min(cur.count, 1000)
        val messages = ArrayList<Message>()
        if (cur.moveToFirst()) {
            while (counter < messageCount) {
                val message = getMmsForCursor(cur, context)
                if (messages.size > MAX_MESSAGES) {
                    MessageController.postMessages(messages, context, apiService)
                    messages.clear()
                }
                messages.add(message)

                counter++
                cur.moveToNext()
            }
        }
        cur.close()

        if (messages.isNotEmpty()) {
            MessageController.postMessages(messages, context, apiService)
        }
    }

    fun getMmsForId(context: Context, id: Int): Message? {
        var message: Message? = null
        val cur = context.contentResolver.query(Uri.parse("content://mms"), null, "_id=$id", null, null)
        if (cur.moveToFirst()) {
            message = getMmsForCursor(cur, context)
        }
        cur.close()
        return message
    }

    private fun getMmsForCursor(cur: Cursor, context: Context): Message {
        val id = cur.getInt(cur.getColumnIndex("_id"))
        val threadId = cur.getInt(cur.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID))
        val date = cur.getLong(cur.getColumnIndex(Telephony.BaseMmsColumns.DATE))
        val recipients = getSenderAndRecipients(id, context)
        return Message(
                type = "mms",
                androidId = id,
                threadId = threadId,
                body = null,
                sender = recipients.first,
                addresses = recipients.second,
                date = date * 1000)
    }

    private fun getSenderAndRecipients(id: Int, context: Context): Pair<String, List<String>> {
        val uri = Uri.parse("content://mms/$id/addr")
        val cur = context.contentResolver.query(uri, null, "msg_id=$id", null, null)
        val addresses = ArrayList<String>()
        var sender: String? = null
        if (cur.moveToFirst()) {
            do {
                val address = cur.getString(cur.getColumnIndex("address"))
                if (!TextUtils.isEmpty(address)) {
                    if (sender == null) {
                        sender = if (isAddressMe(address, context)) "me" else address
                    }
                    if (!isAddressMe(address, context)) addresses.add(address)
                }
            } while (cur.moveToNext())
        }
        cur.close()
        return Pair(sender ?: "", addresses)
    }

    fun postParts(parts: List<MmsPart>, apiService: ApiService, context: Context) {
        val postedParts = apiService.createMmsParts(parts).execute().body()["mmsParts"]
        postedParts?.forEach {
            it?.imageUrl?.let { imageUrl ->
                uploadFullImage(it.contentType, imageUrl, it.androidId, context, apiService)
            }
        }
    }

    fun getPartsForMessage(mms: Message, context: Context): List<MmsPart> {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid=${mms.androidId}"
        val cur = context.contentResolver.query(uri, null, selection, null, null)
        val parts = ArrayList<MmsPart>()
        if (cur.moveToFirst()) {
            do {
                try {
                    val partId = cur.getInt(cur.getColumnIndex("_id"))
                    val type = cur.getString(cur.getColumnIndex("ct"))
                    Log.d(TAG, "Part from " + mms.sender + ": " + type)
                    if (isTextPart(type)) {
                        val data =  cur.getString(cur.getColumnIndex("_data"))
                        val textPart =
                            if (data != null) {
                                readTextData(partId, context)
                            } else {
                                cur.getString(cur.getColumnIndex("text"))
                            }
                        val part = MmsPart(
                                androidId = partId,
                                data = textPart,
                                contentType = "text/plain",
                                messageId = mms.androidId,
                                imageUrl = null,
                                thumbnail = null)
                        parts.add(part)
                    } else if (isImagePart(type)) {
                        parts.add(createImagePart(mms, partId, type, context))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating mms part", e)
                    Crashlytics.logException(e)
                }

            } while (cur.moveToNext())
        }
        cur.close()
        return parts
    }

    private fun readTextData(id: Int, context: Context): String {
        val uri = Uri.parse("content://mms/part/" + id)
        val builder = StringBuffer()
        var stream: InputStream? = null
        try {
            stream = context.contentResolver.openInputStream(uri)
            stream?.let {
                val streadReader = InputStreamReader(it, "UTF-8")
                val bufferedReader = BufferedReader(streadReader)
                var line = bufferedReader.readLine()
                while (line != null) {
                    builder.append(line)
                    line = bufferedReader.readLine()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading mms text", e)
            Crashlytics.logException(e)
        } finally {
            stream?.close()
        }
        return builder.toString()
    }

    private fun createImagePart(mms: Message, partId: Int, contentType: String, context: Context): MmsPart {
        // create the thumbnail for the image
        val thumbnail = getThumbnail(partId, context) ?: ""
        return MmsPart(
                androidId = partId,
                data = "",
                thumbnail = thumbnail,
                contentType = contentType,
                messageId = mms.androidId,
                imageUrl = null)
    }

    private fun getThumbnail(partId: Int, context: Context): String? {
        val uri = Uri.parse("content://mms/part/$partId")
        var inputStream = context.contentResolver.openInputStream(uri)

        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, options)
        inputStream.close()

        if ((options.outWidth == -1) || (options.outHeight == -1)) return null

        val originalSize = if (options.outHeight > options.outWidth) options.outHeight else options.outWidth
        val ratio = (originalSize / 25).toDouble()

        val outOptions = BitmapFactory.Options()
        outOptions.inSampleSize = getSampleRatio(ratio)
        inputStream = context.contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream, null, outOptions)
        inputStream.close()
        val imageString = ImageUtils.imageToBase64(bitmap)
        bitmap.recycle()
        return imageString
    }

    private fun getSampleRatio(ratio: Double): Int {
        val i = Integer.highestOneBit(Math.floor(ratio).toInt())
        return if (i == 0) 1 else i
    }

    private fun uploadFullImage(contentType: String, imageUrl: String, partId: Int, context: Context, apiService: ApiService) {
        try {
            val uri = Uri.parse("content://mms/part/$partId")

            val stream: InputStream = context.contentResolver.openInputStream(uri)
            val bytes: ByteArray

            when (contentType) {
                "image/gif" -> {
                    bytes = ByteArray(stream.available())
                    while (stream.read(bytes) != -1);
                    stream.close()
                }
                "image/png" -> bytes = ImageUtils.compressImage(stream, Bitmap.CompressFormat.PNG)
                else -> bytes = ImageUtils.compressImage(stream, Bitmap.CompressFormat.JPEG)
            }

            // upload image to aws.
            val body = RequestBody.create(MediaType.parse(contentType), bytes)
            apiService.putMmsImage(imageUrl, body).enqueue(object: retrofit2.Callback<Int> {
                override fun onFailure(call: Call<Int>?, t: Throwable?) {
                    Log.e(TAG, "failed to upload mms image")
                }

                override fun onResponse(call: Call<Int>?, response: Response<Int>?) {
                    Log.d(TAG, "Uploaded mms image")
                }
            })

        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error uploading full size image", e)
        }
    }

    private fun isTextPart(type: String) = ("text/plain" == (type))

    private fun isImagePart(type: String): Boolean {
        return  type == "image/jpeg" ||
                type == "image/bmp"  ||
                type == "image/gif"  ||
                type == "image/jpg"  ||
                type == "image/png"
    }

    private fun isAddressMe (address: String, context: Context): Boolean {
        val myNumber = (context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).line1Number
        return when (address) {
            ME -> true
            myNumber -> true
            else -> false
        }
    }

}