package com.moduloapps.textto.message

import android.app.Application
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.text.TextUtils
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.MAX_MESSAGES_PER_REQUEST
import com.moduloapps.textto.jobs.UploadMmsJob
import com.moduloapps.textto.model.Message
import com.moduloapps.textto.model.MmsPart
import com.moduloapps.textto.utils.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Created by hudson on 8/6/17.
 */
object Mms {

    private val TAG = "Mms"

    fun syncMms(date: Long, id: Int, context: Application, apiService: ApiService) {
        // mms date bug
        val mmsDate = date / 1000
        val selection = "date > $mmsDate AND _id > $id"
        val uri = Uri.parse("content://mms")
        val ordering = "${Telephony.Mms._ID} desc"
        val cur = context.contentResolver.query(uri, null, selection, null, ordering)
        val messageCount = Math.min(cur.count, 1000)
        val messages = ArrayList<Message>()

        cur.whileUnder(messageCount, {
            val message = getMmsForCursor(it, context)
            messages.add(message)
            if (messages.size > MAX_MESSAGES_PER_REQUEST) {
                MessageController.postMessages(messages, context, apiService)
                messages.clear()
            }
        })

        cur.close()

        if (messages.isNotEmpty()) {
            MessageController.postMessages(messages, context, apiService)
        }
    }

    fun getMmsForId(context: Context, id: Int): Message? {
        var message: Message? = null
        val cur = context.contentResolver.query(Uri.parse("content://mms"), null, "_id=$id", null, null)
        cur.withFirst {
            message = getMmsForCursor(it, context)
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

    fun postParts(parts: List<MmsPart>, apiService: ApiService, context: Application) {
        val postedParts = apiService.createMmsParts(parts).execute().body()["mmsParts"]
        postedParts?.forEach {
            it?.imageUrl?.let { imageUrl ->
                (context as BaseApplication).addBackgroundJob(UploadMmsJob(it.contentType, imageUrl, it.androidId))
            }
        }
    }

    fun getPartsForMms(mmsId: Int, context: Context): List<MmsPart> {
        val uri = Uri.parse("content://mms/part")
        val cur = context.contentResolver.query(uri, null, "mid=$mmsId", null, null)
        val parts = ArrayList<MmsPart>()

        cur.tryForEach {
            val partId = cur.getInt(cur.getColumnIndex(Telephony.Mms.Part._ID))
            val contentType = cur.getString(cur.getColumnIndex(Telephony.Mms.Part.CONTENT_TYPE))
            if (contentType != "application/smil") {
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
        }
        cur.close()
        return parts
    }

    private fun getMmsText(cur: Cursor, context: Context): String {
        val data = cur.getString(cur.getColumnIndex(Telephony.Mms.Part._DATA))
        val id = cur.getInt(cur.getColumnIndex(Telephony.Mms.Part._ID))
        return if (data != null) readTextData(id, context)
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


    //https://github.com/aosp-mirror/platform_packages_apps_mms/blob/master/src/com/android/mms/util/AddressUtils.java
    private fun getSender(id: Int, context: Context): String {
        val uri = Uri.parse("content://mms/$id/addr")
        val selection = "${Telephony.Mms.Addr.TYPE}=${MmsPdu.PDU_FROM}"
        val cur = context.contentResolver.query(uri, arrayOf(Telephony.Mms.Addr.ADDRESS), selection, null, null)
        var sender: String? = null
        cur?.withFirst {
            val address = cur.getString(0)
            if (!TextUtils.isEmpty(address)) {
                val isMe = MessageController.isMyAddress(address, context)
                sender = if (isMe) "me" else address
            }
        }

        cur?.close()

        return sender ?: "unknown"
    }

    private fun getMmsImageThumbnail(partId: Int, context: Context): String? {
        val uri = Uri.parse("content://mms/part/$partId")
        return ImageUtils.createThumbnail(uri, context)
    }

    private fun isTextPart(type: String) = "text/plain" == (type)
    private fun isImagePart(type: String): Boolean {
        return  type == "image/jpeg" ||
                type == "image/bmp"  ||
                type == "image/gif"  ||
                type == "image/jpg"  ||
                type == "image/png"
    }

}