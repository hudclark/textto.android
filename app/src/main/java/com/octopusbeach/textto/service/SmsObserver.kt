package com.octopusbeach.textto.service

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.util.Log
import com.octopusbeach.textto.api.ApiClient
import com.octopusbeach.textto.api.MessageEndpointInterface
import com.octopusbeach.textto.model.Message
import com.octopusbeach.textto.utils.MessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserver: ContentObserver {
    private val TAG = "Sms Observer"
    private var handler: Handler? = null
    private lateinit var context: Context

    constructor(handler: Handler, context: Context): super(handler) {
        this.handler = handler
        this.context = context
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        MessageUtils.updateMessages(context)
    }
}