/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.settings

import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseActivity
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.encryption.EncryptionSetupFragment
import com.moduloapps.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 3/8/18.
 */
class SettingsActivity: BaseActivity(), EncryptionSetupFragment.OnFinishedSettingPasswordListener {
    private val TAG = SettingsActivity::class.java.simpleName

    @Inject lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        (application as BaseApplication).appComponent.inject(this)

        setContentView(R.layout.activity_settings)

        // init toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.title = resources.getString(R.string.settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)


        // Init password changed listener
        val encryptionSetup = supportFragmentManager.findFragmentById(R.id.encryption_fragment) as EncryptionSetupFragment
        encryptionSetup.setOnFinishedSettingPasswordListener(this)

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

    /**
     * Resync messages after master password has been changed.
     */
    override fun onFinishedSettingPassword() {
        ThreadUtils.runSingleThreadTask(Runnable {
            try {
                apiService.resyncMessages().execute()
            } catch (e: Exception) {
                Log.e(TAG, "Error resyncing messages")
                Crashlytics.logException(e)
            }
        })
    }

}