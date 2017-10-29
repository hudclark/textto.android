package com.moduloapps.textto.home;

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.login.LoginActivity
import com.moduloapps.textto.onboarding.OnboardingActivity
import com.moduloapps.textto.service.NotificationListener
import com.moduloapps.textto.service.SmsObserverService
import com.moduloapps.textto.utils.PERMISSIONS_CODE
import com.moduloapps.textto.utils.getNeededPermissions
import com.moduloapps.textto.utils.requestPermissions
import io.fabric.sdk.android.Fabric
import javax.inject.Inject


class MainActivity: AppCompatActivity(),
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
        Fabric.with(this, Crashlytics())

        // check for redirects
        if (!prefs.getBoolean(OnboardingActivity.ONBOARDING_COMPLETED, false)) {
            redirect(OnboardingActivity::class.java)
            return
        }
        if (sessionController.getRefreshToken() == null) {
            redirect(LoginActivity::class.java)
            return
        }

        // no redirects
        setContentView(R.layout.activity_main)
        initSyncButtons()

        googleApiClient = GoogleApiClient.Builder(this)
                .addApi(Auth.GOOGLE_SIGN_IN_API)
                .build()
        googleApiClient!!.connect()

        findViewById(R.id.contact_support).setOnClickListener { presenter?.contactSupport() }
        findViewById(R.id.log_out).setOnClickListener {
            presenter?.logOut(googleApiClient!!)
            Answers.getInstance().logCustom(CustomEvent("Log Out"))
        }

        // init presenter
        if (presenter == null)
            presenter = HomePresenter(apiService, sessionController, prefs)
        presenter!!.onTakeView(this)
        presenter!!.loadUser()

        if (intent.getBooleanExtra(LoginActivity.DID_LOG_IN, false)) {
            startService(Intent(this, SmsObserverService::class.java))
            presenter?.syncMessages()
            presenter?.syncContacts()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!sessionController.isLoggedIn()) {
            redirect(LoginActivity::class.java)
            return
        }
        presenter?.checkPermissions()
        if (!intent.getBooleanExtra(LoginActivity.DID_LOG_IN, false)) {
            presenter?.loadSyncTimes()
        }

        val notificationsEnabled = NotificationListener.isEnabled(this)
        findViewById(R.id.notification_card).visibility = if (notificationsEnabled) View.GONE else View.VISIBLE
        if (!notificationsEnabled) {
            findViewById(R.id.enable_notifications).setOnClickListener { openNotificationSettings() }
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

    override fun setDisplayName(name: String) {
        val displayName = findViewById(R.id.user_name) as TextView
        displayName.text = name
    }

    override fun setDisplayEmail(email: String) {
        val displayEmail = findViewById(R.id.user_email) as TextView
        displayEmail.text = email
    }

    override fun setPhotoUrl(url: String) {
        val profilePicture = findViewById(R.id.profile_picture) as ImageView
        if (url.isNotEmpty()) {
            Glide.with(this)
                    .load(url)
                    .into(profilePicture)
        }
    }

    override fun showRequestPermissions() {
        val snackbar = Snackbar.make(findViewById(R.id.main_activity_root), R.string.missing_permissions, Snackbar.LENGTH_INDEFINITE)
        snackbar.setAction(R.string.enable, {
            Answers.getInstance().logCustom(CustomEvent("Enable Permissions Click"))
            requestPermissions(this, getNeededPermissions(this@MainActivity))
            snackbar.dismiss()
        })
        snackbar.show()
    }

    override fun setContactsLastSynced(message: String) {
        val contactsLastSynced = findViewById(R.id.contacts_last_synced) as TextView
        contactsLastSynced.text = message
    }

    override fun setMessagesLastSynced(message: String) {
        val messagesLastSynced = findViewById(R.id.messages_last_synced) as TextView
        messagesLastSynced.text = message
    }

    override fun redirectToLogin() {
        redirect(LoginActivity::class.java)
    }

    override fun onClick(v: View) {
        if (!sessionController.isLoggedIn()) {
            redirect(LoginActivity::class.java)
            return
        }
        when (v.id) {
            R.id.messages_sync -> {
                presenter?.syncMessages()
                Answers.getInstance().logCustom(CustomEvent("Sync Messages Click"))
            }

            R.id.contacts_sync -> {
                presenter?.syncContacts()
                Answers.getInstance().logCustom(CustomEvent("Sync Contacts Click"))
            }
        }
    }

    private fun openNotificationSettings() {
        AlertDialog.Builder(this)
                .setTitle(R.string.enable_notification_access)
                .setMessage(R.string.enable_notification_message)
                .setPositiveButton(R.string.enable, { d, _ ->
                    startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                    Answers.getInstance().logCustom(CustomEvent("Enable Notifications"))
                    d.dismiss()
                })
                .show()
    }

    private fun initSyncButtons() {
        findViewById(R.id.contacts_sync).setOnClickListener(this)
        findViewById(R.id.messages_sync).setOnClickListener(this)
    }

    private fun redirect(activity: Class<*>) {
        Log.d(TAG, "Redirecting to $activity")
        val intent = Intent(this, activity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
