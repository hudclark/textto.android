package com.octopusbeach.textto.home;

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
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
import javax.inject.Inject


class MainActivity: AppCompatActivity(),
        HomePresenter.View, View.OnClickListener {

    private val TAG = "MainActivity"
    val RC_SIGN_IN = 2

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
        SmsObserverService.ensureStarted(this)

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
        presenter?.loadSyncTimes()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.onTakeView(null)
        if (!isChangingConfigurations)
            presenter = null
    }

    override fun setDisplayName(name: String) {
        val displayName = findViewById(R.id.user_name) as TextView
        displayName.text = name
    }

    override fun setPhotoUrl(url: String) {
        val profilePicture = findViewById(R.id.profile_picture) as ImageView
        if (url.isNotEmpty()) {
            Glide.with(this)
                    .load(url)
                    .into(profilePicture)
        }
    }

    override fun setContactsLastSynced(message: String) {
        val contactsLastSynced = findViewById(R.id.contacts_last_synced) as TextView
        contactsLastSynced.text = message
    }

    override fun setMessagesLastSynced(message: String) {
        val messagesLastSynced = findViewById(R.id.messages_last_synced) as TextView
        messagesLastSynced.text = message
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
