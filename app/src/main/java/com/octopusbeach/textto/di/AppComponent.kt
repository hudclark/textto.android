package com.octopusbeach.textto.di

import com.octopusbeach.textto.MainActivity
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.login.LoginActivity
import com.octopusbeach.textto.onboarding.OnboardingActivity
import com.octopusbeach.textto.service.MessagingService
import com.octopusbeach.textto.service.NotificationListener
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

    fun inject(listener: NotificationListener)

    fun inject(service: MessagingService)

    fun getSessionController(): SessionController
}