package com.moduloapps.textto.service

import com.google.firebase.iid.FirebaseInstanceIdService
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.jobs.UpdateFirebaseIdJob

/**
 * Created by hudson on 9/5/16.
 */

class FirebaseIdService: FirebaseInstanceIdService() {

    override fun onTokenRefresh() {
        (application as BaseApplication).addBackgroundJob(UpdateFirebaseIdJob())
    }
}
