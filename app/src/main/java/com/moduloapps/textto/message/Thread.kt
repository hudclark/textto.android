package com.moduloapps.textto.message

import android.content.Context
import android.net.Uri
import android.text.TextUtils

/**
 * Created by hudson on 12/17/17.
 */
object Thread {

    fun getAddresses(threadId: Int, context: Context): List<String> {
        val uri = Uri.parse("content://mms-sms/conversations/$threadId/recipients")
        val cur = context.contentResolver.query(uri, arrayOf("recipient_ids"), "_id=$threadId", null, null)

        val addresses = ArrayList<String>()

        if (cur != null && cur.moveToFirst()) {
            val recipientIds = cur.getString(cur.getColumnIndex("recipient_ids")).split(" ")
            recipientIds.forEach {
                addresses.add(getRecipientAddress(it.toInt(), context))
            }
        }

        addresses.filter {
            (!TextUtils.isEmpty(it) && !MessageController.isMyAddress(it, context))
        }

        cur?.close()
        return addresses
    }

    // TODO could do a _id IN (ids..) statement instead of this.
    private fun getRecipientAddress(recipientId: Int, context: Context): String {
        val uri = Uri.parse("content://mms-sms/canonical-address/$recipientId")
        val cur = context.contentResolver.query(uri, arrayOf("address"), "_id=$recipientId", null, null)

        if (cur != null && cur.moveToFirst()) {
            return cur.getString(cur.getColumnIndex("address"))
        }
        cur?.close()
        return ""
    }

}