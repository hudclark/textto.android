package com.moduloapps.textto.login

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.LoginEvent
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.GoogleApiClient
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.PublicApiService
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.home.MainActivity
import io.fabric.sdk.android.Fabric
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
        Fabric.with(this, Crashlytics())

        if (sessionController.isLoggedIn()) {
            redirectToMainActivity()
            return
        }

        setContentView(R.layout.activity_login)

        rootView = findViewById(R.id.login_content)

        signInButton = findViewById(R.id.sign_in_button) as SignInButton
        signInButton?.setOnClickListener(this)

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

        Answers.getInstance().logLogin(LoginEvent().putSuccess(false))
        Crashlytics.log(1, TAG, error)
    }

    override fun onLoginSuccess() {
        Log.d(TAG, "Successfully signed in")
        Answers.getInstance().logLogin(LoginEvent().putSuccess(true))
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
