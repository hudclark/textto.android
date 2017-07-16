package com.octopusbeach.textto;

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.login.LoginActivity
import com.octopusbeach.textto.onboarding.OnboardingActivity
import javax.inject.Inject


class MainActivity: AppCompatActivity() {

    private val TAG = "MainActivity"
    val RC_SIGN_IN = 2

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var sharedPrefs: SharedPreferences
    @Inject lateinit var sessionController: SessionController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as BaseApplication).appComponent.inject(this)

        // check for onboarding
        if (!sharedPrefs.getBoolean(OnboardingActivity.ONBOARDING_COMPLETED, false)) {
            redirect(OnboardingActivity::class.java)
            return
        }

        // check logged in status
        if (sessionController.getRefreshToken() == null) {
            redirect(LoginActivity::class.java)
            return
        }

        setContentView(R.layout.activity_main)
    }

    private fun redirect(activity: Class<*>) {
        Log.d(TAG, "Redirecting to $activity")
        val intent = Intent(this, activity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
        }
    }
}
