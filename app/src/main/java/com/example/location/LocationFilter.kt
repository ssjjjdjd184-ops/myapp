package com.example.location

import android.location.Location

object LocationFilter {

    /**
     * Calculates distance in meters between two lat/lng points
     */
    fun distanceBetween(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Checks if a new location point is valid, filtering out noise, duplicate points,
     * poor accuracy, and unrealistic teleportation jumps.
     */
    fun isValidPoint(
        prevLocation: Location?,
        newLocation: Location
    ): Boolean {
        // Reject if accuracy is worse than 45 meters
        if (newLocation.hasAccuracy() && newLocation.accuracy > 45f) {
            return false
        }

        if (prevLocation == null) return true

        val timeDeltaSec = (newLocation.time - prevLocation.time) / 1000.0
        // Ignore negative or duplicate timestamps
        if (timeDeltaSec <= 0.2) return false

        val distanceMeters = prevLocation.distanceTo(newLocation)

        // Ignore micro-jitter / tiny GPS noise if movement is under 2.5 meters and accuracy is mediocre
        if (distanceMeters < 2.5 && newLocation.accuracy > 12f) {
            return false
        }

        // Calculate implied speed
        val impliedSpeedKmh = (distanceMeters / timeDeltaSec) * 3.6

        // Ignore unrealistic jumps (over 180 km/h for bike/walking)
        if (impliedSpeedKmh > 180.0) {
            return false
        }

        return true
    }
}
