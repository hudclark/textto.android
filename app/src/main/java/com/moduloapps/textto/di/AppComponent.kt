package com.moduloapps.textto.di

import android.content.SharedPreferences
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.PublicApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.home.MainActivity
import com.moduloapps.textto.login.LoginActivity
import com.moduloapps.textto.onboarding.OnboardingActivity
import com.moduloapps.textto.service.ContactSyncService
import com.moduloapps.textto.service.MessagingService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by hudson on 7/15/17.
 */
@Singleton
@Component(modules = arrayOf(ApiModule::class))
interface AppComponent {

    fun inject(activity: MainActivity)
    fun inject(activity: OnboardingActivity)
    fun inject(activity: LoginActivity)

    fun inject(service: MessagingService)
    fun inject(service: ContactSyncService)

    fun getSessionController(): SessionController
    fun getPublicApiService(): PublicApiService
    fun getApiService(): ApiService
    fun getSharedPrefs(): SharedPreferences
}