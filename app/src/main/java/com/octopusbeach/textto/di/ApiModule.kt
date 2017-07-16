package com.octopusbeach.textto.di

import android.content.SharedPreferences
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.AuthInterceptor
import com.octopusbeach.textto.api.SessionController
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by hudson on 7/14/17.
 */

@Module(includes = arrayOf(PreferencesModule::class))
class ApiModule {

    // TODO move to gradle when it might change
    private val BASE_URL = "https://textto.herokuapp.com"

    @Provides
    @Singleton
    fun providesSessionController(prefs: SharedPreferences): SessionController {
        return SessionController(null, prefs)
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(sessionController: SessionController): OkHttpClient {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(AuthInterceptor(sessionController))
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
        builder.addInterceptor(loggingInterceptor)

        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.connectTimeout(30, TimeUnit.SECONDS)

        return builder.build()
    }

    @Provides
    @Singleton
    fun providesRetrofit(client: OkHttpClient): Retrofit {
        Log.d("ApiModule", "Creating retrofit")
        return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
    }

    @Provides
    @Singleton
    fun providesApiService(retrofit: Retrofit, sessionController: SessionController): ApiService {
        val service = retrofit.create(ApiService::class.java)
        sessionController.apiService = service
        return service
    }

}
