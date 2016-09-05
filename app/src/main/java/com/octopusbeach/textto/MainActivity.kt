package com.octopusbeach.textto;

import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.iid.FirebaseInstanceId
import com.octopusbeach.textto.api.SessionManager


class MainActivity: AppCompatActivity() {

    private val TAG = "MainActivity"
    private val RC_SIGN_IN = 1
    private val AUTH_TOKEN = "auth_token"

    private lateinit var gClient: GoogleApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // check logged in status
        if (SessionManager.getToken() == null)
            initSignIn()
        //todo remove
        findViewById(R.id.login_btn)!!.visibility = View.VISIBLE
    }

    private fun initSignIn() {
        // Start google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .build()

        gClient = GoogleApiClient.Builder(this).addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

        findViewById(R.id.login_btn)!!.visibility = View.VISIBLE
        findViewById(R.id.login_btn)!!.setOnClickListener { signIn() }
    }

    private fun signIn() {
        intent = Auth.GoogleSignInApi.getSignInIntent(gClient)
        startActivityForResult(intent, RC_SIGN_IN)
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val acc = result.signInAccount
            val token = acc?.idToken
            SessionManager.setToken(token)
            val firebaseToken = FirebaseInstanceId.getInstance().token
            SessionManager.setFirebaseToken(firebaseToken)

            // todo update ui
        } else
            Log.e(TAG, result.status.toString())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
            handleSignInResult(result)
        }
    }

}
