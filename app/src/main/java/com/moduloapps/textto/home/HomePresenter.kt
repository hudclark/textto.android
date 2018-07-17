package com.moduloapps.textto.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.common.api.GoogleApiClient
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.encryption.EncryptionHelper
import com.moduloapps.textto.service.ContactSyncService
import com.moduloapps.textto.service.SmsObserverService
import com.moduloapps.textto.tasks.MessageSyncTask
import com.moduloapps.textto.utils.ThreadUtils
import com.moduloapps.textto.utils.getNeededPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Response

/**
 * Created by hudson on 7/16/17.
 */
class HomePresenter(val apiService: ApiService,
                    val sessionController: SessionController,
                    val prefs: SharedPreferences,
                    val encryptionHelper: EncryptionHelper) {

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
            it.setContactsLastSynced(formatSyncTime(contactsLastSynced))
        }
    }

    fun syncContacts() {
        view?.let {
            it.setSyncingContacts(true)

            apiService.syncContacts().enqueue(object: retrofit2.Callback<String> {
                override fun onFailure(call: Call<String>?, t: Throwable?) {
                    Log.d(TAG, "Failed to sync contacts.")
                }

                override fun onResponse(call: Call<String>?, response: Response<String>?) {
                    Log.d(TAG, "Send contact sync request.")
                }
            })

            ThreadUtils.runOnMainThread(Runnable { view?.setSyncingContacts(false)}, 20000) // completely arbitrary
            it.setContactsLastSynced("Synced less than a minute ago")
        }
    }

    fun syncMessages() {


        view?.let {
            it.setSyncingMessages(true)
            val syncRunnable = Runnable {
                val start = System.currentTimeMillis()
                MessageSyncTask(apiService, it.getApplicationContext() as BaseApplication, prefs).run()
                // Make sure 'syncing' takes at least 2s
                val delta = System.currentTimeMillis() - start
                if (delta < 2000) {
                    ThreadUtils.runOnMainThread(Runnable { view?.setSyncingMessages(false)}, 2000 - delta)
                } else {
                    ThreadUtils.runOnMainThread(Runnable { view?.setSyncingMessages(false)})
                }
            }
            ThreadUtils.runSingleThreadTask(syncRunnable)
            it.setMessagesLastSynced("Synced less than a minute ago")
        }
    }

    // TODO could refresh from server
    fun loadUser() {
        view?.let {
            val displayEmail = sessionController.getDisplayEmail()
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

    fun contactSupport(context: Context) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:")
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("help@moduloapps.com"))
        context.startActivity(Intent.createChooser(intent, "Email Support"))
    }

    fun logOut (googleApiClient: GoogleApiClient) {
        if (googleApiClient.isConnected) {
            Auth.GoogleSignInApi.signOut(googleApiClient)
        }

        if (encryptionHelper.enabled()) encryptionHelper.disable()


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

        fun setDisplayEmail(email: String)

        fun setSyncingMessages(syncing: Boolean)
        fun setSyncingContacts(syncing: Boolean)

    }

}
