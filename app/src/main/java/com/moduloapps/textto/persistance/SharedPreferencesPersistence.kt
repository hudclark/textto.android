/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.persistance

import android.content.SharedPreferences

/**
 * Created by hudson on 3/8/18.
 */
class SharedPreferencesPersistence(private val prefs: SharedPreferences): Persistence {

    override fun getString(key: String, default: String?) = prefs.getString(key, default)

    override fun putString(key: String, value: String?) {
        val editor = prefs.edit()
        editor.putString(key, value)
        editor.apply()
    }

}