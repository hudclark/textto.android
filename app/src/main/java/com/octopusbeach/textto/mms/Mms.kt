package com.octopusbeach.textto.mms

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.Telephony
import android.text.TextUtils
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.model.Message
import com.octopusbeach.textto.model.MmsPart
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
    private val MAX_MESSAGES = 75

    fun syncMmsToDate(date: Long, context: Context, apiService: ApiService) {
        // mms date bug
        val mmsDate = date / 1000
        val selection = "date > $mmsDate"
        val uri = Uri.parse("content://mms")
        val cur = context.contentResolver.query(uri, null, selection, null, null)
        var counter = 0
        val messageCount = Math.min(cur.count, 1000)
        val messages = ArrayList<Message>()
        if (cur.moveToFirst()) {
            while (counter < messageCount) {
                val message = getMmsForCursor(cur, context)
                if (messages.size > MAX_MESSAGES) {
                    postMessages(messages, context, apiService)
                    messages.clear()
                }
                messages.add(message)

                counter++
                cur.moveToNext()
            }
        }
        cur.close()

        if (messages.isNotEmpty()) {
            postMessages(messages, context, apiService)
        }
    }

    private fun getMmsForCursor(cur: Cursor, context: Context): Message {
        val id = cur.getInt(cur.getColumnIndex("_id"))
        val threadId = cur.getInt(cur.getColumnIndex(Telephony.BaseMmsColumns.THREAD_ID))
        val date = cur.getLong(cur.getColumnIndex(Telephony.BaseMmsColumns.DATE))
        val recipients = getSenderAndRecepients(id, context)
        val mms = Message(
                type = "mms",
                androidId = id,
                threadId = threadId,
                body = null,
                sender = recipients.first,
                addresses = recipients.second,
                date = date * 1000)
        return mms
    }

    private fun getSenderAndRecepients(id: Int, context: Context): Pair<String, List<String>> {
        val uri = Uri.parse("content://mms/$id/addr");
        val cur = context.contentResolver.query(uri, null, "msg_id=$id", null, null);
        val addresses = ArrayList<String>();
        var sender: String? = null
        if (cur.moveToFirst()) {
            do {
                val address = cur.getString(cur.getColumnIndex("address"))
                if (!TextUtils.isEmpty(address)) {
                    if (sender == null) {
                        if (address == ME) sender = "me"
                        else sender = address
                    }
                    if (address != ME) addresses.add(address)
                }
            } while (cur.moveToNext())
        }
        cur.close()
        return Pair(sender ?: "", addresses)

    }

    private fun isTextPart(type: String) = ("text/plain" == (type))
    private fun isImagePart(type: String): Boolean {
        return  type == "image/jpeg" ||
                type == "image/bmp"  ||
                type == "image/gif"  ||
                type == "image/jpg"  ||
                type == "image/png"
    }
    private fun postMessages(messages: List<Message>, context: Context, apiService: ApiService) {
        apiService.createMessages(messages).execute()
        messages.forEach {
            uploadParts(it, context, apiService)
        }
    }

    private fun uploadParts(mms: Message, context: Context, apiService: ApiService) {
        val uri = Uri.parse("content://mms/part")
        val selection = "mid=${mms.androidId}"
        val cur = context.contentResolver.query(uri, null, selection, null, null)

        if (cur.moveToFirst()) {
            do {
                try {
                    val partId = cur.getInt(cur.getColumnIndex("_id"))
                    val type = cur.getString(cur.getColumnIndex("ct"))
                    if (isTextPart(type)) {
                        val data =  cur.getString(cur.getColumnIndex("_data"))
                        val textPart: String
                        if (data != null) {
                            textPart = readTextData(partId, context)
                        } else {
                            textPart = cur.getString(cur.getColumnIndex("text"))
                        }
                        uploadTextPart(mms, partId, textPart, apiService)
                    } else if (isImagePart(type)) {
                        uploadImagePart(mms, partId, type, context, apiService)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating mms part", e);
                }

            } while (cur.moveToNext())
        }

        cur.close()
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
        } finally {
            stream?.close()
        }
        return builder.toString()
    }

    private fun uploadImagePart(mms: Message, partId: Int, contentType: String, context: Context, apiService: ApiService) {
        // get signed url
        val part = MmsPart(
                androidId = partId,
                data = "",
                contentType = contentType,
                messageId = mms.androidId )
        val url = apiService.createMmsPart(part).execute().body()["mmsPart"]?.data
        url?.let {
            val uri = Uri.parse("content://mms/part/$partId")
            val stream: InputStream = context.contentResolver.openInputStream(uri)
            val buffer = ByteArray(stream.available())
            while (stream.read(buffer) != -1);
            stream.close()
            // upload image to aws.
            val body = RequestBody.create(MediaType.parse(contentType), buffer)
            apiService.putMmsImage(url, body).enqueue(object: retrofit2.Callback<Int> {
                override fun onFailure(call: Call<Int>?, t: Throwable?) {
                    Log.e(TAG, "failed to upload mms image")
                }

                override fun onResponse(call: Call<Int>?, response: Response<Int>?) {
                    Log.d(TAG, "Uploaded mms image")
                }
            })
        }
    }

    private fun uploadTextPart(mms: Message, partId: Int, content: String, apiService: ApiService) {
        val part = MmsPart(
                androidId = partId,
                data = content,
                contentType = "text/plain",
                messageId = mms.androidId )
        apiService.createMmsPart(part).execute()
    }

}