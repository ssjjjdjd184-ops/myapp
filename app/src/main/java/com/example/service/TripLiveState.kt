package com.example.service

import com.example.data.database.entity.TripRoutePoint

data class TripLiveState(
    val tripId: Long? = null,
    val tripName: String = "",
    val tripState: String = "NOT_STARTED", // NOT_STARTED, RUNNING, PAUSED, COMPLETED
    val currentSpeedKmh: Double = 0.0,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val movementType: String = "Stopped", // Stopped, Walking, Bike
    val totalDistanceMeters: Double = 0.0,
    val bikeDistanceMeters: Double = 0.0,
    val walkingDistanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val gpsAccuracyMeters: Float = 0f,
    val gpsStatusMessage: String = "Waiting for GPS signal",
    val currentLatitude: Double? = null,
    val currentLongitude: Double? = null,
    val startOdometer: Double = 0.0,
    val notes: String = "",
    val routePoints: List<TripRoutePoint> = emptyList()
)
