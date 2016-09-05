package com.octopusbeach.textto.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.octopusbeach.textto.model.User
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by hudson on 9/5/16.
 */
interface SessionEndpointInterface {

    @GET("user")
    fun getUser(): Call<JsonElement>

    @PUT("users/:id")
    fun updateUser(@Path("id") id: String, @Body data: RequestBody): Call<User>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Call<User>

}