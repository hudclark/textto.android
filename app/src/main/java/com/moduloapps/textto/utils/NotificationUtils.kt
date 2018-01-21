/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi

/**
 * Created by hudson on 1/10/18.
 */

const val SYNC_CHANNEL_ID = "sync"
const val SYNC_CHANNEL_NAME = "Sync"
const val SYNC_CHANNEL_DESCRIPTION = "Notification Channel for Sync"

@RequiresApi(Build.VERSION_CODES.O)
fun createSyncChannel(context: Context) {
    // TODO hmmm what importance. What's worse - 'This app is using battery' or constantly notification?
    val channel = NotificationChannel(SYNC_CHANNEL_ID, SYNC_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
    channel.description = SYNC_CHANNEL_DESCRIPTION

    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.createNotificationChannel(channel)
}
