/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.settings

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.moduloapps.textto.BaseActivity
import com.moduloapps.textto.R

/**
 * Created by hudson on 3/8/18.
 */
class SettingsActivity: BaseActivity() {
    private val TAG = SettingsActivity::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // init toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = resources.getString(R.string.settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }


    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

}