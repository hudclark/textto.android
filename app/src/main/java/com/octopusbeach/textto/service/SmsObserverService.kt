package com.octopusbeach.textto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.support.v7.app.NotificationCompat
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.R
import com.octopusbeach.textto.home.MainActivity

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserverService: Service() {

    private var observer: SmsObserver? = null
    private val TAG = "SmsObserverService"
    private var isForeground = false

    companion object {
        val FOREGROUND_EXTRA = "foreground"
        val START_FOREGROUND = "startForeground"
        val STOP_FOREGROUND = "stopForeground"
    }

    override fun onCreate() {
        super.onCreate()
        observeSms()
    }

    override fun onDestroy() {
        Log.d(TAG, "Stopping")
        if (observer != null) {
            contentResolver.unregisterContentObserver(observer)
            observer = null
        }
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
            observer = SmsObserver(applicationContext, baseApp.appComponent.getApiService(), baseApp.appComponent.getSharedPrefs())
            contentResolver.registerContentObserver(Uri.parse("content://mms-sms"), true, observer)
        }
    }

    private fun startForeground() {
        if (!isForeground) {
            Log.d(TAG, "Starting foreground")
            isForeground = true
            startForeground(1, createNotification())
        }
    }

    private fun stopForeground() {
        if (isForeground) {
            Log.d(TAG, "Stopping foreground")
            isForeground = false
            stopForeground(true)
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


    override fun onBind(intent: Intent?) = null

}