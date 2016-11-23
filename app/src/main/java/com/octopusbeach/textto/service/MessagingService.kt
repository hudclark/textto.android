package com.octopusbeach.textto.service

import android.content.Intent
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopusbeach.textto.utils.MessageUtils

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        // make sure we are listening to messages
        if (!SmsListenerService.running)
            applicationContext.startService(Intent(applicationContext, SmsListenerService::class.java))
        MessageUtils.updateMessages(applicationContext)
    }

}
