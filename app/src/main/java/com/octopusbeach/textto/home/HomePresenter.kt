package com.octopusbeach.textto.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.model.Contact
import com.octopusbeach.textto.service.ContactSyncService
import com.octopusbeach.textto.tasks.MessageSyncTask
import com.octopusbeach.textto.tasks.TestingClass
import com.octopusbeach.textto.utils.ThreadUtils

/**
 * Created by hudson on 7/16/17.
 */
class HomePresenter(val apiService: ApiService,
                    val sessionController: SessionController,
                    val prefs: SharedPreferences) {

    private val TAG = "HomePresenter"

    private var view: View? = null

    fun onTakeView(view: View?) {
        this.view = view
    }

    fun loadSyncTimes() {
        val messagesLastSynced = prefs.getLong(MessageSyncTask.MESSAGES_LAST_SYNCED, 0)
        view?.let {
            it.setMessagesLastSynced(formatSyncTime(messagesLastSynced))
            it.setContactsLastSynced(formatSyncTime(0)) // todo
        }
    }

    // TODO this is a longer running task. Will have to make sure it is not already running
    fun syncContacts() {

        view?.let {


            val intent = Intent("com.octopusbeach.MMS_SENT")
            val pendingIntent = PendingIntent.getBroadcast(it.getApplicationContext(), 0, intent, 0)
            TestingClass(it.getApplicationContext(), pendingIntent).run()

        }

        /*
        view?.let {
            it.getApplicationContext().startService(Intent(it.getApplicationContext(), ContactSyncService::class.java))
        }
        */
    }

    fun syncMessages() {
        view?.let {
            ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, it.getApplicationContext(), prefs))
            it.setMessagesLastSynced("Last synced 0 seconds ago")
        }
    }

    // TODO could refresh from server
    fun loadUser() {
        view?.let {
            val photoUrl = sessionController.getProfileImage()
            val displayName = sessionController.getDisplayName()

            it.setPhotoUrl(photoUrl)
            it.setDisplayName(displayName)
        }
    }

    private fun formatSyncTime(time: Long): String {
        if (time == 0L) {
            return "Syncing now"
        }

        val delta = System.currentTimeMillis() - time

        val seconds = Math.round(delta.toDouble() / 1000)
        val minutes = Math.round(seconds.toDouble() / 60)
        val hours = Math.round(minutes.toDouble() / 60)
        val days = Math.round(hours.toDouble() / 24)

        if (seconds < 60) {
            return "Last synced $seconds ${if (seconds == 1L) "second" else "seconds"} ago"
        } else if (minutes < 60) {
            return "Last synced $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        } else if (hours < 24) {
            return "Last synced $hours ${if (hours == 1L) "hour" else "hours"} ago"
        } else {
            return "Last synced $days ${if (days == 1L) "day" else "days"} ago"
        }
    }

    interface View {

        fun setContactsLastSynced(message: String)
        fun setMessagesLastSynced(message: String)
        fun getApplicationContext(): Context

        fun setDisplayName(name: String)
        fun setPhotoUrl(url: String)

    }

}
