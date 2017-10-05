package com.octopusbeach.textto.onboarding

import android.support.v4.view.ViewPager
import android.util.Log
import android.view.View
import com.octopusbeach.textto.R

/**
 * Created by hudson on 10/5/17.
 */
class OnboardingPageTransformer : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pagePosition = page.tag as Int

        val pageWidth = page.width

        Log.d("TEST", "Pos: $position")
        if (position > -1f && position < 1f) {
            val delta = pageWidth * position
            Log.d("TEST", "Delta: $delta, Pos: $pagePosition")

            if (pagePosition < 2) {
                val imageView = page.findViewById<View>(R.id.onboarding_image)
                imageView.translationX = (delta * .8).toFloat()

                val titleView = page.findViewById<View>(R.id.onboarding_title)
                titleView.translationX = (delta * .5).toFloat()

                val messageView = page.findViewById<View>(R.id.onboarding_message)
                messageView.translationX = (delta * .2).toFloat()
            } else {

            }
        }
    }
}