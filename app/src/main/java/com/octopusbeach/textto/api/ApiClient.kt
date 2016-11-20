
package com.octopusbeach.textto.api

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Created by hudson on 9/5/16.
 */

object ApiClient {
    val TAG = "Api Client"
    private val TOKEN_HEADER = "x-access-token"
    private val BASE_URL = "https://textto.herokuapp.com"

    private var client: Retrofit? = null

    private fun init() {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(object: Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                val org = chain.request()
                val token = SessionManager.getToken() ?: ""
                val req = org.newBuilder()
                        .header(TOKEN_HEADER, token)
                        .method(org.method(), org.body())
                        .build()
                val origResponse = chain.proceed(req)

                // handle token refresh
                if (origResponse.code() == 403) {
                    SessionManager.reAuth()
                    val newToken = SessionManager.getToken() ?: ""
                    val newReq = org.newBuilder()
                            .header(TOKEN_HEADER, newToken)
                            .method(org.method(), org.body())
                            .build()
                    val newResponse = chain.proceed(newReq)
                    return newResponse
                } else
                    return origResponse
            }
        })

        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        builder.addInterceptor(interceptor)

        // might have to spin heroku up
        builder.readTimeout(5, TimeUnit.MINUTES)
        builder.connectTimeout(5, TimeUnit.MINUTES)
        client = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .client(builder.build())
                .build()
    }

    fun getInstance(): Retrofit {
        if (client == null) init()
        return client!!
    }
}
