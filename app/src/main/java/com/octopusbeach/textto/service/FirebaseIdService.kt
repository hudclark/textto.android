package com.octopusbeach.textto.service

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.octopusbeach.textto.api.SessionManager

/**
 * Created by hudson on 9/5/16.
 */

class FirebaseIdService: FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        SessionManager.setFirebaseToken(refreshedToken)
    }
}
