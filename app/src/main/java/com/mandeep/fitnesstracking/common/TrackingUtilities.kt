package com.mandeep.fitnesstracking.common

import android.Manifest
import android.content.Context
import android.location.Location
import android.os.Build
import com.google.android.gms.maps.model.LatLng
import com.mandeep.fitnesstracking.services.Polyline
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

object TrackingUtilities {

    fun hasLocationPermission(context: Context) =
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    fun getFormattedTimeString(ms: Long, includeMillis: Boolean = false): String {
        var timeInMillis = ms
        val hours = TimeUnit.MILLISECONDS.toHours(timeInMillis)
        timeInMillis -= TimeUnit.HOURS.toMillis(hours)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMillis)
        timeInMillis -= TimeUnit.MINUTES.toMillis(minutes)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeInMillis)
        if (!includeMillis) {
            return "${if (hours < 10) "0" else ""}$hours:" +
                    "${if (minutes < 10) "0" else ""}$minutes:" +
                    "${if (seconds < 10) "0" else ""}$seconds"
        }
        timeInMillis -= TimeUnit.SECONDS.toMillis(seconds)
        timeInMillis /= 10
        return "${if (hours < 10) "0" else ""}$hours:" +
                "${if (minutes < 10) "0" else ""}$minutes:" +
                "${if (seconds < 10) "0" else ""}$seconds:" +
                "${if (timeInMillis < 10) "0" else ""}$timeInMillis"

    }

    fun calculatePolyLineDistance(polyline: Polyline): Float{
        var distanceInMeters = 0f
        for (i in 0..polyline.size -2){
            val point1 = polyline[i]
            val point2 = polyline[i+1]

            val result = FloatArray(1)

            Location.distanceBetween(
                point1.latitude,
                point1.longitude,
                point2.latitude,
                point2.longitude,
                result
            )
            distanceInMeters += result[0]
        }
        return distanceInMeters
    }

}