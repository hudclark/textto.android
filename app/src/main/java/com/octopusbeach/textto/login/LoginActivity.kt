package com.octopusbeach.textto.login

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.octopusbeach.textto.BaseApplication
import com.octopusbeach.textto.R
import com.octopusbeach.textto.api.PublicApiService
import com.octopusbeach.textto.api.SessionController
import com.octopusbeach.textto.home.MainActivity
import com.squareup.haha.perflib.Main
import javax.inject.Inject

class LoginActivity : AppCompatActivity(), View.OnClickListener, LoginPresenter.View {

    private val TAG = "LoginActivity"

    @Inject lateinit var sessionController: SessionController
    @Inject lateinit var apiService: PublicApiService

    private var googleClient: GoogleApiClient? = null

    private var signInButton: SignInButton? = null
    private var loginLoader: View? = null
    private var rootView: View? = null

    private var presenter: LoginPresenter? = null

    companion object {
        val DID_LOG_IN = "did_log_in"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as BaseApplication).appComponent.inject(this)

        if (sessionController.isLoggedIn()) {
            redirectToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        rootView = findViewById(R.id.login_content)

        signInButton = findViewById(R.id.sign_in_button) as SignInButton
        signInButton?.let {
            it.setOnClickListener(this)
        }
        loginLoader = findViewById(R.id.login_loader)

        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.client_id))
                .requestEmail()
                .build()
        googleClient = GoogleApiClient.Builder(this).addApi(Auth.GOOGLE_SIGN_IN_API, options)
                .build()

        if (presenter == null)
            presenter = LoginPresenter(apiService, sessionController)
        presenter!!.onTakeView(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.onTakeView(null)
        if (!isChangingConfigurations)
            presenter = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
        presenter?.handleSignInResult(result)
    }

    override fun onClick(view: View?) {
        Log.d(TAG, "Attempting sign in")
        val intent = Auth.GoogleSignInApi.getSignInIntent(googleClient)
        startActivityForResult(intent, 3)
        setLoadingViewVisibility(true)
    }

    override fun onLoginFailure(error: String) {
        Log.e(TAG, "Log in failure: $error")
        setLoadingViewVisibility(false)
        Snackbar.make(rootView!!, error, Snackbar.LENGTH_SHORT).show()
    }

    override fun onLoginSuccess() {
        Log.d(TAG, "Successfully signed in")
        redirectToMainActivity()
    }

    override fun getBaseApplication() = applicationContext as BaseApplication

    private fun setLoadingViewVisibility(visible: Boolean) {
        loginLoader?.visibility = if (visible) View.VISIBLE else View.GONE
        signInButton?.visibility = if (visible) View.GONE else View.VISIBLE
    }

    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra(DID_LOG_IN, true)
        startActivity(intent)
        finish()
    }
}
