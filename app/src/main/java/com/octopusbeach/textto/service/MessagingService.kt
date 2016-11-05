package com.octopusbeach.textto.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.octopusbeach.textto.utils.MessageUtils

/**
 * Created by hudson on 9/18/16.
 */

class MessagingService: FirebaseMessagingService() {

    override fun onMessageReceived(msg: RemoteMessage) {
        MessageUtils.updateMessages(applicationContext)
    }

}
