package com.octopusbeach.textto.api

import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import com.google.android.gms.common.api.Api
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.octopusbeach.textto.model.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 9/5/16.
 */
object SessionManager {

    private val AUTH_TOKEN = "AUTH_TOKEN"
    private val TAG = "Session Manager"

    private var token: String? = null
    private var context: Context? = null


    fun init(context: Context) {
        this.context = context
    }

    fun getToken() = token

    fun setToken(token: String?) {
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

    fun setFirebaseToken(token: String?) {
        if (token != null) {
            val data = JsonObject()
            data.addProperty("firebaseId", token)
            val req = ApiClient.getInstance().create(SessionEndpointInterface::class.java).updateFirebaseId(data)
            req.enqueue(object: Callback<User> {
                override fun onResponse(call: Call<User>?, response: Response<User>?) {
                    Log.d(TAG, "Set firebase id: ${response?.body()?.firebaseId ?: "null"}")
                }

                override fun onFailure(call: Call<User>?, t: Throwable?) {
                    Log.e(TAG, t?.message.toString())
                }
            })
        }
    }

    fun updateUser(user: User) {
        val client = ApiClient.getInstance().create(SessionEndpointInterface::class.java)
        val body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), Gson().toJson(user, User::class.java))
        client.updateUser(user._id, body).enqueue(object: Callback<User> {
            override fun onResponse(call: Call<User>?, response: Response<User>?) {
            }
            override fun onFailure(call: Call<User>?, t: Throwable?) {
                Log.e(TAG, t?.message.toString())
            }
        })
    }
}