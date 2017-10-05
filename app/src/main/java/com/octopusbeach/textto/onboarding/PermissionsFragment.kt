package com.octopusbeach.textto.onboarding

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.octopusbeach.textto.R

/**
 * Created by hudson on 7/15/17.
 */
class PermissionsFragment: Fragment() {

    var requestPermissionsListener: RequestPermissionsListener? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_permissions, container, false)
        rootView.findViewById<View>(R.id.permission_button).setOnClickListener {
            requestPermissionsListener?.onRequestPermissions()
        }
        rootView.tag = 2
        return rootView
    }

    companion object {

        fun newInstance(requestPermissionsListener: RequestPermissionsListener): PermissionsFragment {
            val frag = PermissionsFragment()
            frag.requestPermissionsListener = requestPermissionsListener
            return frag
        }

    }

    interface RequestPermissionsListener {
        fun onRequestPermissions()
    }
}