package com.moduloapps.textto.onboarding

import android.animation.ArgbEvaluator
import android.content.Intent
import android.content.SharedPreferences
import android.support.v7.app.AppCompatActivity

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.answers.CustomEvent
import com.moduloapps.textto.BaseApplication

import com.moduloapps.textto.R
import com.moduloapps.textto.login.LoginActivity
import com.moduloapps.textto.utils.*
import io.fabric.sdk.android.Fabric
import javax.inject.Inject

class OnboardingActivity :
        AppCompatActivity(), ViewPager.OnPageChangeListener, PermissionsFragment.RequestPermissionsListener {

    companion object {
        val ONBOARDING_COMPLETED = "has_onboarded"
    }

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    private lateinit var viewPager: ViewPager
    private lateinit var indicatorOne: ImageView
    private lateinit var indicatorTwo: ImageView
    private lateinit var indicatorThree: ImageView
    private lateinit var rootView: View
    private lateinit var argbEvaluator: ArgbEvaluator
    private lateinit var colors: Array<Int>

    @Inject lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (applicationContext as BaseApplication).appComponent.inject(this)
        Fabric.with(this, Crashlytics())

        setContentView(R.layout.activity_onboarding)

        rootView = findViewById(R.id.main_content)

        indicatorOne = findViewById(R.id.view_pager_indicator_one) as ImageView
        indicatorTwo = findViewById(R.id.view_pager_indicator_two) as ImageView
        indicatorThree = findViewById(R.id.view_pager_indicator_three) as ImageView

        argbEvaluator = ArgbEvaluator()
        colors = arrayOf(ContextCompat.getColor(this, R.color.blueBackground),
                        ContextCompat.getColor(this, R.color.greenBackground),
                        ContextCompat.getColor(this, R.color.indigoBackground))

        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        viewPager = findViewById(R.id.container) as ViewPager
        viewPager.adapter = sectionsPagerAdapter
        viewPager.addOnPageChangeListener(this)
        viewPager.setPageTransformer(false, OnboardingPageTransformer())

        Answers.getInstance().logCustom(CustomEvent("Start Onboarding"))

        // set initial page to 0
        onPageSelected(0)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == PERMISSIONS_CODE) {
            val neededPermissions = getNeededPermissions(this)
            if (!neededPermissions.isEmpty())
                Snackbar.make(rootView, R.string.need_permissions, Snackbar.LENGTH_SHORT).show()
            else
                finishOnboarding()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onRequestPermissions() {
        val neededPermissions = getNeededPermissions(this)
        if (!neededPermissions.isEmpty())
            requestPermissions(this, neededPermissions)
        else
            finishOnboarding()
    }

    private fun finishOnboarding() {
        Answers.getInstance().logCustom(CustomEvent("Finish Onboarding"))
        val editor = prefs.edit()
        editor.putBoolean(ONBOARDING_COMPLETED, true)
        editor.apply()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }

    override fun onPageScrollStateChanged(state: Int) {}

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
        if (position < (sectionsPagerAdapter.count - 1)) {
            rootView.setBackgroundColor(argbEvaluator.evaluate(positionOffset, colors[position], colors[position + 1]) as Int)
        } else {
            rootView.setBackgroundColor(colors.last())
        }
    }

    override fun onPageSelected(position: Int) {
        indicatorOne.alpha = if (position == 0) 1f else 0.5f
        indicatorTwo.alpha = if (position == 1) 1f else 0.5f
        indicatorThree.alpha = if (position == 2) 1f else 0.5f
    }

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): Fragment {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return if  (position == 0 || position == 1) PlaceholderFragment.newInstance(position)
            else PermissionsFragment.newInstance(this@OnboardingActivity)
        }

        override fun getCount() = 3

        override fun getPageTitle(position: Int): CharSequence? {
            when (position) {
                0 -> return "SECTION 1"
                1 -> return "SECTION 2"
                2 -> return "SECTION 3"
            }
            return null
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    class PlaceholderFragment : Fragment() {

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                                  savedInstanceState: Bundle?): View? {
            val rootView = inflater.inflate(R.layout.fragment_onboarding, container, false)
            val titleView = rootView.findViewById<View>(R.id.onboarding_title) as TextView
            val messageView = rootView.findViewById<View>(R.id.onboarding_message) as TextView
            val imageView = rootView.findViewById<View>(R.id.onboarding_image) as ImageView

            val position = arguments.get(ARG_SECTION_NUMBER) as Int
            titleView.text = getTitleForPosition(position)
            messageView.text = getMessageForPosition(position)
            imageView.setImageResource(getImageForPosition(position))

            rootView.tag = position

            return rootView
        }

        private fun getTitleForPosition(position: Int): String {
            if (position == 0) {
                return getString(R.string.text_anywhere)
            } else {
                return getString(R.string.dont_change)
            }
        }

        private fun getMessageForPosition(position: Int): String {
            if (position == 0) {
                return getString(R.string.text_anywhere_message)
            } else {
                return getString(R.string.dont_change_message)
            }
        }

        private fun getImageForPosition(position: Int): Int {
            if (position == 0) {
                return R.drawable.ic_chat
            } else {
                return R.drawable.ic_sms
            }
        }

        companion object {
            /**
             * The fragment argument representing the section number for this
             * fragment.
             */
            private val ARG_SECTION_NUMBER = "section_number"

            /**
             * Returns a new instance of this fragment for the given section
             * number.
             */
            fun newInstance(sectionNumber: Int): PlaceholderFragment {
                val fragment = PlaceholderFragment()
                val args = Bundle()
                args.putInt(ARG_SECTION_NUMBER, sectionNumber)
                fragment.arguments = args
                return fragment
            }
        }
    }
}
