package com.moduloapps.textto.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

/**
 * Created by hudson on 7/14/17.
 */
object ThreadUtils {

    private val singleThreadExecutor = Executors.newSingleThreadExecutor()
    private val mainThreadHandler = Handler(Looper.getMainLooper())

    fun runSingleThreadTask(runnable: Runnable) {
        singleThreadExecutor.execute(runnable)
    }

    fun runOnMainThread(runnable: Runnable) {
        mainThreadHandler.post(runnable)
    }

    fun runOnMainThread(runnable: Runnable, delay: Long) {
        mainThreadHandler.postDelayed(runnable, delay)
    }

    // TODO could use pooling here!
    fun runInBackground(runnable: Runnable) {
        Thread(runnable).start()
    }
}
