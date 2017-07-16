package com.octopusbeach.textto.api

import android.content.SharedPreferences
import android.util.Log
import com.google.gson.JsonObject
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by hudson on 7/15/17.
 */
class SessionController(var apiService: ApiService?, val prefs: SharedPreferences) {

    private val AUTH_TOKEN = "AUTH_TOKEN"
    private val REFRESH_TOKEN = "REFRESH_TOKEN"
    private val TAG = "Session Manager"

    private var token: String? = null

    fun getAuthToken(): String? {
        synchronized(this) {
            if (token != null) return token
        }
        return prefs.getString(REFRESH_TOKEN, null)

    }

    fun getRefreshToken() = prefs.getString(REFRESH_TOKEN, null)

    fun setAuthToken(token: String?) {
        synchronized(this) {
            this.token = token
        }
        val editor = prefs.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
    }

    fun setRefreshToken(token: String?) {
        val editor = prefs.edit()
        editor.putString(REFRESH_TOKEN, token)
        editor.apply()
    }

    fun isLoggedIn() = getRefreshToken() != null

    fun reAuthenticate() {
        val refreshToken = getRefreshToken()
        refreshToken?.let {
            try {
                val data = JsonObject()
                data.addProperty("token", it)
                val call = apiService?.refreshToken(data)
                val response = call?.execute()
                val token = response?.body()?.get("token")?.asString
                setAuthToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token")
            }
        }
    }

    fun setFirebaseToken(token: String) {
        val data = JsonObject()
        data.addProperty("firebaseId", token)
        apiService?.let {
            it.updateFirebaseId(data)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.d(TAG, "Set firebase id")
                    }, {
                        Log.e(TAG, "Error setting firebase id: $it")
                    })
        }
    }
}