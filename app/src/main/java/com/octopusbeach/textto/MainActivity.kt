package com.octopusbeach.textto;

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.octopusbeach.textto.api.SessionManager
import com.octopusbeach.textto.fragments.LoginFragment
import com.octopusbeach.textto.service.SmsListenerService
import com.octopusbeach.textto.utils.MessageUtils
import java.util.*

class MainActivity: AppCompatActivity(), LoginFragment.OnAuthListener {

    private val TAG = "MainActivity"
    private val RC_SIGN_IN = 1

    private val permissions = arrayOf(
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.SEND_SMS,
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.READ_CONTACTS
    )
    private val PERMISSIONS_CODE = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // check logged in status
        if (SessionManager.getToken() == null) {
            val frag = LoginFragment()
            frag.setAuthListener(this)
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_content, frag)
                .commit()
        }
        checkPermissions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val fragment = supportFragmentManager.findFragmentById(R.id.fragment_content)
            fragment.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onAuth() {
        startService(Intent(this, SmsListenerService::class.java))
        MessageUtils.updateMessages(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_CODE) {
            // check all permissions
            val neededPermissions = ArrayList<String>()
            permissions.forEach {
                if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                    neededPermissions.add(it)
            }
            // TODO handle permissions we still need
            } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun needsPermissions(): Boolean {
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                return true
        }
        return false
    }

    private fun checkPermissions() {
        val neededPermissions = ArrayList<String>()
        permissions.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED)
                neededPermissions.add(it)
        }
        if (!neededPermissions.isEmpty())
            ActivityCompat.requestPermissions(this, neededPermissions.toTypedArray(), PERMISSIONS_CODE)
    }
}
