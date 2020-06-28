package com.mandeep.fitnesstracking.ui.fragments

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.mandeep.fitnesstracking.R
import com.mandeep.fitnesstracking.viewmodels.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val viewModel: StatisticsViewModel by viewModels()

}