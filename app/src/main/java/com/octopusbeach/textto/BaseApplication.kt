package com.octopusbeach.textto

import android.app.Application
import com.octopusbeach.textto.di.AppComponent
import com.octopusbeach.textto.di.DaggerAppComponent
import com.octopusbeach.textto.di.PreferencesModule

/**
 * Created by hudson on 9/5/16.
 */
class BaseApplication: Application() {

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
                .preferencesModule(PreferencesModule(applicationContext))
                .build()
    }

}