package com.octopusbeach.textto.api

import com.google.gson.JsonObject
import com.octopusbeach.textto.model.*
import io.reactivex.Observable
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.http.*

/**
 * Created by hudson on 7/14/17.
 */
interface ApiService {

    @POST("contacts/bulk")
    fun postContacts(@Body contacts: List<Contact>): Call<String>

    @POST("messages/bulk")
    fun createMessages(@Body messages: List<Message>): Call<String>

    @POST("mmsparts/bulk")
    fun createMmsParts(@Body parts: List<MmsPart>): Call<Map<String, List<MmsPart?>>>

    @PUT
    fun putMmsImage(@Url url: String, @Body body: RequestBody): Call<Int>

    @GET("messages/status")
    fun getStatusUpdate(): Call<StatusUpdate>

    @PUT("scheduledMessages/{id}")
    fun updateScheduledMessage(@Path("id") id: String, @Body scheduledMessage: ScheduledMessage):
            Call<Map<String, ScheduledMessage>>

    @DELETE("scheduledMessages/{id}")
    fun deleteScheduledMessage(@Path("id") id: String): Call<Void>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Observable<Map<String, String>>

    @GET("ping")
    fun ping(): Call<String>

    @POST("revokeToken")
    fun revokeToken(@Query("refreshToken") refreshToken: String): Observable<Map<String, String>>
}
