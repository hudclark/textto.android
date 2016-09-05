package com.octopusbeach.textto.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Created by hudson on 9/5/16.
 */

object ApiClient {
    val TAG = "Api Client"

    private val BASE_URL = "http://35.2.229.24:8000"
    private var client: Retrofit? = null

    private fun init() {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(object: Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val org = chain.request()
                val req = org.newBuilder()
                        .header("Authorization", "Bearer ${SessionManager.getToken()}")
                        .method(org.method(), org.body())
                        .build()
                return chain.proceed(req)
            }
        })

        client = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build()
    }

    fun getInstance(): Retrofit {
        if (client == null) {
            init()
        }
        return client!!
    }
}
