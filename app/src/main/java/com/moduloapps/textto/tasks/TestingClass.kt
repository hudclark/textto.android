package com.moduloapps.textto.tasks

import android.content.Context
import android.util.Log
import com.moduloapps.textto.api.ApiService

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val apiService: ApiService) : Runnable {

    override fun run() {
        Log.e("TEST", com.moduloapps.textto.message.Thread.getAddresses(45, context).toString())
        Log.e("TEST", com.moduloapps.textto.message.Thread.getAddresses(3, context).toString())
        Log.e("TEST", com.moduloapps.textto.message.Thread.getAddresses(27, context).toString())
    }

}