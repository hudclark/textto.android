package com.octopusbeach.textto.api

import com.google.gson.JsonObject
import com.octopusbeach.textto.model.User
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by hudson on 9/5/16.
 */
interface SessionEndpointInterface {

    @POST("googleAuth")
    fun googleAuth(@Body token: JsonObject): Call<JsonObject>

    @POST("refreshToken")
    fun refreshToken(@Body token: JsonObject): Call<JsonObject>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Call<Map<String, User>>

}