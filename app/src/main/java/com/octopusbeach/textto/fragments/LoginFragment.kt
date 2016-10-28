package com.octopusbeach.textto.fragments

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonObject
import com.octopusbeach.textto.R
import com.octopusbeach.textto.api.ApiClient
import com.octopusbeach.textto.api.SessionEndpointInterface
import com.octopusbeach.textto.api.SessionManager
import com.octopusbeach.textto.utils.MessageUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 10/27/16.
 */

class LoginFragment: Fragment {

    private lateinit var googleClient: GoogleApiClient
    private val TAG = "LoginFragment"

    private lateinit var listener: OnAuthListener

     constructor(listener: OnAuthListener): super() {
        this.listener = listener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_login, container, false)

        v.findViewById(R.id.login_btn).setOnClickListener { login() }

        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .build()
        googleClient = GoogleApiClient.Builder(activity).addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        handleSignInResult(result)
    }

    private fun login() {
        val intent = Auth.GoogleSignInApi.getSignInIntent(googleClient)
        startActivityForResult(intent, 1)
    }

    private fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount
            val token = account?.idToken
            if (token != null)
                getTokens(token)
        } else
            Log.e(TAG, result.status.toString())
    }

    private fun getTokens(token: String) {
        view.findViewById(R.id.login_btn).visibility = View.GONE
        view.findViewById(R.id.spinner).visibility = View.VISIBLE
        val data = JsonObject()
        data.addProperty("token", token)
        val call = ApiClient.getInstance().create(SessionEndpointInterface::class.java).googleAuth(data)
        call.enqueue(object: Callback<JsonObject> {

            private fun stopLoading() {
                //view.findViewById(R.id.login_btn).visibility = View.VISIBLE
                view.findViewById(R.id.spinner).visibility = View.GONE
            }

            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                stopLoading()
            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                val tokens = response?.body()?.getAsJsonObject("tokens")
                if (tokens != null) {
                    SessionManager.setAccessToken(tokens.get("access").asString)
                    SessionManager.setRefreshToken(tokens.get("refresh").asString)
                }
                val firebaseToken = FirebaseInstanceId.getInstance().token
                if (firebaseToken != null)
                    SessionManager.setFirebaseToken(firebaseToken)
                MessageUtils.updateMessages(activity)
                stopLoading()
            }
        })
    }

    interface OnAuthListener {
        fun onAuth()
    }

}
