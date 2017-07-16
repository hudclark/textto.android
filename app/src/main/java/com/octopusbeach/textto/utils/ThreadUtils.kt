package com.octopusbeach.textto.utils

import java.util.concurrent.Executors

/**
 * Created by hudson on 7/14/17.
 */
object ThreadUtils {

    private val singleThreadExecut0r = Executors.newSingleThreadExecutor()

    fun runSingleThreadTask(runnable: Runnable) {
        singleThreadExecut0r.execute(runnable)
    }

}
