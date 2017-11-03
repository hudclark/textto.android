package com.moduloapps.textto

import android.content.Intent
import android.support.annotation.LayoutRes
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView

/**
 * Created by hudson on 11/3/17.
 */
open class BaseActivity : AppCompatActivity() {


    protected fun View.setVisible(visible: Boolean) {
        visibility = if (visible) View.VISIBLE else View.GONE
    }

    protected fun Snackbar.setTextColor(@LayoutRes id: Int) {
        val color = ContextCompat.getColor(context, id)
        setActionTextColor(color)
        view.findViewById<TextView>(android.support.design.R.id.snackbar_text).setTextColor(color)
    }

    protected fun redirectToActivity(activity: Class<*>) {
        val intent = Intent(this, activity)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}