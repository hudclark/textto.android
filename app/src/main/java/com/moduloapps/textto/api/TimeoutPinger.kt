package com.moduloapps.textto.api

import android.os.Handler
import android.util.Log

/**
 * Created by hudson on 10/24/17.
 */
class TimeoutPinger(private val listener: TimeoutPinger.OnFailedListener) {

    private val TAG = "TimeoutPinger"

    private val PING_INTERVAL: Long = 1000 * 60 * 1
    private val PING_TIMEOUT = 1000 * 60 * 1.5
    private val MAX_RETRIES = 1

    private var retries = 0
    private var isPinging = false

    private var lastReceivedPing: Long = 0

    private val handler = Handler()
    private val pingTask = Runnable {
        if (isPinging)  {
            val now = System.currentTimeMillis()
            if (now - lastReceivedPing > PING_TIMEOUT) {
                onPingTimeout()
            } else {
                schedulePing()
            }
        }
    }

    fun startPinging() {
        if (!isPinging) {
            retries = 0
            isPinging = true
            schedulePing()
        }
    }

    fun stopPinging() {
        if (isPinging) {
            isPinging = false
            handler.removeCallbacks(pingTask)
        }
    }

    fun onPingReceived(timestamp: Long) {
        Log.d(TAG, "Received ping with timestamp $timestamp")
        if (timestamp > lastReceivedPing) {
            lastReceivedPing = timestamp
        }
    }

    private fun schedulePing() {
        if (isPinging) handler.postDelayed(pingTask, PING_INTERVAL)
    }

    private fun onPingTimeout() {
        if (!isPinging) return
        Log.e(TAG, "Ping timed out")
        retries++
        if (retries > MAX_RETRIES) {
            stopPinging()
            listener.onPingFailed()
        } else {
            schedulePing()
        }
    }

    interface OnFailedListener {
        fun onPingFailed()
    }
}