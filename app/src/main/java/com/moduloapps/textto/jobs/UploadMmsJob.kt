/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.jobs

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.Params
import com.birbit.android.jobqueue.RetryConstraint
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.utils.ImageUtils
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.InputStream
import java.nio.charset.Charset

/**
 * Created by hudson on 2/18/18.
 */
class UploadMmsJob(private val contentType: String, private val imageUrl: String, private val partId: Int)
                   : Job(Params(PRIORITY).requireNetwork().persist()) {

    companion object {
        const val PRIORITY = 50 // firebaseid is 100
        const val TAG = "UploadMmsJob"
    }

    override fun onAdded() {
        Log.d(TAG, "Added job for part $partId")
    }

    override fun onRun() {
        val component = (applicationContext as BaseApplication).appComponent

        if (!component.getSessionController().isLoggedIn()) return

        Log.d(TAG, "Uploading image for part $partId")

        val uri = Uri.parse("content://mms/part/$partId")
        val stream: InputStream = applicationContext.contentResolver.openInputStream(uri)
        val bytes = ImageUtils.compressImage(stream, contentType)

        val mediaType: MediaType

        var body: RequestBody? = null

        /*
        If encryption is enabled, encrypt the image and send it over as base64.
        Note that the front-end can still get the contentType as it's set on the
        mms part that this image belongs to.

        Note that the image is base64 encoded before it is encrypted. This makes it
        a lot easier to display on the front-end.
         */
        val encryptionHelper = component.getEncryptionHelper()
        if (encryptionHelper.enabled()) {
            try {
                mediaType = MediaType.parse("text/plain; charset=utf-8")
                val data = encryptionHelper.encrypt(bytes.toBase64())
                body = RequestBody.create(mediaType, data)
            } catch (e: Exception) {
                Log.e(TAG, "Error encrypting image")
                return
            }
        }

        /*
        If encryption is not enabled, send the image as normal, setting the contentType
        and posting the binary data.
         */
        else {
            mediaType = MediaType.parse(contentType)
            body = RequestBody.create(mediaType, bytes)
        }

        val response = component.getApiService().uploadImage(imageUrl, body).execute()

        if (response.code() < 200 || response.code() > 400) {
            Log.d(TAG, "Invalid response")
            throw Error("Invalid api response.")
        }

        Log.d(TAG, "Finished uploading part $partId")

    }

    fun ByteArray.toBase64() = String(Base64.encode(this, Base64.NO_WRAP), Charset.forName("UTF-8"))

    override fun onCancel(cancelReason: Int, throwable: Throwable?) {
        Log.d(TAG, "Cancelled")
    }

    override fun shouldReRunOnThrowable(throwable: Throwable, runCount: Int, maxRunCount: Int): RetryConstraint {
        return RetryConstraint.createExponentialBackoff(runCount, 1000)
    }

    override fun getRetryLimit() = 10

}