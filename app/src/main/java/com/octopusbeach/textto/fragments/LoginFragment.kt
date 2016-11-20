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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 10/27/16.
 */

class LoginFragment: Fragment() {

    private val TAG = "LoginFragment"

    private lateinit var googleClient: GoogleApiClient
    private var listener: OnAuthListener? = null

    fun setAuthListener(listener: OnAuthListener) {
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
        toggleLoadingView(true)
        val data = JsonObject()
        data.addProperty("token", token)
        val client = ApiClient.getInstance().create(SessionEndpointInterface::class.java)
        client.googleAuth(data).enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                toggleLoadingView(false)
                Log.e(TAG, "Unable to authenticate: $t")
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
                // everything was successful
                toggleLoadingView(false)
                listener?.onAuth()
            }
        })
    }

    private fun toggleLoadingView(isLoading: Boolean) {
        view.findViewById(R.id.login_btn).visibility = if (isLoading) View.GONE else View.VISIBLE
        view.findViewById(R.id.spinner).visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    /**
     * Provide an authentication callback
     */
    interface OnAuthListener {
        fun onAuth()
    }
}
