package com.moduloapps.textto.tasks

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.util.Log
import com.moduloapps.textto.api.ApiService

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val apiService: ApiService) : Runnable {

    override fun run() {
        val uri = Uri.parse("content://mms-sms/conversations")
        val cur = context.contentResolver.query(uri, null, null, null, null, null)
        if (cur.moveToFirst()) {
            do {
                val read = cur.getInt(cur.getColumnIndex("read"))
                val address = cur.getString(cur.getColumnIndex("address"))
                if (read == 0) {
                    val contentValues = ContentValues()
                    contentValues.put("seen", true)
                    val id = cur.getString(cur.getColumnIndex("_id"))
                    Log.e("TEST", "Attempting to mark message $id as read")
                    val result = context.contentResolver.update(Uri.parse("content://mms-sms/conversations"), contentValues, "_id=$id", null)
                    Log.e("TEST", "result: $result")
                }
            } while (cur.moveToNext())
        }
        cur.close()
    }

}