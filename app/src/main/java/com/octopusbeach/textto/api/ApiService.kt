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

    // contacts
    @POST("contacts")
    fun postContact(@Body contact: Contact): Observable<Map<String, Contact>>

    @POST("contacts/bulk")
    fun postContacts(@Body contacts: List<Contact>): Call<String>

    // messaging
    @POST("messages")
    fun createMessage(@Body message: Message): Call<Map<String, Message>>

    @POST("messages/bulk")
    fun createMessages(@Body messages: List<Message>): Call<String>

    @GET("messages/last-update")
    fun getLastUpdated(): Call<Map<String, Long>>

    @POST("mmsparts")
    fun createMmsPart(@Body part: MmsPart): Call<Map<String, MmsPart>>

    fun putMmsImage(@Url url: String, @Body body: RequestBody): Call<Int>

    @GET("scheduledMessages")
    fun getScheduledMessages(@Query("failed") failed: Boolean): Call<Map<String, List<ScheduledMessage>>>

    @DELETE("scheduledMessages/{id}")
    fun deleteScheduledMessage(@Path("id") id: String): Call<Void>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Observable<Map<String, User>>
}
