package com.octopusbeach.textto.service

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.util.Log

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
        Log.e(TAG, "sms observer not configured")
    }
}