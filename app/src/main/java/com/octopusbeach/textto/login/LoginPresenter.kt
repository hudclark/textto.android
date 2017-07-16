package com.octopusbeach.textto.login

import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonObject
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.SessionController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 11/29/16.
 */

class LoginPresenter(val apiService: ApiService, val sessionController: SessionController) {

    private var view: View? = null
    private val TAG = "LoginPresenter"

    fun onTakeView(view: View?) {
        this.view = view
    }

    fun handleSignInResult(result: GoogleSignInResult) {
        if (result.isSuccess) {
            val token = result.signInAccount?.idToken
            if (token != null) {
                Log.d(TAG, "Received token from sign in result")
                getTokens(token)
                return
            }
        }
        view?.onLoginFailure("Unable to sign in")
    }

    private fun getTokens(token: String) {
        val data = JsonObject()
        data.addProperty("token", token)
        data.addProperty("platform", android.os.Build.MODEL)
        apiService.googleAuth(data).enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.e(TAG, "Unable to authenticate: $t")
                view?.onLoginFailure("Unable to sign in. Error: ${t?.message ?: "Unknown"}")
            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                val firebaseToken = FirebaseInstanceId.getInstance().token
                if (firebaseToken != null)
                    sessionController.setFirebaseToken(firebaseToken)

                val tokens = response?.body()?.getAsJsonObject("tokens")
                if (tokens != null) {
                    Log.d(TAG, "Received tokens from api")
                    sessionController.setAuthToken(tokens.get("access").asString)
                    sessionController.setRefreshToken(tokens.get("refresh").asString)
                    // everything was successful
                    view?.onLoginSuccess()
                } else {
                    view?.onLoginFailure("Unable to sign in. Try again in a minute")
                }
            }
        })
    }

    interface View {
        fun onLoginSuccess()
        fun onLoginFailure(error: String)
    }
}
