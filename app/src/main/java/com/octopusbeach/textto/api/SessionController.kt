package com.octopusbeach.textto.api

import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.gson.JsonObject

/**
 * Created by hudson on 7/15/17.
 */
class SessionController(var apiService: PublicApiService, val prefs: SharedPreferences) {

    private val TAG = "Session Manager"

    private val AUTH_TOKEN = "AUTH_TOKEN"
    private val REFRESH_TOKEN = "REFRESH_TOKEN"

    private val PROFILE_IMAGE = "PROFILE_IMAGE"
    private val DISPLAY_NAME = "DISPLAY_NAME"

    val TOKEN_HEADER = "x-access-token"

    private var token: String? = null

    @Synchronized fun getAuthToken(): String? {
        if (token != null) return token
        return prefs.getString(REFRESH_TOKEN, null)

    }

    @Synchronized fun getRefreshToken() = prefs.getString(REFRESH_TOKEN, null)

    @Synchronized fun setAuthToken(token: String?) {
        this.token = token
        val editor = prefs.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
    }

    @Synchronized fun setRefreshToken(token: String?) {
        val editor = prefs.edit()
        editor.putString(REFRESH_TOKEN, token)
        editor.apply()
    }

    fun isLoggedIn() = getRefreshToken() != null

    fun saveSignInAccount(account: GoogleSignInAccount?) {
        account?.let {
            Log.d(TAG, "Saving google sign in account")
            val editor = prefs.edit()
            editor.putString(PROFILE_IMAGE, it.photoUrl.toString())
            editor.putString(DISPLAY_NAME, it.displayName)
            editor.apply()
        }
    }

    fun getProfileImage(): String = prefs.getString(PROFILE_IMAGE, "")
    fun getDisplayName(): String = prefs.getString(DISPLAY_NAME, "")

    fun reAuthenticate() {
        setAuthToken(null)
        val refreshToken = getRefreshToken()
        refreshToken?.let {
            try {
                val data = JsonObject()
                data.addProperty("token", it)
                val call = apiService.refreshToken(data)
                val response = call.execute()
                val token = response?.body()?.get("token")?.asString
                if (token == null) {
                    Log.d(TAG, "Logged out")
                    setRefreshToken(null)
                }
                setAuthToken(token)
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token")
            }
        }
    }
}