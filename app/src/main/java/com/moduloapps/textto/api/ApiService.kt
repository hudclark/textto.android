package com.moduloapps.textto.api

import com.google.gson.JsonObject
import com.moduloapps.textto.model.*
import io.reactivex.Observable
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by hudson on 7/14/17.
 */
interface ApiService {

    @POST("v2/contacts/bulk")
    fun postContacts(@Body contacts: List<Contact>): Call<String>

    @POST("messages/bulk")
    fun createMessages(@Body messages: List<Message>): Call<String>

    @POST("mmsparts/bulk")
    fun createMmsParts(@Body parts: List<MmsPart>): Call<Map<String, List<MmsPart?>>>

    @POST("user/sync-contacts")
    fun syncContacts(): Call<String>

    @PUT
    fun putMmsImage(@Url url: String, @Body body: RequestBody): Call<Void>

    @PUT
    fun uploadImage(@Url url: String, @Body body: RequestBody): Call<Void>

    @GET
    fun getFile(@Url url: String): Call<ResponseBody>

    @GET("messages/status?encrypted=1")
    fun getStatusUpdate(): Call<StatusUpdate>

    @PUT("scheduledMessages/{id}")
    fun updateScheduledMessage(@Path("id") id: String, @Body scheduledMessage: ScheduledMessage):
            Call<Map<String, ScheduledMessage>>

    @DELETE("scheduledMessages/{id}")
    fun deleteScheduledMessage(@Path("id") id: String): Call<Void>

    @POST("scheduledMessages/{id}/reportFailure")
    fun reportFailed(@Path("id") id: String, @Body body: JsonObject): Call<String>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Call<Map<String, String>>

    @POST("user/startInitialSync")
    fun startInitialSync(): Call<Map<String, String>>

    @POST("user/endInitialSync")
    fun endInitialSync(): Call<Map<String, String>>

    @POST("user/resyncMessages")
    fun resyncMessages(): Call<Map<String, String>>

    @GET("ping")
    fun ping(): Call<String>

    @POST("revokeToken")
    fun revokeToken(@Query("refreshToken") refreshToken: String): Observable<Map<String, String>>

    @POST("notifications")
    fun postNotification(@Body notification: Notification): Call<String>
}
