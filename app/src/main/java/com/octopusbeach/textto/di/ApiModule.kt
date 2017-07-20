package com.octopusbeach.textto.di

import android.content.SharedPreferences
import android.util.Log
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import com.octopusbeach.textto.BuildConfig
import com.octopusbeach.textto.api.*
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

    @Provides
    @Singleton
    fun providesHttpLogginIntercepter(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BASIC
        return loggingInterceptor
    }

    @Provides
    @Singleton
    fun providesPublicApiService(loggingInterceptor: HttpLoggingInterceptor): PublicApiService {
        val builder = OkHttpClient.Builder()
        builder.addInterceptor(loggingInterceptor)

        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.connectTimeout(30, TimeUnit.SECONDS)

        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(builder.build())
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        val service = retrofit.create(PublicApiService::class.java)
        return service
    }

    @Provides
    @Singleton
    fun providesSessionController(prefs: SharedPreferences, publicApiService: PublicApiService): SessionController {
        return SessionController(publicApiService, prefs)
    }

    @Provides
    @Singleton
    fun providesOkHttpClient(sessionController: SessionController, loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {

        val builder = OkHttpClient.Builder()
        builder.addInterceptor(AuthInterceptor(sessionController))

        builder.addInterceptor(loggingInterceptor)

        builder.readTimeout(30, TimeUnit.SECONDS)
        builder.connectTimeout(30, TimeUnit.SECONDS)

        return builder.build()
    }

    @Provides
    @Singleton
    fun providesApiService(client: OkHttpClient, sessionController: SessionController): ApiService {
        val retrofit = Retrofit.Builder()
                .baseUrl(BuildConfig.API_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build()
        return retrofit.create(ApiService::class.java)
    }

}
