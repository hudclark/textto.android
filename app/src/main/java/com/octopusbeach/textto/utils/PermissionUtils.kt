package com.octopusbeach.textto.utils

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat

/**
 * Created by hudson on 7/15/17.
 */

val BASE_PERMISSIONS = arrayOf(
        android.Manifest.permission.RECEIVE_SMS,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_SMS,
        android.Manifest.permission.READ_CONTACTS
)

val PERMISSIONS_CODE = 0

fun getNeededPermissions(context: Context): ArrayList<String> {
    val neededPermissions = ArrayList<String>()
    BASE_PERMISSIONS.forEach {
        if (ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED)
            neededPermissions.add(it)
    }
    return neededPermissions
}

fun requestPermissions(activity: Activity, permissions: List<String>) {
    ActivityCompat.requestPermissions(activity, permissions.toTypedArray(), PERMISSIONS_CODE)
}