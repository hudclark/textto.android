package com.moduloapps.textto.api

import android.util.Log
import com.crashlytics.android.Crashlytics
import retrofit2.Call
import retrofit2.Callback

/**
 * Created by hudson on 10/23/17.
 */
abstract class RetryCallback<T>(private val maxRetries: Int,
                                private val waitTime: Int) : Callback<T> {

    private val TAG = "RetryCallback"

    private var retries = 0

    final override fun onFailure(call: Call<T>, t: Throwable) {
        if (retries < maxRetries) {
            Log.e(TAG, "Request failed, retrying...")
            retry(call)
        } else {
            Log.e(TAG, "Request failed, aborting...")
            Crashlytics.logException(t)
            onFailed(t)
        }
    }

    abstract fun onFailed(t: Throwable)

    private fun retry(call: Call<T>) {
        retries++
        val wait = (waitTime * (retries * retries)).toLong()
        Thread(Runnable {
            try {
                Thread.sleep(wait)
            } catch (e: Exception) {
                Log.e(TAG, "Error retrying request")
                Crashlytics.logException(e)
            }
            call.clone().enqueue(this)
        }).run()
    }

}