package com.octopusbeach.textto.di

import com.octopusbeach.textto.service.MessagingService
import dagger.Component
import javax.inject.Singleton

/**
 * Created by hudson on 7/14/17.
 */
@Singleton
@Component(modules = arrayOf(ApiModule::class))
interface ApiComponent {

    fun inject(service: MessagingService)

}
