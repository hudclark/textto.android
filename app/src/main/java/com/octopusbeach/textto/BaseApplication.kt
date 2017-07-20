package com.octopusbeach.textto

import android.app.Application
import android.util.Log
import com.google.gson.JsonObject
import com.octopusbeach.textto.di.AppComponent
import com.octopusbeach.textto.di.DaggerAppComponent
import com.octopusbeach.textto.di.PreferencesModule
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by hudson on 9/5/16.
 */
class BaseApplication: Application() {

    private val TAG = "BaseApplication"

    lateinit var appComponent: AppComponent

    override fun onCreate() {
        super.onCreate()
        appComponent = DaggerAppComponent.builder()
                .preferencesModule(PreferencesModule(applicationContext))
                .build()
    }

    fun setFirebaseToken(token: String) {
        val data = JsonObject()
        data.addProperty("firebaseId", token)
        appComponent.getApiService().updateFirebaseId(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    Log.d(TAG, "Set firebase id")
                }, {
                    Log.e(TAG, "Error setting firebase id: $it")
                })
        }

}