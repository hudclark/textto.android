package com.octopusbeach.textto.di

import android.content.SharedPreferences
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.PublicApiService
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.home.MainActivity
import com.octopusbeach.textto.login.LoginActivity
import com.octopusbeach.textto.onboarding.OnboardingActivity
import com.octopusbeach.textto.service.ContactSyncService
import com.octopusbeach.textto.service.MessagingService
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