/*
 * Copyright (c) 2018. Modulo Apps LLC
 */

package com.moduloapps.textto.encryption

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.view.ViewCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.moduloapps.textto.BaseApplication
import com.moduloapps.textto.R
import com.moduloapps.textto.api.SessionController
import com.moduloapps.textto.utils.ThreadUtils
import javax.inject.Inject

/**
 * Created by hudson on 3/8/18.
 */
class EncryptionSetupFragment: Fragment() {

    companion object {
        const val TAG = "EncryptionSetupFragment"
    }

    private lateinit var titleView: TextView
    private lateinit var descriptionView: TextView
    private lateinit var passwordView: TextView
    private lateinit var errorView: TextView
    private lateinit var loadingView: View
    private lateinit var rightButton: Button
    private lateinit var leftButton: Button

    @Inject lateinit var encryptionHelper: EncryptionHelper
    @Inject lateinit var sessionController: SessionController

    private var currentStage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        (activity?.application as BaseApplication).appComponent.inject(this)

        val rootView = inflater.inflate(R.layout.fragment_encryption_setup, null)

        // init views

        titleView = rootView.findViewById(R.id.title)
        descriptionView = rootView.findViewById(R.id.description)
        passwordView = rootView.findViewById(R.id.password)
        leftButton = rootView.findViewById(R.id.button_left)
        rightButton = rootView.findViewById(R.id.button_right)
        loadingView = rootView.findViewById(R.id.loading)
        errorView = rootView.findViewById(R.id.error)

        // Init left, right buttons
        leftButton.setOnClickListener { leftButtonClick() }
        rightButton.setOnClickListener { rightButtonClick() }


        setStage(if (encryptionHelper.enabled()) 3 else 0)

        return rootView
    }

    private fun setStage(stage: Int) {
        currentStage = stage
        errorView.setText(R.string.empty)

        setTitle(stage)
        setDescription(stage)
        setButtonState(stage)
        setViewState(stage)
    }

    private fun setTitle(stage: Int) {
        val newTitle = when (stage) {
            0 -> R.string.encryption_title_0
            1 -> R.string.encryption_title_1
            2 -> R.string.encryption_title_2
            3 -> R.string.encryption_title_3
            else -> R.string.empty
        }
        titleView.setText(newTitle)
    }

    private fun setDescription(stage: Int) {
        val newDescription = when (stage) {
            0 -> R.string.encryption_description_0
            1 -> R.string.encryption_description_1
            3 -> R.string.encryption_description_3
            else -> R.string.empty
        }
        descriptionView.setText(newDescription)
    }

    private fun setButtonState(stage: Int) {
        when (stage) {
            0 -> {
                leftButton.visibility = View.GONE

                rightButton.setText(R.string.next)
            }

            1 -> {
                leftButton.visibility = View.VISIBLE
                rightButton.setText(R.string.next)

                leftButton.setText(R.string.back)
                rightButton.setText(R.string.next)
            }

            2 -> {
                leftButton.visibility = View.VISIBLE

                leftButton.setText(R.string.back)
                rightButton.setText(R.string.set)
            }

            3 -> {
                leftButton.visibility = View.VISIBLE

                leftButton.setText(R.string.disable)
                rightButton.setText(R.string.change)
            }

        }

        val color = if (stage == 2) R.color.green else R.color.blue

        ViewCompat.setBackgroundTintList(rightButton,
                ContextCompat.getColorStateList(context!!, color) )

    }

    private fun setViewState(stage: Int) {
        when (stage) {
            0, 1, 3 -> {
                descriptionView.visibility = View.VISIBLE
                passwordView.visibility = View.GONE
            }
            2 -> {
                descriptionView.visibility = View.GONE
                passwordView.visibility = View.VISIBLE
            }
        }
    }

    private fun leftButtonClick() {
        when (currentStage) {
            1 -> setStage(0)
            2 -> setStage(1)
            3 -> {
                encryptionHelper.disable()
                setStage(0)
            }
        }
    }

    private fun rightButtonClick() {
        when (currentStage) {
            0 -> setStage(1)
            1 -> setStage(2)
            2 -> setPassword(passwordView.text?.toString())
            3 -> setStage(2)
        }
    }

    private fun setError(error: String) {
        errorView.text = error
    }

    private fun setIsLoading(isLoading: Boolean) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        passwordView.visibility = if (!isLoading) View.VISIBLE else View.GONE

        leftButton.visibility = if (!isLoading) View.VISIBLE else View.GONE
        rightButton.visibility = if (!isLoading) View.VISIBLE else View.GONE
    }

    private fun setPassword(password: String?) {
        val email = sessionController.getDisplayEmail()
        if (TextUtils.isEmpty(email)) {
            setError("You do not have a valid email!")
            return
        }

        val passwordError = validatePassword(password)
        if (passwordError != null) {
            setError(passwordError)
            return
        }

        // Derive password
        setIsLoading(true)
        ThreadUtils.runInBackground( Runnable {
            try {
                encryptionHelper.setPassword(password!!, email)

                // Success
                ThreadUtils.runOnMainThread(Runnable {
                    // Advance to final stage

                    passwordView.text = ""

                    setIsLoading(false)
                    setStage(3)

                    // TODO need to communicate with owner that we have hit success.
                })

            } catch (e: Exception) {
                Log.e(TAG, e.toString())
                Crashlytics.logException(e)
                ThreadUtils.runOnMainThread(Runnable {
                    // Stay in same stage
                    setError("Error setting password. Please try again.")
                    setIsLoading(false)
                })
            }

        })
    }

    private fun validatePassword (password: String?): String? {
        if (TextUtils.isEmpty(password)) return "No Password entered."
        if (password!!.length < 4) return "Password must be at least 4 characters."
        return null
    }

}