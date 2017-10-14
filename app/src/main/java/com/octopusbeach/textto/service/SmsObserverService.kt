package com.octopusbeach.textto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.support.v7.app.NotificationCompat
import android.text.TextUtils
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.R
import com.octopusbeach.textto.home.MainActivity
import retrofit2.Call
import retrofit2.Response

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserverService: Service() {

    companion object {
        val FOREGROUND_EXTRA = "foreground"
        val START_FOREGROUND = "startForeground"
        val STOP_FOREGROUND = "stopForeground"
    }

    private val PING_INTERVAL = 60L * 1000L

    private var observer: SmsObserver? = null
    private val TAG = "SmsObserverService"
    private var isForeground = false

    private var isPinging = false
    private var retries = 0
    private val handler = Handler()
    private val pingTask = Runnable {
        (applicationContext as BaseApplication).appComponent.getApiService().ping().enqueue(object : retrofit2.Callback<String> {
            override fun onResponse(call: Call<String>?, response: Response<String>) {
                if (!TextUtils.isEmpty(response.body()))
                    schedulePing()
                else
                    onPingFailure()
            }

            override fun onFailure(call: Call<String>?, t: Throwable?) {
                onPingFailure()
            }
        })
    }

    override fun onCreate() {
        super.onCreate()
        observeSms()
        isPinging = false
    }

    override fun onDestroy() {
        Log.d(TAG, "Stopping")
        if (observer != null) {
            contentResolver.unregisterContentObserver(observer)
            observer = null
        }
        if (isPinging) stopPinging()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand")
        intent?.getStringExtra(FOREGROUND_EXTRA)?.let {
            if (it == START_FOREGROUND && !isForeground) {
                startForeground()
            } else if (it == STOP_FOREGROUND && isForeground) {
                stopForeground()
            }
        }
        return START_STICKY
    }

    private fun observeSms() {
        Log.d(TAG, "Starting")
        if (observer == null) {
            val baseApp = applicationContext as BaseApplication
            observer = SmsObserver(baseApp, baseApp.appComponent.getApiService(), baseApp.appComponent.getSharedPrefs())
            contentResolver.registerContentObserver(Uri.parse("content://mms-sms"), true, observer)
        }
    }

    private fun startForeground() {
        if (!isForeground) {
            Log.d(TAG, "Starting foreground")
            isForeground = true
            startForeground(1, createNotification())
            if (!isPinging) startPinging()
        }
    }

    private fun stopForeground() {
        if (isForeground) {
            Log.d(TAG, "Stopping foreground")
            isForeground = false
            stopForeground(true)
            if (isPinging) stopPinging()
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.getString(R.string.connected))
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setShowWhen(false)
                .build()
    }

    private fun startPinging() {
        retries = 0
        if (!isPinging) {
            isPinging = true
            handler.post(pingTask)
        }
    }

    private fun stopPinging() {
        isPinging = false
        handler.removeCallbacks(pingTask)
    }

    private fun schedulePing() {
        if (!isPinging) return
        handler.postDelayed(pingTask, PING_INTERVAL)
    }

    private fun onPingFailure() {
        if (!isPinging) return
        if (retries < 3) {
            retries++
            schedulePing()
        } else {
            stopPinging()
            stopForeground()
        }
    }

    override fun onBind(intent: Intent?) = null

}