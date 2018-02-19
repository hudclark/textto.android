package com.moduloapps.textto

import android.app.Application
import android.os.Build
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.config.Configuration
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.core.CrashlyticsCore
import com.moduloapps.textto.di.AppComponent
import com.moduloapps.textto.di.DaggerAppComponent
import com.moduloapps.textto.di.PreferencesModule
import com.moduloapps.textto.service.JobSchedulerService
import com.moduloapps.textto.utils.createSyncChannel
import com.squareup.leakcanary.LeakCanary
import io.fabric.sdk.android.Fabric

/**
 * Created by hudson on 9/5/16.
 */
class BaseApplication: Application() {

    private val TAG = "BaseApplication"

    lateinit var appComponent: AppComponent
    lateinit var jobManager: JobManager

    override fun onCreate() {
        super.onCreate()
        initJobManager()
        appComponent = DaggerAppComponent.builder()
                .preferencesModule(PreferencesModule(applicationContext))
                .build()

        if (LeakCanary.isInAnalyzerProcess(this)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createSyncChannel(this)
        LeakCanary.install(this)

        val kit = Crashlytics.Builder()
                .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
                .build()
        Fabric.with(this, kit)
    }

    private fun initJobManager () {
        val builder = Configuration.Builder(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            @Suppress("INACCESSIBLE_TYPE")
            builder.scheduler(FrameworkJobSchedulerService.createSchedulerFor(this, JobSchedulerService::class.java))
        }
        jobManager = JobManager(builder.build())
    }

    fun addBackgroundJob(job: Job) {
        jobManager.addJobInBackground(job)
    }

}