/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.dialog

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.moduloapps.textto.R

/**
 * Created by hudson on 1/3/18.
 */
class EnableNotificationsDialog(private val activity: Activity) {

    fun show() {


        val inflater = activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.dialog_enable_notifications, null)

        val dialog = AlertDialog.Builder(activity)
                .setView(view)
                .setCancelable(false)
                .create()

        view.findViewById<View>(R.id.btn_enable).setOnClickListener {
            dialog.dismiss()
            activity.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
            Answers.getInstance().logCustom(CustomEvent("Enable Notifications"))
        }

        view.findViewById<View>(R.id.btn_dismiss).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()

    }

}