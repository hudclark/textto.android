package com.octopusbeach.textto.service

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopusbeach.textto.utils.MessageUtils

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        val data = msg.data
        val scheduledMessageId = data["messageId"]
        if (scheduledMessageId != null)
            MessageUtils.sendScheduledMessage(scheduledMessageId)
    }

}
