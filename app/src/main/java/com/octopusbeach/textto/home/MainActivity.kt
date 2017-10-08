package com.octopusbeach.textto.home;

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.R
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.login.LoginActivity
import com.octopusbeach.textto.onboarding.OnboardingActivity
import com.octopusbeach.textto.service.SmsObserverService
import com.octopusbeach.textto.utils.PERMISSIONS_CODE
import com.octopusbeach.textto.utils.getNeededPermissions
import com.octopusbeach.textto.utils.requestPermissions
import javax.inject.Inject


class MainActivity: AppCompatActivity(),
        HomePresenter.View, View.OnClickListener {

    private val TAG = "MainActivity"

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences
    @Inject lateinit var sessionController: SessionController

    private var presenter: HomePresenter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as BaseApplication).appComponent.inject(this)

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

        findViewById(R.id.contact_support).setOnClickListener { presenter?.contactSupport() }
        findViewById(R.id.log_out).setOnClickListener { presenter?.logOut() }

        // init presenter
        if (presenter == null)
            presenter = HomePresenter(apiService, sessionController, prefs)
        presenter!!.onTakeView(this)
        presenter!!.loadUser()

    }

    override fun onResume() {
        super.onResume()
        if (!sessionController.isLoggedIn()) {
            redirect(LoginActivity::class.java)
            return
        }
        presenter?.checkPermissions()
        if (intent.getBooleanExtra(LoginActivity.DID_LOG_IN, false)) {
            startService(Intent(this, SmsObserverService::class.java))
            presenter?.syncMessages()
            presenter?.syncContacts()
        } else {
            presenter?.loadSyncTimes()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.onTakeView(null)
        if (!isChangingConfigurations)
            presenter = null
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
            R.id.messages_sync -> presenter?.syncMessages()
            R.id.contacts_sync -> presenter?.syncContacts()
        }
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
