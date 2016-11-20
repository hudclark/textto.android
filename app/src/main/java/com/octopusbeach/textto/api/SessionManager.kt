package com.octopusbeach.textto.api

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.gson.JsonObject
import com.octopusbeach.textto.model.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 9/5/16.
 */
object SessionManager {

    private val AUTH_TOKEN = "AUTH_TOKEN"
    private val REFRESH_TOKEN = "REFRESH_TOKEN"
    private val TAG = "Session Manager"

    private var token: String? = null
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
    }

    fun getToken(): String? {
        if (token != null)
            return token
        if (context != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString(AUTH_TOKEN, null)
        }
        return null
    }

    private fun getRefreshToken(): String? {
        if (context != null) {
            val prefs = PreferenceManager.getDefaultSharedPreferences(context)
            return prefs.getString(REFRESH_TOKEN, null)
        }
        return null
    }

    fun setAccessToken(token: String?) {
        this.token = token
        if (context == null) {
            Log.e(TAG, "No context")
        } else {
            Log.d(TAG, "Saving session")
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(AUTH_TOKEN, token)
            editor.apply()
        }
    }

    fun setRefreshToken(token: String?) {
        this.token = token
        if (context == null) {
            Log.e(TAG, "No context")
        } else {
            val editor = PreferenceManager.getDefaultSharedPreferences(context).edit()
            editor.putString(REFRESH_TOKEN, token)
            editor.apply()
        }
    }

    fun reAuth() {
        val refreshToken = getRefreshToken()
        if (refreshToken != null) {

            try {
                val data = JsonObject()
                data.addProperty("token", refreshToken)
                val call = ApiClient.getInstance().create(SessionEndpointInterface::class.java).refreshToken(data)
                val response = call.execute()
                val token = response?.body()?.get("token")?.asString
                if (token != null) {
                    setAccessToken(token)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing token")
            }
        }
    }

    fun setFirebaseToken(token: String?) {
        if (token != null) {
            val data = JsonObject()
            data.addProperty("firebaseId", token)
            val req = ApiClient.getInstance().create(SessionEndpointInterface::class.java).updateFirebaseId(data)
            req.enqueue(object: Callback<Map<String, User>> {
                override fun onResponse(call: Call<Map<String, User>>?, response: Response<Map<String, User>>) {
                    Log.d(TAG, "Set firebase id: ${response.body()?.get("user")?.firebaseId ?: "null"}")
                }

                override fun onFailure(call: Call<Map<String, User>>?, t: Throwable?) {
                    Log.e(TAG, t?.message.toString())
                }
            })
        }
    }
}