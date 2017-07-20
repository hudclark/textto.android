package com.octopusbeach.textto.api

import android.net.Proxy
import android.util.Log
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

/**
 * Created by hudson on 7/19/17.
 */
class TokenAuthenticator(val sessionController: SessionController): Authenticator {

    val TAG = "TokenAuthenticator"

    override fun authenticate(route: Route, response: Response): Request? {

        Log.d(TAG, "Authenticating for response " + response.code())
        // have we already tried with this token?
        if (response.priorResponse()?.priorResponse() != null) {
            return null // give up. Token might be revoked.
        }

        sessionController.reAuthenticate()
        val newToken = sessionController.getRefreshToken() ?: return null
        return response.request().newBuilder()
                .header(sessionController.TOKEN_HEADER, newToken)
                .build()
    }

}