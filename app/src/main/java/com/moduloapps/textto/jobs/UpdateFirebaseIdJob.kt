/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.jobs

import android.util.Log
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonObject
import com.moduloapps.textto.BaseApplication

/**
 * Created by hudson on 2/18/18.
 */
class UpdateFirebaseIdJob: Job(Params(PRIORITY).requireNetwork().persist()) {

    companion object {
        const val PRIORITY = 100
        const val FIREBASE_ID_KEY = "firebaseId"
        const val TAG = "UpdateFireBaseIdJob"
    }

    override fun onAdded() {
        Log.d(TAG, "Added job")
    }

    override fun onRun() {
        val component = (applicationContext as BaseApplication).appComponent
        if (!component.getSessionController().isLoggedIn()) return

        Log.d(TAG, "Updating firebase id...")

        val data = JsonObject()
        val token = FirebaseInstanceId.getInstance().token ?: return
        data.addProperty(FIREBASE_ID_KEY, token)

        val response = component.getApiService().updateFirebaseId(data).execute()
        if (response.code() != 200) {
            Log.d(TAG, "Received ${response.code()} from api. Aborting...")
            throw Error("Invalid response received")
        }

        Log.d(TAG, "Updated firebase id")
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        return RetryConstraint.createExponentialBackoff(runCount, 10)
    }

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        Log.d(TAG, "Cancelled.")
    }

    override fun getRetryLimit() = 25

}