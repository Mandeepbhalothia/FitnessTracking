package com.mandeep.fitnesstracking.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.*
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.snackbar.Snackbar
import com.mandeep.fitnesstracking.R
import com.mandeep.fitnesstracking.common.Constants.ACTION_PAUSE_SERVICE
import com.mandeep.fitnesstracking.common.Constants.ACTION_START_OR_RESUME_SERVICE
import com.mandeep.fitnesstracking.common.Constants.ACTION_STOP_SERVICE
import com.mandeep.fitnesstracking.common.Constants.MAP_ZOOM
import com.mandeep.fitnesstracking.common.Constants.POLYLINE_COLOR
import com.mandeep.fitnesstracking.common.Constants.POLYLINE_WIDTH
import com.mandeep.fitnesstracking.common.TrackingUtilities
import com.mandeep.fitnesstracking.db.Run
import com.mandeep.fitnesstracking.services.Polyline
import com.mandeep.fitnesstracking.services.TrackingService
import com.mandeep.fitnesstracking.viewmodels.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.fragment_tracking.*
import java.util.*
import javax.inject.Inject
import kotlin.math.round


@AndroidEntryPoint
class TrackingFragment : Fragment(R.layout.fragment_tracking) {
    private val CANCEL_DIALOG = "cancel_dialog"

    private val viewModel: MainViewModel by viewModels()
    private var isTracking = false
    private var pathPoints = mutableListOf<Polyline>()
    private var map: GoogleMap? = null
    private var curTimeInMillis = 0L
    private var menu: Menu? = null

    @set: Inject
    var weight = 70f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mapView?.onCreate(savedInstanceState)
        mapView.getMapAsync {
            map = it
            addAllPolyLines()
        }

        btnFinishRun.setOnClickListener {
            zoomMapForScreenshot()
            endRunAndSaveToDb()
        }

        btnToggleRun.setOnClickListener {
            toggleRun()
        }

        if (savedInstanceState != null) {
            val cancelTrackingDialog = parentFragmentManager.findFragmentByTag(
                CANCEL_DIALOG
            ) as CancelTrackingDialog?
            cancelTrackingDialog?.setYesListener {
                stopRun()
            }
        }

        subscribeObservers()

    }

    private fun subscribeObservers() {
        TrackingService.isTracking.observe(viewLifecycleOwner, Observer {
            updateTracking(it)
        })

        TrackingService.pathPoints.observe(viewLifecycleOwner, Observer {
            pathPoints = it
            addLatestPolyline()
            moveCameraToUser()
        })

        TrackingService.timeRunInMillis.observe(viewLifecycleOwner, Observer {
            curTimeInMillis = it
            val formattedString = TrackingUtilities.getFormattedTimeString(curTimeInMillis, true)
            tvTimer.text = formattedString
        })
    }

    private fun toggleRun() {
        if (isTracking) {
            menu?.getItem(0)?.isVisible = true
            sendCommandToService(ACTION_PAUSE_SERVICE)
        } else {
            sendCommandToService(ACTION_START_OR_RESUME_SERVICE)
        }
    }

    private fun updateTracking(isTracking: Boolean) {
        this.isTracking = isTracking
        if (!isTracking && curTimeInMillis > 0L) {
            btnToggleRun.text = "Start"
            btnFinishRun.visibility = View.VISIBLE
        } else if (isTracking) {
            btnToggleRun.text = "Stop"
            menu?.getItem(0)?.isVisible = true
            btnFinishRun.visibility = View.GONE
        }
    }

    private fun moveCameraToUser() {
        if (pathPoints.isNotEmpty() && pathPoints.last().isNotEmpty()) {
            map?.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    pathPoints.last().last(),
                    MAP_ZOOM
                )
            )
        }
    }

    private fun addAllPolyLines() {
        for (polyline in pathPoints) {
            val polyLineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .addAll(polyline)
            map?.addPolyline(polyLineOptions)
        }
    }

    private fun addLatestPolyline() {
        if (pathPoints.isNotEmpty() && pathPoints.last().size > 1) {
            val preLastPoint = pathPoints.last()[pathPoints.last().size - 2]
            val lastPoint = pathPoints.last().last()
            val polyLineOptions = PolylineOptions()
                .color(POLYLINE_COLOR)
                .width(POLYLINE_WIDTH)
                .add(preLastPoint)
                .add(lastPoint)
            map?.addPolyline(polyLineOptions)
        }
    }

    private fun sendCommandToService(action: String) =
        Intent(requireContext(), TrackingService::class.java).apply {
            this.action = action
            requireContext().startService(this)
        }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.close_tracking_menu, menu)
        this.menu = menu
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (curTimeInMillis > 0L)
            this.menu?.get(0)?.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.closeTrackingMenu -> showCancelTrackingDialog()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCancelTrackingDialog() {
        CancelTrackingDialog().apply {
            setYesListener {
                stopRun()
            }
        }.show(parentFragmentManager, CANCEL_DIALOG)
    }

    private fun zoomMapForScreenshot() {
        val boundBuilder = LatLngBounds.builder()
        for (polyline in pathPoints) {
            for (latLng in polyline) {
                boundBuilder.include(latLng)
            }
        }
        map?.moveCamera(
            CameraUpdateFactory.newLatLngBounds(
                boundBuilder.build(),
                mapView.width,
                mapView.height,
                (mapView.height * 0.05f).toInt()
            )
        )
    }

    private fun endRunAndSaveToDb() {
        map?.snapshot { bitmap ->
            var totalDistance = 0
            for (polyline in pathPoints) {
                totalDistance += TrackingUtilities.calculatePolyLineDistance(polyline).toInt()
            }
            val avgSpeed =
                round((totalDistance / 1000f) / (curTimeInMillis / 1000f / 60 / 60) * 10) / 10f
            val datetimeStamp = Calendar.getInstance().timeInMillis
            val caloriesBurned = ((totalDistance / 1000) * weight).toInt()
            val run =
                Run(bitmap, datetimeStamp, avgSpeed, totalDistance, curTimeInMillis, caloriesBurned)
            viewModel.saveRun(run)
            Snackbar.make(
                requireActivity().findViewById(R.id.rootView),
                "Run saved successfully",
                Snackbar.LENGTH_LONG
            )
            stopRun()
        }
    }

    private fun stopRun() {
        tvTimer.text = "00:00:00:00"
        sendCommandToService(ACTION_STOP_SERVICE)
        findNavController().navigate(R.id.action_trackingFragment_to_runFragment)
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

}