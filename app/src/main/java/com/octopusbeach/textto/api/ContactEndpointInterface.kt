package com.octopusbeach.textto.api

import com.octopusbeach.textto.model.Contact
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Created by hudson on 12/10/16.
 */
interface ContactEndpointInterface {

    @POST("contacts")
    fun postContact(@Body contact: Contact): Call<Map<String, Contact>>
}
