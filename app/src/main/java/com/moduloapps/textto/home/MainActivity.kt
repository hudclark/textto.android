package com.moduloapps.textto.home;

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.util.Log
import android.view.View
import android.widget.TextView
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.moduloapps.textto.BaseActivity
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.dialog.EnableNotificationsDialog
import com.moduloapps.textto.login.LoginActivity
import com.moduloapps.textto.onboarding.OnboardingActivity
import com.moduloapps.textto.service.ContactSyncService
import com.moduloapps.textto.service.NotificationListener
import com.moduloapps.textto.service.SmsObserverService
import com.moduloapps.textto.utils.PERMISSIONS_CODE
import com.moduloapps.textto.utils.getNeededPermissions
import com.moduloapps.textto.utils.requestPermissions
import javax.inject.Inject


class MainActivity: BaseActivity(),
        HomePresenter.View, View.OnClickListener {

    private val TAG = "MainActivity"

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var sessionController: SessionController

    private var presenter: HomePresenter? = null
    private var googleApiClient: GoogleApiClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as BaseApplication).appComponent.inject(this)

        // check for redirects
        if (!prefs.getBoolean(OnboardingActivity.ONBOARDING_COMPLETED, false)) {
            redirectToActivity(OnboardingActivity::class.java)
            return
        }
        if (sessionController.getRefreshToken() == null) {
            redirectToActivity(LoginActivity::class.java)
            return
        }

        // no redirects
        setContentView(R.layout.activity_main)
        initSyncButtons()

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()
        googleApiClient!!.connect()

        findViewById<View>(R.id.contact_support)?.setOnClickListener { presenter?.contactSupport() }
        findViewById<View>(R.id.log_out)?.setOnClickListener {
            presenter?.logOut(googleApiClient!!)
            Answers.getInstance().logCustom(CustomEvent("Log Out"))
        }

        // init presenter
        if (presenter == null)
            presenter = HomePresenter(apiService, sessionController, prefs)

        with (presenter!!) {
            onTakeView(this@MainActivity)
            loadUser()

            // On login start services and sync up
            if (intent.getBooleanExtra(LoginActivity.DID_LOG_IN, false)) {
                startService(Intent(this@MainActivity, SmsObserverService::class.java))
                syncMessages()

                // Sync contacts immediantly - may not have set firebase id yet.
                startService(Intent(this@MainActivity, ContactSyncService::class.java))
                //syncContacts()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        if (!sessionController.isLoggedIn()) {
            redirectToActivity(LoginActivity::class.java)
            return
        }

        val notificationsEnabled = NotificationListener.isEnabled(this)
        Log.d(TAG, "Notifications enabled: $notificationsEnabled")

        presenter?.checkPermissions()
        if (!intent.getBooleanExtra(LoginActivity.DID_LOG_IN, false)) {
            presenter?.loadSyncTimes()
        } else if (!notificationsEnabled) {
            openNotificationSettings()
        }

        // show/hide notification card
        findViewById<View>(R.id.notifications_card)?.visibility = if (notificationsEnabled) View.GONE else View.VISIBLE
        if (!notificationsEnabled) {
            findViewById<View>(R.id.enable_notifications)?.setOnClickListener { openNotificationSettings() }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.onTakeView(null)
        if (!isChangingConfigurations)
            presenter = null
        googleApiClient?.disconnect()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_CODE) {
            presenter?.checkPermissions()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun setDisplayEmail(email: String) {
        val displayEmail = findViewById(R.id.user_email) as TextView
        displayEmail?.text = email
    }

    override fun showRequestPermissions() {
        val snackbar = Snackbar.make(findViewById(R.id.main_activity_root), R.string.missing_permissions, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.enable, {
            Answers.getInstance().logCustom(CustomEvent("Enable Permissions Click"))
            requestPermissions(this, getNeededPermissions(this@MainActivity))
            snackbar.dismiss()
        })
        snackbar.setTextColor(R.color.textWhite)
        snackbar.show()
    }

    override fun setContactsLastSynced(message: String) {
        val contactsLastSynced = findViewById(R.id.contacts_last_synced) as TextView?
        contactsLastSynced?.text = message
    }

    override fun setMessagesLastSynced(message: String) {
        val messagesLastSynced = findViewById(R.id.messages_last_synced) as TextView?
        messagesLastSynced?.text = message
    }

    override fun setSyncingMessages(syncing: Boolean) {
        findViewById<View>(R.id.messages_sync)?.setVisible(!syncing)
        findViewById<View>(R.id.messages_loader)?.setVisible(syncing)
    }

    override fun setSyncingContacts(syncing: Boolean) {
        findViewById<View>(R.id.contacts_sync)?.setVisible(!syncing)
        findViewById<View>(R.id.contacts_loader)?.setVisible(syncing)
    }

    override fun onClick(v: View) {
        if (!sessionController.isLoggedIn()) {
            redirectToActivity(LoginActivity::class.java)
            return
        }
        when (v.id) {
            R.id.messages_sync -> {
                presenter?.syncMessages()
                Answers.getInstance().logCustom(CustomEvent("Sync Messages Click"))
                //ThreadUtils.runSingleThreadTask(TestingClass(this, apiService))
            }

            R.id.contacts_sync -> {
                presenter?.syncContacts()
                Answers.getInstance().logCustom(CustomEvent("Sync Contacts Click"))
            }
        }
    }

    private fun openNotificationSettings() {
        EnableNotificationsDialog(this).show()
    }

    private fun initSyncButtons() {
        findViewById<View>(R.id.contacts_sync)?.setOnClickListener(this)
        findViewById<View>(R.id.messages_sync)?.setOnClickListener(this)
    }

    override fun redirectToLogin() {
        redirectToActivity(LoginActivity::class.java)
    }
}
