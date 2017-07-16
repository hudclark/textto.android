package com.octopusbeach.textto.di

import com.octopusbeach.textto.service.MessagingService
import com.octopusbeach.textto.service.NotificationListener
import dagger.Component
import javax.inject.Singleton

/**
 * Created by hudson on 7/14/17.
 */
@Singleton
@Component(modules = arrayOf(ApiModule::class))
interface ApiComponent {

    fun inject(service: MessagingService)
    fun inject(listener: NotificationListener)

}
