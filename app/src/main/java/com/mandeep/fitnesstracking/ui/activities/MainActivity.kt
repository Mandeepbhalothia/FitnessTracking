package com.mandeep.fitnesstracking.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.mandeep.fitnesstracking.R
import com.mandeep.fitnesstracking.common.Constants
import com.mandeep.fitnesstracking.common.Constants.ACTION_SHOW_TRACKING_FRAGMENT
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigateToTrackingFragmentIfNeeded(intent)

        setSupportActionBar(toolbar)

        updateToolBarText()

        bottomNavigationView.setupWithNavController(/*findNavController(R.id.navHostFragment)*/
            navHostFragment.findNavController()
        )

        bottomNavigationView.setOnNavigationItemReselectedListener { /* No Operation*/ }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.settingsFragment, R.id.runFragment, R.id.statisticsFragment ->
                    bottomNavigationView.visibility = VISIBLE
                else -> bottomNavigationView.visibility = GONE
            }
        }

    }

    @SuppressLint("SetTextI18n")
    private fun updateToolBarText() {
        val name = sharedPref.getString(Constants.KEY_NAME, "") ?: ""
        if (name.isNotEmpty())
            tvToolbarTitle.text = "Let's go $name!"
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        navigateToTrackingFragmentIfNeeded(intent)
    }

    private fun navigateToTrackingFragmentIfNeeded(intent: Intent?) {
        if (intent?.action == ACTION_SHOW_TRACKING_FRAGMENT) {
            navHostFragment.findNavController().navigate(R.id.action_global_trackingFragment)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}