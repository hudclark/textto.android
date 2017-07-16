package com.octopusbeach.textto.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by hudson on 11/22/16.
 */
class AuthInterceptor(val sessionController: SessionController): Interceptor {

    private val TOKEN_HEADER = "x-access-token"

    override fun intercept(chain: Interceptor.Chain): Response {
        val org = chain.request()
        val token = sessionController.getAuthToken() ?: ""
        val req = org.newBuilder()
                .header(TOKEN_HEADER, token)
                .method(org.method(), org.body())
                .build()
        val origResponse = chain.proceed(req)

        // handle token refresh
        if (origResponse.code() == 403) {
            sessionController.reAuthenticate()
            val newToken = sessionController.getAuthToken() ?: ""
            val newReq = org.newBuilder()
                    .header(TOKEN_HEADER, newToken)
                    .method(org.method(), org.body())
                    .build()
            val newResponse = chain.proceed(newReq)
            return newResponse
        } else
            return origResponse
    }
}
