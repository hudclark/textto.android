package com.octopusbeach.textto.login

import android.app.Fragment
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.GoogleApiClient
import com.octopusbeach.textto.R
import com.octopusbeach.textto.login.LoginPresenter

/**
 * Created by hudson on 10/27/16.
 */

class LoginFragment: Fragment() {

    private lateinit var googleClient: GoogleApiClient
    private var listener: OnAuthListener? = null
    private var presenter: LoginPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val v = inflater.inflate(R.layout.fragment_login, container, false)
        //v.findViewById(R.id.login_btn).setOnClickListener { login() }
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .build()
        googleClient = GoogleApiClient.Builder(activity).addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()

        /*
        if (presenter == null)
            presenter = LoginPresenter()
        presenter?.onTakeView(this)
        */
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        toggleLoadingView(true)
        presenter?.handleSignInResult(result)
    }

    override fun onDestroy() {
        super.onDestroy()
        // clean up presenter
        presenter?.onTakeView(null)
        if (!activity.isChangingConfigurations)
            presenter = null
    }

    private fun login() {
        val intent = Auth.GoogleSignInApi.getSignInIntent(googleClient)
        startActivityForResult(intent, 1)
    }

    fun onLoginSuccess() {
        toggleLoadingView(false)
        listener?.onAuth()
    }

    fun onLoginFailure() {
        toggleLoadingView(false)
        // TODO display error
    }

    fun toggleLoadingView(isLoading: Boolean) {
        //view.findViewById(R.id.login_btn).visibility = if (isLoading) View.GONE else View.VISIBLE
        //view.findViewById(R.id.spinner).visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    fun setAuthListener(listener: OnAuthListener) {
        this.listener = listener
    }

    /**
     * Provide an authentication callback
     */
    interface OnAuthListener {
        fun onAuth()
    }
}
