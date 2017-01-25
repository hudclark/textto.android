package com.octopusbeach.textto.service

import android.app.Service
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.util.Base64
import android.util.Log
import com.octopusbeach.textto.api.ApiClient
import com.octopusbeach.textto.api.ContactEndpointInterface
import com.octopusbeach.textto.model.Contact
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Created by hudson on 12/2/16.
 */

class ContactSyncService : Service() {
    private val TAG = "ContactSyncService"

    override fun onCreate() {
        super.onCreate()
        readContacts()
        Log.e("TEST", "Starting")
    }

    override fun onBind(intent: Intent?) = null

    private fun readContacts() {
        Log.d(TAG, "Syncing contacts...")
        Runnable {
            val api = ApiClient.getInstance().create(ContactEndpointInterface::class.java)
            val cur = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    arrayOf(ContactsContract.Contacts._ID,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                            ContactsContract.Contacts.DISPLAY_NAME,
                            ContactsContract.CommonDataKinds.Phone.NUMBER),
                    null, null, null)
            if (cur == null || cur.count == 0)
                return@Runnable
            try {
                while (cur.moveToNext()) {
                    val contactId = cur.getInt(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID))
                    val address = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val thumbnail = getContactThumbnail(contactId)
                    val name = cur.getString(cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val contact = Contact(contactId, name, address, thumbnail)
                    api.postContact(contact).enqueue(object : Callback<Map<String, Contact>> {
                        override fun onFailure(call: Call<Map<String, Contact>>?, t: Throwable?) {
                            Log.e(TAG, t.toString())
                        }

                        override fun onResponse(call: Call<Map<String, Contact>>, response: Response<Map<String, Contact>>) {
                            Log.d(TAG, "Posted Contact")
                        }
                    })
                }
            } finally {
                cur.close()
            }
        }.run()
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
