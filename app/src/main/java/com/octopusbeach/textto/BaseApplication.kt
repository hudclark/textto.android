package com.octopusbeach.textto

import android.app.Application
import com.octopusbeach.textto.api.SessionManager

/**
 * Created by hudson on 9/5/16.
 */
class BaseApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        SessionManager.init(applicationContext)
    }

}