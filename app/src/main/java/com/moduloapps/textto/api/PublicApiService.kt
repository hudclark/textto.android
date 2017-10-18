package com.moduloapps.textto.api

import com.google.gson.JsonObject
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by hudson on 7/19/17.
 */
interface PublicApiService {

    // Session
    @POST("googleAuth")
    fun googleAuth(@Body token: JsonObject): Call<JsonObject>

    @POST("refreshToken")
    fun refreshToken(@Body token: JsonObject): Call<JsonObject>

}
