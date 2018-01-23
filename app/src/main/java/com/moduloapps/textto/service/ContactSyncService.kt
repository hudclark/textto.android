package com.moduloapps.textto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.os.Looper
import android.provider.ContactsContract
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.ApiService
import com.moduloapps.textto.api.MAX_CONTACTS_PER_REQUEST
import com.moduloapps.textto.home.MainActivity
import com.moduloapps.textto.model.Contact
import com.moduloapps.textto.utils.ImageUtils
import com.moduloapps.textto.utils.SYNC_CHANNEL_ID
import com.moduloapps.textto.utils.tryForEach
import javax.inject.Inject

/**
 * Created by hudson on 12/2/16.
 */

class ContactSyncService : Service() {
    private val TAG = "ContactSyncService"

    @Inject lateinit var apiService: ApiService
    @Inject lateinit var prefs: SharedPreferences

    companion object {
        val CONTACTS_LAST_SYNCED = "contacts_last_synced"
    }

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)

        val runnable = Runnable {
            startForeground(2, createNotification())
            try {
                syncContacts()
            } catch (e: Exception) {
                Crashlytics.logException(e)
            }
            Looper.getMainLooper().run {
                Log.d(TAG, "Finished sync")
                stopSelf()
            }
        }
        Thread(runnable).start()
        prefs.edit().putLong(CONTACTS_LAST_SYNCED, System.currentTimeMillis()).apply()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this, SYNC_CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(this.getString(R.string.syncing_contacts))
                .setContentIntent(pendingIntent)
                .setColor(ContextCompat.getColor(applicationContext, R.color.blue))
                .build()
    }

    private fun syncContacts() {
        Log.d(TAG, "Start contacts sync")
        readContacts()
                .chunked(MAX_CONTACTS_PER_REQUEST)
                .forEach { postContacts(it) }
    }

    private fun postContacts(contacts: List<Contact>) {
        Log.d(TAG, "Posting ${contacts.size} contacts")
        try {
            apiService.postContacts(contacts).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error posting contacts $e")
            Crashlytics.logException(e)
        }
    }

    override fun onBind(intent: Intent?) = null

    private fun readContacts(): List<Contact> {
        val cur = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null)
        if (cur == null || cur.count == 0) return emptyList()

        val contactMap = HashMap<Int, Contact>()

        cur.tryForEach {
            val contactId = it.getInt(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
            if (!contactMap.containsKey(contactId)) {
                val address = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val thumbnail = getContactThumbnail(contactId)
                val name = it.getString(it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val contact = Contact(contactId, name, address, thumbnail)
                contactMap[contactId] = contact
            }
        }

        cur.close()
        return contactMap.values.toList()
    }

    private fun getContactThumbnail(id: Int): String? {
        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLong())

        val inStream = ContactsContract.Contacts.openContactPhotoInputStream(applicationContext.contentResolver, contactUri)

        var data: String? = null

        // Compress thumbnail
        inStream?.let {
            try {
                val compressed = ImageUtils.compressImage(inStream, Bitmap.CompressFormat.JPEG, 20)
                data = ImageUtils.byteArrayToBase64(compressed)
            } catch (e: Exception) {
                Crashlytics.logException(e)
            }
        }

        return data
    }
}
