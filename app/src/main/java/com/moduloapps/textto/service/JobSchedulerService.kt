/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.service

import com.birbit.android.jobqueue.JobManager
import com.birbit.android.jobqueue.scheduling.FrameworkJobSchedulerService
import com.moduloapps.textto.BaseApplication

/**
 * Created by hudson on 2/18/18.
 */
open class JobSchedulerService: FrameworkJobSchedulerService() {

    override fun getJobManager(): JobManager {
        return (application as BaseApplication).jobManager
    }
}