package com.moduloapps.textto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.TimeoutPinger
import com.moduloapps.textto.home.MainActivity
import com.moduloapps.textto.tasks.MessageSyncTask
import com.moduloapps.textto.utils.SYNC_CHANNEL_ID
import com.moduloapps.textto.utils.ThreadUtils

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserverService: Service(), TimeoutPinger.OnFailedListener {

    companion object {
        val FOREGROUND_EXTRA = "foreground"
        val START_FOREGROUND = "startForeground"
        val STOP_FOREGROUND = "stopForeground"
        val PING_FOREGROUND = "pingForeground"
        val PING_TIMESTAMP = "pingTimestamp"
    }

    private var observer: SmsObserver? = null
    private val TAG = "SmsObserverService"
    private var isForeground = false

    private val pinger = TimeoutPinger(this)

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
        pinger.stopPinging()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand")
        intent?.getStringExtra(FOREGROUND_EXTRA)?.let {
            when (it) {
                START_FOREGROUND -> ensureForegroundStarted()
                STOP_FOREGROUND -> ensureForegroundStopped()
                PING_FOREGROUND -> {
                    ensureForegroundStarted()
                    pinger.onPingReceived(intent.getLongExtra(PING_TIMESTAMP, 0))
                }
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

    private fun ensureForegroundStarted() {
        startForeground(1, createNotification()) // Fix for oreo. NEED to call if going to start in background
        if (!isForeground) {
            Log.d(TAG, "Starting foreground")
            isForeground = true
            pinger.startPinging()

            val app = applicationContext as BaseApplication

            ThreadUtils.runSingleThreadTask(MessageSyncTask(app.appComponent.getApiService(),
                    app, app.appComponent.getSharedPrefs()))
        }
    }

    private fun ensureForegroundStopped() {
        if (!NotificationListener.isRunning) {
            applicationContext.startService(Intent(applicationContext, NotificationListener::class.java))
        }
        if (isForeground) {
            Log.d(TAG, "Stopping foreground")
            isForeground = false
            stopForeground(true)
            pinger.stopPinging()
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, SYNC_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(this.getString(R.string.connected))
                //.setContentText(this.getString(R.string.dont_want_to_see))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(applicationContext, R.color.blue))
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setChannelId(SYNC_CHANNEL_ID)
                .setShowWhen(false)
                .build()
    }

    override fun onBind(intent: Intent?) = null

    override fun onPingFailed() {
        ensureForegroundStopped()
    }

}