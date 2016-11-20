package com.octopusbeach.textto.api

import com.octopusbeach.textto.model.Message
import com.octopusbeach.textto.model.ScheduledMessage
import retrofit2.Call
import retrofit2.http.*
import java.util.*

/**
 * Created by hudson on 9/5/16.
 */
interface MessageEndpointInterface {

    @POST("messages")
    fun createMessage(@Body message: Message): Call<Map<String, Message>>

    @GET("messages/last-id")
    fun getLastId(): Call<Map<String, Int>>

    @GET("scheduledMessages")
    fun getScheduledMessages(): Call<Map<String, List<ScheduledMessage>>>

    @DELETE("scheduledMessages/{id}")
    fun deleteScheduledMessage(@Path("id") id: String): Call<Void>

}