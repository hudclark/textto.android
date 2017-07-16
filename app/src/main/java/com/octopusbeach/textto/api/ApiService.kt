package com.octopusbeach.textto.api

import com.google.gson.JsonObject
import com.octopusbeach.textto.model.Contact
import com.octopusbeach.textto.model.Message
import com.octopusbeach.textto.model.ScheduledMessage
import com.octopusbeach.textto.model.User
import io.reactivex.Observable
import retrofit2.Call
import retrofit2.http.*

/**
 * Created by hudson on 7/14/17.
 */
interface ApiService {

    // contacts
    @POST("contacts")
    fun postContact(@Body contact: Contact): Observable<Map<String, Contact>>

    // messaging
    @POST("messages")
    fun createMessage(@Body message: Message): Call<Map<String, Message>>

    @GET("messages/last-id")
    fun getLastId(): Call<Map<String, Int>>

    @GET("scheduledMessages")
    fun getScheduledMessages(@Query("failed") failed: Boolean): Call<Map<String, List<ScheduledMessage>>>

    @DELETE("scheduledMessages/{id}")
    fun deleteScheduledMessage(@Path("id") id: String): Call<Void>

    // Session
    @POST("googleAuth")
    fun googleAuth(@Body token: JsonObject): Call<JsonObject>

    @POST("refreshToken")
    fun refreshToken(@Body token: JsonObject): Call<JsonObject>

    @POST("user/firebase-id")
    fun updateFirebaseId(@Body token: JsonObject): Observable<Map<String, User>>
}
