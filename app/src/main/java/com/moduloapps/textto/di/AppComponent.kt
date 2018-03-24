package com.moduloapps.textto.di

import android.content.SharedPreferences
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.PublicApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.encryption.EncryptionHelper
import com.moduloapps.textto.encryption.EncryptionSetupFragment
import com.moduloapps.textto.home.MainActivity
import com.moduloapps.textto.login.LoginActivity
import com.moduloapps.textto.notifications.NotificationListener
import com.moduloapps.textto.onboarding.OnboardingActivity
import com.moduloapps.textto.service.ContactSyncService
import com.moduloapps.textto.service.MessagingService
import com.moduloapps.textto.settings.SettingsActivity
import dagger.Component
import javax.inject.Singleton

/**
 * Created by hudson on 7/15/17.
 */
@Singleton
@Component(modules = arrayOf(ApiModule::class, EncryptionModule::class))
interface AppComponent {

    fun inject(activity: MainActivity)
    fun inject(activity: OnboardingActivity)
    fun inject(activity: LoginActivity)
    fun inject(activity: SettingsActivity)

    fun inject(fragment: EncryptionSetupFragment)

    fun inject(service: MessagingService)
    fun inject(service: ContactSyncService)
    fun inject(service: NotificationListener)

    fun getSessionController(): SessionController
    fun getPublicApiService(): PublicApiService
    fun getApiService(): ApiService
    fun getSharedPrefs(): SharedPreferences

    fun getEncryptionHelper(): EncryptionHelper
}