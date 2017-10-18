package com.moduloapps.textto.api

import android.content.SharedPreferences
import android.util.Log
import com.crashlytics.android.Crashlytics
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
    private val DISPLAY_EMAIL = "DISPLAY_EMAIL"

    val TOKEN_HEADER = "x-access-token"

    private var token: String? = null

    @Synchronized fun getAuthToken(): String? {
        if (token != null) return token
        return prefs.getString(AUTH_TOKEN, null)
    }

    @Synchronized fun getRefreshToken() = prefs.getString(REFRESH_TOKEN, null)

    fun clearTokens () {
        setAuthToken(null)
        setRefreshToken(null)
    }

    @Synchronized fun setAuthToken(token: String?) {
        this.token = token
        val editor = prefs.edit()
        editor.putString(AUTH_TOKEN, token)
        editor.apply()
        Log.d(TAG, "Saved auth token")
    }

    @Synchronized fun setRefreshToken(token: String?) {
        val editor = prefs.edit()
        editor.putString(REFRESH_TOKEN, token)
        editor.apply()
        Log.d(TAG, "Saved refresh token")
    }

    fun isLoggedIn() = getRefreshToken() != null

    fun saveSignInAccount(account: GoogleSignInAccount?) {
        account?.let {
            Log.d(TAG, "Saving google sign in account")
            Log.d(TAG, "${it.grantedScopes}")
            val editor = prefs.edit()
            editor.putString(PROFILE_IMAGE, it.photoUrl.toString())
            editor.putString(DISPLAY_NAME, it.displayName)
            editor.putString(DISPLAY_EMAIL, it.email)
            editor.apply()

            Crashlytics.setUserEmail(it.email)
            Crashlytics.setUserIdentifier(it.email)
        }
    }

    fun getProfileImage(): String = prefs.getString(PROFILE_IMAGE, "")
    fun getDisplayName(): String = prefs.getString(DISPLAY_NAME, "")
    fun getDisplayEmail(): String = prefs.getString(DISPLAY_EMAIL, "")

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