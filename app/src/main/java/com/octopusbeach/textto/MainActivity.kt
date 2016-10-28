package com.octopusbeach.textto;

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.octopusbeach.textto.fragments.LoginFragment
import com.octopusbeach.textto.service.SmsListenerService
import com.octopusbeach.textto.utils.MessageUtils


class MainActivity: AppCompatActivity(), LoginFragment.OnAuthListener {

    private val TAG = "MainActivity"
    private val RC_SIGN_IN = 1

    private lateinit var gClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // check logged in status
        if (true) {
            val frag = LoginFragment(this)
            val ft = fragmentManager.beginTransaction()
            ft.replace(R.id.fragment_content, frag)
            ft.commit()
        }
        startService(Intent(this, SmsListenerService::class.java))
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_content)
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onAuth() {
        MessageUtils.updateMessages(this)
    }

}
