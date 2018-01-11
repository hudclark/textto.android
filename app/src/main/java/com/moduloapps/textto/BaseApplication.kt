package com.moduloapps.textto

import android.app.Application
import android.os.Build
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.google.gson.JsonObject
import com.moduloapps.textto.di.AppComponent
import com.moduloapps.textto.di.DaggerAppComponent
import com.moduloapps.textto.di.PreferencesModule
import com.moduloapps.textto.utils.createSyncChannel
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric
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
        if (LeakCanary.isInAnalyzerProcess(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createSyncChannel(this)
        LeakCanary.install(this)
        Fabric.with(this, Crashlytics())
    }

    // TODO move this to something else
    fun setFirebaseToken(token: String) {
        if (appComponent.getSessionController().isLoggedIn()) {
            val data = JsonObject()
            data.addProperty("firebaseId", token)
            appComponent.getApiService().updateFirebaseId(data)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.d(TAG, "Set firebase id")
                    }, {
                        Log.e(TAG, "Error setting firebase id: $it")
                        Crashlytics.log(1, TAG, it.toString())
                    })
            }
        }

}