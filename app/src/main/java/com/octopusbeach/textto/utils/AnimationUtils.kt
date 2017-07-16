package com.octopusbeach.textto.utils

import android.animation.Animator
import android.os.Build
import android.view.View
import android.view.ViewAnimationUtils
import android.view.animation.AccelerateInterpolator

/**
 * Created by hudson on 7/15/17.
 */
fun circleRevealEnter(view: View) {
    val cx = view.measuredWidth / 2
    val cy = view.measuredHeight / 2
    val radius = (Math.max(view.width, view.height) / 2).toFloat()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, 0f, radius)
        animator.addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
                view.visibility = View.VISIBLE
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) { }
            override fun onAnimationEnd(p0: Animator?) {
                view.visibility = View.VISIBLE
            }
        })
        animator.duration = 100
        animator.interpolator = AccelerateInterpolator()
        animator.start()
    } else {
        // TODO spice up?
        view.visibility = View.VISIBLE
    }
}
fun circleRevealExit(view: View) {
    val cx = view.measuredWidth / 2
    val cy = view.measuredHeight / 2
    val radius = (view.width / 2).toFloat()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        val animator = ViewAnimationUtils.createCircularReveal(view, cx, cy, radius, 0f)
        animator.addListener(object: Animator.AnimatorListener {
            override fun onAnimationStart(p0: Animator?) {
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) { }
            override fun onAnimationEnd(p0: Animator?) {
                view.visibility = View.GONE
            }
        })
        animator.duration = 100
        animator.interpolator = AccelerateInterpolator()
        animator.start()
    } else {
        // TODO spice up?
        view.visibility = View.GONE
    }
}
