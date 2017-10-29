package com.moduloapps.textto.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInApi
import com.google.android.gms.common.api.GoogleApiClient
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.service.ContactSyncService
import com.moduloapps.textto.service.SmsObserverService
import com.moduloapps.textto.tasks.MessageSyncTask
import com.moduloapps.textto.utils.ThreadUtils
import com.moduloapps.textto.utils.getNeededPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

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
        val contactsLastSynced = prefs.getLong(ContactSyncService.CONTACTS_LAST_SYNCED, 0)
        view?.let {
            it.setMessagesLastSynced(formatSyncTime(messagesLastSynced))
            it.setContactsLastSynced(formatSyncTime(contactsLastSynced)) // todo
        }
    }

    fun syncContacts() {
        view?.let {
            it.getApplicationContext().startService(Intent(it.getApplicationContext(), ContactSyncService::class.java))
            //ThreadUtils.runSingleThreadTask(TestingClass(it.getApplicationContext(), apiService))
            it.setContactsLastSynced("Synced less than a minute ago")
        }
    }

    fun syncMessages() {
        view?.let {
            ThreadUtils.runSingleThreadTask(MessageSyncTask(apiService, it.getApplicationContext() as BaseApplication, prefs))
            it.setMessagesLastSynced("Synced less than a minute ago")
        }
    }

    // TODO could refresh from server
    fun loadUser() {
        view?.let {
            val photoUrl = sessionController.getProfileImage()
            val displayName = sessionController.getDisplayName()
            val displayEmail = sessionController.getDisplayEmail()

            it.setPhotoUrl(photoUrl)
            it.setDisplayName(displayName)
            it.setDisplayEmail(displayEmail)
        }
    }

    fun checkPermissions() {
        view?.let {
            val needed = getNeededPermissions(it.getApplicationContext())
            if (needed.isNotEmpty()) {
                it.showRequestPermissions()
            }
        }
    }

    fun contactSupport() {
        val context = view?.getApplicationContext() ?: return
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("help@textto.io"))
        context.startActivity(Intent.createChooser(intent, "Email Support"))
    }

    fun logOut (googleApiClient: GoogleApiClient) {
        if (googleApiClient.isConnected) {
            Auth.GoogleSignInApi.signOut(googleApiClient)
        }
        view?.let {
            val refreshToken = sessionController.getRefreshToken()
            apiService.revokeToken(refreshToken)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        Log.d(TAG, "Successfully logged out")
                        onLogOut()
                    }, {
                        Log.e(TAG, "Error logging out", it)
                        onLogOut()
                    })
        }
    }

    private fun onLogOut () {
        sessionController.clearTokens()
        view?.let {
            val context = it.getApplicationContext()
            context.stopService(Intent(context, SmsObserverService::class.java))
            it.redirectToLogin()
        }
    }

    private fun formatSyncTime(time: Long): String {
        if (time == 0L) {
            return "Synced less than a minute ago"
        }

        val delta = System.currentTimeMillis() - time

        val seconds = Math.round(delta.toDouble() / 1000)
        val minutes = Math.round(seconds.toDouble() / 60)
        val hours = Math.round(minutes.toDouble() / 60)
        val days = Math.round(hours.toDouble() / 24)

        if (seconds < 60) {
            return "Synced less than a minute ago"
        } else if (minutes < 60) {
            return "Synced $minutes ${if (minutes == 1L) "minute" else "minutes"} ago"
        } else if (hours < 24) {
            return "Synced $hours ${if (hours == 1L) "hour" else "hours"} ago"
        } else {
            return "Synced $days ${if (days == 1L) "day" else "days"} ago"
        }
    }

    interface View {

        fun setContactsLastSynced(message: String)
        fun setMessagesLastSynced(message: String)
        fun getApplicationContext(): Context
        fun showRequestPermissions()
        fun redirectToLogin()

        fun setDisplayName(name: String)
        fun setDisplayEmail(email: String)
        fun setPhotoUrl(url: String)

    }

}
