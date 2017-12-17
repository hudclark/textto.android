package com.moduloapps.textto.api

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Created by hudson on 11/22/16.
 */
class AuthInterceptor(val sessionController: SessionController): Interceptor {

    private val TAG = "AuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val org = chain.request()
        val token = sessionController.getAuthToken() ?: ""
        val req = org.newBuilder()
                .header(sessionController.TOKEN_HEADER, token)
                .method(org.method(), org.body())
                .build()
        val origResponse = chain.proceed(req)

        // handle token refresh
        if (origResponse.code() == 401) {
            sessionController.reAuthenticate()
            val newToken = sessionController.getAuthToken() ?: return origResponse
            val newReq = origResponse.request().newBuilder()
                    .header(sessionController.TOKEN_HEADER, newToken)
                    .method(org.method(), org.body())
                    .build()
            origResponse.close() // close old request
            return chain.proceed(newReq)
        } else {
            return origResponse
        }
    }
}
