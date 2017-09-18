package com.octopusbeach.textto.tasks

import android.content.Context
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.message.MessageController
import com.octopusbeach.textto.message.Mms

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val apiService: ApiService) : Runnable {

    override fun run() {
        MessageController.syncRecentThreads(context, apiService, 20)
    }


}