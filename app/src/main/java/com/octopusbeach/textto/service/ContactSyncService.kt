package com.octopusbeach.textto.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Looper
import android.provider.ContactsContract
import android.support.v4.app.NotificationCompat
import android.util.Base64
import android.util.Log
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.R
import com.octopusbeach.textto.api.ApiService
import com.octopusbeach.textto.home.MainActivity
import com.octopusbeach.textto.model.Contact
import javax.inject.Inject

/**
 * Created by hudson on 12/2/16.
 */

class ContactSyncService : Service() {
    private val TAG = "ContactSyncService"

    @Inject lateinit var apiService: ApiService

    override fun onCreate() {
        super.onCreate()
        (applicationContext as BaseApplication).appComponent.inject(this)
        val runnable = Runnable {
            startForeground(2, createNotification())
            syncContacts()
            Looper.getMainLooper().run {
                Log.d(TAG, "Finished sync")
                stopSelf()
            }
        }
        Thread(runnable).start()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)

        return NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(this.getString(R.string.syncing_contacts))
                .setContentIntent(pendingIntent)
                .build()
    }

    private fun syncContacts() {
        Log.d(TAG, "Start contacts sync")
        val contacts = readContacts()
        val postContent = ArrayList<Contact>(20)
        contacts.forEach {
            if (postContent.size == 80) {
                postContacts(postContent)
                postContent.clear()
            } else {
                postContent.add(it)
            }
        }
        if (postContent.isNotEmpty())
            postContacts(postContent)
    }

    private fun postContacts(contacts: List<Contact>) {
        Log.d(TAG, "Posting ${contacts.size} contacts")
        try {
            apiService.postContacts(contacts).execute()
        } catch (e: Exception) {
            Log.e(TAG, "Error posting contacts $e")
        }
    }

    override fun onBind(intent: Intent?) = null

    private fun readContacts(): List<Contact> {
        val contacts = ArrayList<Contact>()
        val cur = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.Contacts._ID,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                        ContactsContract.Contacts.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER),
                null, null, null)
        if (cur == null || cur.count == 0) return contacts
        try {
            while (cur.moveToNext()) {
                val contactId = cur.getInt(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                val address = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                val thumbnail = getContactThumbnail(contactId)
                val name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                val contact = Contact(contactId, name, address, thumbnail)
                contacts.add(contact)
            }
        } finally {
            cur.close()
            return contacts
        }
    }

    private fun getContactThumbnail(id: Int): String? {

        val contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id.toLong())
        val photoUri = Uri.withAppendedPath(contactUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY)
        val cur = contentResolver.query(photoUri,
                arrayOf(ContactsContract.Contacts.Photo.PHOTO), null, null, null) ?: return null
        try {
            if (cur.moveToFirst()) {
                val data = cur.getBlob(0)
                if (data != null)
                    return Base64.encodeToString(data, Base64.DEFAULT)
            }
        } finally {
            cur.close()
        }
        return null
    }
}
