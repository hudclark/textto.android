package com.moduloapps.textto.login

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.firebase.iid.FirebaseInstanceId
import com.google.gson.JsonObject
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.PublicApiService
import com.moduloapps.textto.api.SessionController
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 11/29/16.
 */

class LoginPresenter(val apiService: PublicApiService, val sessionController: SessionController) {

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
                logIn(token, result.signInAccount)
                return
            }
        }
        view?.onLoginFailure("Unable to sign in")
    }

    private fun logIn(token: String, account: GoogleSignInAccount?) {
        val context = view?.getBaseApplication() ?: return
        val data = JsonObject()
        data.addProperty("token", token)
        data.addProperty("platform", android.os.Build.MODEL)
        data.addProperty("deviceId", getDeviceId(context))
        apiService.googleAuth(data).enqueue(object: Callback<JsonObject> {
            override fun onFailure(call: Call<JsonObject>?, t: Throwable?) {
                Log.e(TAG, "Unable to authenticate: $t")
                view?.onLoginFailure("Unable to sign in. Error: ${t?.message ?: "Unknown"}")
                Crashlytics.logException(t)
            }

            override fun onResponse(call: Call<JsonObject>?, response: Response<JsonObject>?) {
                val tokens = response?.body()?.getAsJsonObject("tokens")
                if (tokens != null) {
                    Log.d(TAG, "Received tokens from api")
                    sessionController.setAuthToken(tokens.get("access").asString)
                    sessionController.setRefreshToken(tokens.get("refresh").asString)
                    sessionController.saveSignInAccount(account)

                    val firebaseToken = FirebaseInstanceId.getInstance().token
                    if (firebaseToken != null)
                        view?.getBaseApplication()?.setFirebaseToken(firebaseToken)

                    // everything was successful
                    view?.onLoginSuccess()
                } else {
                    Crashlytics.log(1, TAG, "Response did not return tokens")
                    view?.onLoginFailure("Unable to sign in. Try again in a minute")
                }
            }
        })
    }

    private fun getDeviceId(context: Context) = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    interface View {
        fun onLoginSuccess()
        fun onLoginFailure(error: String)
        fun getBaseApplication(): BaseApplication
    }
}
