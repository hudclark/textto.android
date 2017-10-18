package com.moduloapps.textto.onboarding

import android.support.v4.view.ViewPager
import android.view.View
import com.moduloapps.textto.R

/**
 * Created by hudson on 10/5/17.
 */
class OnboardingPageTransformer : ViewPager.PageTransformer {

    override fun transformPage(page: View, position: Float) {
        val pagePosition = page.tag as Int
        val pageWidth = page.width
        if (position > -1f && position < 1f) {
            val delta = pageWidth * position

            if (pagePosition < 2) {
                val imageView = page.findViewById<View>(R.id.onboarding_image)
                imageView.translationX = (delta * .8).toFloat()

                val titleView = page.findViewById<View>(R.id.onboarding_title)
                titleView.translationX = (delta * .5).toFloat()

                val messageView = page.findViewById<View>(R.id.onboarding_message)
                messageView.translationX = (delta * .2).toFloat()
            } else {
                val titleView = page.findViewById<View>(R.id.permission_title)
                titleView.translationX = (delta * .8).toFloat()

                val itemsView = page.findViewById<View>(R.id.permission_items)
                itemsView.translationX = (delta * .5).toFloat()
            }
        }
    }
}