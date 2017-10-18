package com.moduloapps.textto.di

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

/**
 * Created by hudson on 7/15/17.
 */
@Module
class PreferencesModule(val context: Context) {

    @Singleton
    @Provides
    fun providesSharedPreferences(): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

}