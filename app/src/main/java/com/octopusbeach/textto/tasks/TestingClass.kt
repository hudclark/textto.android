package com.octopusbeach.textto.tasks

import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.content.FileProvider
import android.telephony.SmsManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/**
 * Created by hudson on 8/2/17.
 */
class TestingClass(val context: Context, val pendingIntent: PendingIntent) : Runnable {

    private interface LengthContainer {
        var length: Int
    }

    private val TAG = TestingClass::class.java.simpleName


    private val FILE_NAME = "text_0.txt"

    private val sSmilText =
    "<smil>" +
    "<head>" +
    "<layout>" +
    "<root-layout/>" +
    "<region height=\"100%%\" id=\"Text\" left=\"0%%\" top=\"0%%\" width=\"100%%\"/>" +
    "</layout>" +
    "</head>" +
    "<body>" +
    "<par dur=\"8000ms\">" +
    "<text src=\"%s\" region=\"Text\"/>" +
    "</par>" +
    "</body>" +
    "</smil>"

    override fun run() {
    }



    fun getSimNumber(context: Context): String {
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return telephonyManager.line1Number
    }


}