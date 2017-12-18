package com.moduloapps.textto.message

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.text.TextUtils

/**
 * Created by hudson on 12/17/17.
 */
object Thread {

    fun getAddresses(threadId: Int, context: Context): List<String> {
        return getRecipientIds(threadId, context)
                .map { getRecipientAddress(it, context) }
                .filter { (!TextUtils.isEmpty(it) && !MessageController.isMyAddress(it, context)) }
    }

    private fun getRecipientAddress(recipientId: Int, context: Context): String {
        val uri = Uri.parse("content://mms-sms/canonical-address/$recipientId")
        val cur = context.contentResolver.query(uri, arrayOf(Telephony.TextBasedSmsColumns.ADDRESS), "${Telephony.BaseMmsColumns._ID}=$recipientId", null, null)

        val address: String
        if (cur != null && cur.moveToFirst()) {
            address = cur.getString(cur.getColumnIndex("address"))
            cur.close()
        } else {
            address = ""
        }
        return address
    }

    private fun getRecipientIds(threadId: Int, context: Context): List<Int> {
        val uri = Uri.parse("content://mms-sms/conversations/$threadId/recipients")
        val cur = context.contentResolver.query(uri, arrayOf(Telephony.Threads.RECIPIENT_IDS), "${Telephony.Threads._ID}=$threadId", null, null)

        val ids: List<Int>
        if (cur != null && cur.moveToFirst()) {
            ids = cur.getString(cur.getColumnIndex(Telephony.Threads.RECIPIENT_IDS)).split(" ").map { it.toInt()}
            cur.close()
        } else {
            ids = ArrayList()
        }
        return ids
    }

}