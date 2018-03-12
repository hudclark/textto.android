/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.di

import android.content.SharedPreferences
import com.moduloapps.textto.encryption.EncryptionHelper
import com.moduloapps.textto.persistance.SharedPreferencesPersistence
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by hudson on 3/8/18.
 */
@Module(includes = arrayOf(PreferencesModule::class))
class EncryptionModule {

    @Provides
    @Singleton
    fun providesEncryptionHelper(prefs: SharedPreferences): EncryptionHelper {
        val persistence = SharedPreferencesPersistence(prefs)
        return EncryptionHelper(persistence)
    }

}