package com.moduloapps.textto.message

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import com.moduloapps.textto.utils.ImageUtils
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Created by hudson on 8/6/17.
 */
object Mms {

    private val TAG = "Mms"
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
        val sender = getSender(id, context)
        val addresses = Thread.getAddresses(threadId, context)
        return Message(
                type = "mms",
                androidId = id,
                threadId = threadId,
                body = null,
                sender = sender,
                addresses = addresses,
                date = date * 1000)
    }

    fun postParts(parts: List<MmsPart>, apiService: ApiService, context: Context) {
        val postedParts = apiService.createMmsParts(parts).execute().body()["mmsParts"]
        postedParts?.forEach {
            it?.imageUrl?.let { imageUrl ->
                uploadFullImage(it.contentType, imageUrl, it.androidId, context, apiService)
            }
        }
    }

    fun getPartsForMms(mmsId: Int, context: Context): List<MmsPart> {
        val uri = Uri.parse("content://mms/part")
        val cur = context.contentResolver.query(uri, null, "mid=${mmsId}", null, null)
        val parts = ArrayList<MmsPart>()

        cur.tryForEach {
            val partId = cur.getInt(cur.getColumnIndex(Telephony.Mms.Part._ID))
            val contentType = cur.getString(cur.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE))
            val data = if (isTextPart(contentType)) getMmsText(cur, context) else ""
            val thumbnail = if (isImagePart(contentType)) getMmsImageThumbnail(partId, context) else null

            parts.add(MmsPart(
                    androidId = partId,
                    data = data,
                    contentType = contentType,
                    messageId = mmsId,
                    imageUrl = null,
                    thumbnail = thumbnail
            ))
        }

        cur.close()
        return parts
    }

    private fun getMmsText(cur: Cursor, context: Context): String {
        val data = cur.getString(cur.getColumnIndex(Telephony.Mms.Part._DATA))
        val id = cur.getInt(cur.getColumnIndex(Telephony.Mms.Part._ID))
        return  if (data != null) readTextData(id, context)
                else cur.getString(cur.getColumnIndex(Telephony.Mms.Part.TEXT))
    }

    private fun readTextData(id: Int, context: Context): String {
        val uri = Uri.parse("content://mms/part/" + id)
        val stream = context.contentResolver.openInputStream(uri)
        val builder = StringBuffer()
        stream?.let {
            val bufferedReader = BufferedReader(InputStreamReader(it, "UTF-8"))
            var line = bufferedReader.readLine()
            while (line != null) {
                builder.append(line)
                line = bufferedReader.readLine()
            }
            stream.close()
        }
        return builder.toString()
    }

    private fun getSender(id: Int, context: Context): String {
        val uri = Uri.parse("content://mms/$id/addr")
        val cur = context.contentResolver.query(uri, null, "msg_id=$id", null, null)

        val sender = cur.find {
            val address = cur.getString(cur.getColumnIndex("address"))
            if (!TextUtils.isEmpty(address)) {
                val isMe = MessageController.isMyAddress(address, context)
                return@find if (isMe) "me" else address
            }
            return@find null
        }

        cur.close()
        return sender ?: throw RuntimeException("Unable to find sender for mms " + id)
    }

    private fun uploadFullImage(contentType: String, imageUrl: String, partId: Int, context: Context, apiService: ApiService) {
        try {
            val uri = Uri.parse("content://mms/part/$partId")
            val stream: InputStream = context.contentResolver.openInputStream(uri)
            ImageUtils.uploadImage(stream, contentType, imageUrl, apiService)
        } catch (e: Exception) {
            Crashlytics.logException(e)
            Log.e(TAG, "Error uploading full size image", e)
        }
    }

    private fun getMmsImageThumbnail(partId: Int, context: Context): String? {
        val uri = Uri.parse("content://mms/part/$partId")
        return ImageUtils.createThumbnail(uri, context)
    }

    private fun isTextPart(type: String) = ("text/plain" == (type))

    private fun isImagePart(type: String): Boolean {
        return  type == "image/jpeg" ||
                type == "image/bmp"  ||
                type == "image/gif"  ||
                type == "image/jpg"  ||
                type == "image/png"
    }

    private fun <T> Cursor.find(fn: (Cursor) -> T?): T? {
        if (moveToFirst()) {
            do {
                val result = fn(this)
                if (result != null) return result
            } while (moveToNext())
        }
        return null
    }

    private fun Cursor.tryForEach(fn: (Cursor) -> Unit) {
        if (moveToFirst()) {
            do {
                try {
                    fn(this)
                } catch (e: Exception) {
                    Log.e(TAG, e.toString())
                    Crashlytics.logException(e)
                }
            } while (moveToNext())
        }
    }

}