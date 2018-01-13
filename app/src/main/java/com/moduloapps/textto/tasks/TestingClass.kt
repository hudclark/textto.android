package com.moduloapps.textto.tasks

import android.content.Context
import android.util.Log
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.message.Mms

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val apiService: ApiService) : Runnable {

    override fun run() {

        val mms = Mms.getMmsForId(context, 735)

        Log.e("TEST", mms.toString())

        val parts = Mms.getPartsForMms(mms!!.androidId, context)
        Log.e("TEST", parts.size.toString())

        Log.e("TEST", parts[1].toString())

    }

}