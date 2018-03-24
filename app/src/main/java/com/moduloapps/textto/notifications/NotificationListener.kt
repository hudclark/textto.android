/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.notifications

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.SimpleCache
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.encryption.EncryptionHelper
import com.moduloapps.textto.service.SmsObserverService
import com.moduloapps.textto.utils.ImageUtils
import com.moduloapps.textto.utils.ThreadUtils
import com.moduloapps.textto.utils.toBase64
import javax.inject.Inject

/**
 * Created by hudson on 10/2/17.
 */
class NotificationListener : NotificationListenerService() {

    companion object {
        private val TAG = "NotificationListener"

        val CLEAR_TEXT_NOTIFICATIONS = "clear_text"

        private val NOTIFICATION_ICON_SIZE = 2500

        val EMPTY_VIBRATE = longArrayOf(0)

        var isRunning = false

        fun isEnabled (context: Context): Boolean {
            val listeners = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return (listeners != null && listeners.contains(context.packageName))
        }

    }

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sessionController: SessionController
    @Inject lateinit var encryptionHelper: EncryptionHelper

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
    }

    private val iconCache = SimpleCache<String, String>(30)

    override fun onBind(intent: Intent): IBinder {
        val action = intent.action
        if (action == SERVICE_INTERFACE) {
            Log.d(TAG, "Bound by system")
        }
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (eligibleToPostNotification() && shouldPostNotification(sbn)) {
            postNotification(sbn)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isRunning = true
        if (intent != null && intent.getBooleanExtra(CLEAR_TEXT_NOTIFICATIONS, false) && isEnabled(applicationContext)) {
            try {
                clearTextNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing notifications", e)
                Crashlytics.logException(e)
            }
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        Log.d(TAG, "Destroying")
    }

    private fun clearTextNotifications () {
        Log.d(TAG, "Clearing text notifications")
        val defaultApp = getDefaultTextingApp()
        activeNotifications?.forEach {
            if (it.packageName == defaultApp) {
                if (Build.VERSION.SDK_INT >= 21) {
                    cancelNotification(it.key)
                } else {
                    cancelNotification(defaultApp, it.tag, it.id)
                }
            }
        }
    }

    private fun postNotification (sbn: StatusBarNotification) {
        ThreadUtils.runInBackground(Runnable {
            try {
                Log.d(TAG, "Attempting to post notification...")
                val notification = getNotification(sbn) ?: return@Runnable

                // Encryption notification before sending over network
                if (encryptionHelper.enabled()) notification.encrypt(encryptionHelper)

                apiService.postNotification(notification).execute()
            } catch(e: Exception) {
                Crashlytics.logException(e)
                Log.e(TAG, e.toString())
            }
        })

    }

    private fun getNotification (sbn: StatusBarNotification): com.moduloapps.textto.model.Notification? {
        val extras = sbn.notification.extras
        val title = extras.get(Notification.EXTRA_TITLE)?.toString() ?: return null
        val subtitle = extras.get(Notification.EXTRA_TEXT)?.toString()
        var thumbnail = iconCache.get(sbn.packageName)

        // Not in cache. Get the icon and put it in the cache
        if (thumbnail == null) {
            thumbnail = getNotificationThumbnail(sbn)
            iconCache.put(sbn.packageName, thumbnail)
        } else {
            Log.d(TAG, "Notification icon cache hit for ${sbn.packageName}")
        }

        return com.moduloapps.textto.model.Notification(title, subtitle, thumbnail)
    }

    private fun getNotificationThumbnail (sbn: StatusBarNotification): String {
        val icon = packageManager.getApplicationIcon(sbn.packageName) as Drawable
        val bitmap = ImageUtils.drawableToBitmap(icon)
        return ImageUtils.compressPngImage(bitmap, NOTIFICATION_ICON_SIZE).toBase64()
    }

    private fun getDefaultTextingApp () = Telephony.Sms.getDefaultSmsPackage(applicationContext)

    /**
     * If the following conditions are met, a notification should be posted
     * 1) Are we currently in an active session (SmsObserverService.runningInForeground)
     * 2) Is this user a pro subscriber
     */
    private fun eligibleToPostNotification (): Boolean {
        //val isPro = sessionController.isPro()
        //return (SmsObserverService.runningInForeground && isPro)
        return SmsObserverService.runningInForeground
    }

    private fun shouldPostNotification (sbn: StatusBarNotification?): Boolean {
        if (sbn == null) return false
        // if (sbn.packageName == "android") return false
        val note = sbn.notification

        var eligible = true

        // Check to make sure this is not a text notification
        eligible = eligible && sbn.packageName != getDefaultTextingApp()

        // Check to make sure the notification is not silent
        eligible = eligible && !(note.sound == null && (note.vibrate == null || note.vibrate == EMPTY_VIBRATE))

        return eligible
    }

}