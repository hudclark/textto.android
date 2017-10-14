package com.octopusbeach.textto.service

import android.content.SharedPreferences
import android.database.ContentObserver
import android.os.Handler
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.tasks.MessageSyncTask
import com.octopusbeach.textto.utils.ThreadUtils
import io.reactivex.subjects.PublishSubject
import java.util.concurrent.TimeUnit

/**
 * Created by hudson on 9/6/16.
 */
class SmsObserver(val context: BaseApplication,
                  val apiService: ApiService,
                  val sharedPreferences: SharedPreferences):
        ContentObserver(Handler()) {

    private val TAG = "Sms Observer"

    private val publishSubject: PublishSubject<Any?> = PublishSubject.create()

    init {
        publishSubject.debounce(500, TimeUnit.MILLISECONDS).subscribe {
            Log.d(TAG, "Starting...")
            ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, context, sharedPreferences))
        }
    }

    override fun onChange(selfChange: Boolean) {
        super.onChange(selfChange)
        publishSubject.onNext(0)
    }
}