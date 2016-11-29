package com.octopusbeach.textto.fragments

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonObject
import com.octopusbeach.textto.api.ApiClient
import com.octopusbeach.textto.api.SessionEndpointInterface
import com.octopusbeach.textto.api.SessionManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 11/29/16.
 */

class LoginPresenter {

    private var view: LoginFragment? = null
    private val TAG = "LoginPresenter"

    fun onTakeView(view: LoginFragment?) {
        this.view = view
    }

    fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val account = result.signInAccount
            val token = account?.idToken
            if (token != null)
                getTokens(token)
        } else
            Log.e(TAG, result.status.toString())
    }

    private fun getTokens(token: String) {
        val data = JsonObject()
        data.addProperty("token", token)
        val client = ApiClient.getInstance().create(SessionEndpointInterface::class.java)
        client.googleAuth(data).enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.e(TAG, "Unable to authenticate: $t")
                view?.onLoginFailure()
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
                view?.onLoginSuccess()
            }
        })
    }
}
