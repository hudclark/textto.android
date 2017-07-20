package com.octopusbeach.textto.service

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import com.octopusbeach.textto.BaseApplication

/**
 * Created by hudson on 9/5/16.
 */

class FirebaseIdService: FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        val refreshedToken = FirebaseInstanceId.getInstance().token
        if (refreshedToken != null)
            (applicationContext as BaseApplication).setFirebaseToken(refreshedToken)
    }
}
