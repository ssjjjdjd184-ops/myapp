package com.example.service

import android.content.Context
import android.location.Location
import com.example.data.database.AppDatabase
import com.example.data.database.entity.Trip
import com.example.data.database.entity.TripRoutePoint
import com.example.location.LocationFilter
import com.example.location.MovementClassifier
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TripTrackingManager private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(appContext)
    private val movementClassifier = MovementClassifier()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _liveState = MutableStateFlow(TripLiveState())
    val liveState: StateFlow<TripLiveState> = _liveState.asStateFlow()

    private var lastValidLocation: Location? = null
    private var timerJob: Job? = null
    private var speedSamples = mutableListOf<Double>()

    fun startTrip(
        name: String,
        startOdometer: Double,
        notes: String
    ) {
        scope.launch {
            val startTime = System.currentTimeMillis()
            val newTrip = Trip(
                name = name.ifBlank { "Trip ${java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault()).format(java.util.Date(startTime))}" },
                startTimeMillis = startTime,
                startOdometer = startOdometer,
                notes = notes,
                status = "RUNNING"
            )
            val tripId = database.tripDao().insertTrip(newTrip)

            movementClassifier.reset()
            lastValidLocation = null
            speedSamples.clear()

            _liveState.value = TripLiveState(
                tripId = tripId,
                tripName = newTrip.name,
                tripState = "RUNNING",
                startOdometer = startOdometer,
                notes = notes,
                gpsStatusMessage = "Waiting for GPS signal"
            )

            startTimer()
            TripLocationService.startService(appContext)
        }
    }

    fun pauseTrip() {
        if (_liveState.value.tripState != "RUNNING") return
        stopTimer()
        _liveState.update { it.copy(tripState = "PAUSED", currentSpeedKmh = 0.0, movementType = "Stopped") }
        val tripId = _liveState.value.tripId ?: return
        scope.launch {
            val trip = database.tripDao().getTripById(tripId)
            if (trip != null) {
                database.tripDao().updateTrip(trip.copy(status = "PAUSED"))
            }
        }
    }

    fun resumeTrip() {
        if (_liveState.value.tripState != "PAUSED") return
        _liveState.update { it.copy(tripState = "RUNNING") }
        startTimer()
        val tripId = _liveState.value.tripId ?: return
        scope.launch {
            val trip = database.tripDao().getTripById(tripId)
            if (trip != null) {
                database.tripDao().updateTrip(trip.copy(status = "RUNNING"))
            }
        }
    }

    suspend fun stopTrip(
        endOdometer: Double,
        fuelCost: Double,
        tollCost: Double,
        parkingCost: Double,
        otherCost: Double,
        notes: String
    ): Trip? {
        val currentState = _liveState.value
        val tripId = currentState.tripId ?: return null
        stopTimer()

        val endTime = System.currentTimeMillis()
        val existingTrip = database.tripDao().getTripById(tripId)

        val updatedTrip = (existingTrip ?: Trip(
            id = tripId,
            name = currentState.tripName,
            startTimeMillis = endTime - (currentState.durationSeconds * 1000)
        )).copy(
            endTimeMillis = endTime,
            endOdometer = if (endOdometer > 0) endOdometer else currentState.startOdometer,
            totalGpsDistanceMeters = currentState.totalDistanceMeters,
            bikeDistanceMeters = currentState.bikeDistanceMeters,
            walkingDistanceMeters = currentState.walkingDistanceMeters,
            durationSeconds = currentState.durationSeconds,
            avgSpeedKmh = currentState.avgSpeedKmh,
            maxSpeedKmh = currentState.maxSpeedKmh,
            fuelCost = fuelCost,
            tollCost = tollCost,
            parkingCost = parkingCost,
            otherCost = otherCost,
            notes = if (notes.isNotBlank()) notes else currentState.notes,
            endLatitude = currentState.currentLatitude,
            endLongitude = currentState.currentLongitude,
            status = "COMPLETED"
        )

        database.tripDao().updateTrip(updatedTrip)

        TripLocationService.stopService(appContext)

        _liveState.value = TripLiveState(
            tripState = "COMPLETED",
            tripId = tripId,
            tripName = updatedTrip.name,
            totalDistanceMeters = updatedTrip.totalGpsDistanceMeters,
            bikeDistanceMeters = updatedTrip.bikeDistanceMeters,
            walkingDistanceMeters = updatedTrip.walkingDistanceMeters,
            durationSeconds = updatedTrip.durationSeconds,
            avgSpeedKmh = updatedTrip.avgSpeedKmh,
            maxSpeedKmh = updatedTrip.maxSpeedKmh,
            notes = updatedTrip.notes
        )

        return updatedTrip
    }

    fun onLocationReceived(location: Location) {
        if (_liveState.value.tripState != "RUNNING") return

        val prev = lastValidLocation
        val isValid = LocationFilter.isValidPoint(prev, location)

        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        val gpsStatus = when {
            accuracy > 30f -> "GPS accuracy is poor (${accuracy.toInt()}m)"
            accuracy > 0f -> "GPS connected (±${accuracy.toInt()}m)"
            else -> "GPS connected"
        }

        if (!isValid) {
            _liveState.update { it.copy(gpsAccuracyMeters = accuracy, gpsStatusMessage = gpsStatus) }
            return
        }

        var speedKmh = 0.0
        if (location.hasSpeed() && location.speed >= 0f) {
            speedKmh = location.speed.toDouble() * 3.6
        } else if (prev != null) {
            val dt = (location.time - prev.time) / 1000.0
            if (dt > 0.5) {
                val dist = location.distanceTo(prev)
                speedKmh = (dist / dt) * 3.6
            }
        }

        if (speedKmh > 160.0) speedKmh = 0.0 // Reject speed spikes

        val mode = movementClassifier.classify(speedKmh)
        var deltaMeters = 0.0

        if (prev != null) {
            deltaMeters = location.distanceTo(prev).toDouble()
        }

        lastValidLocation = location
        speedSamples.add(speedKmh)

        val tripId = _liveState.value.tripId ?: return

        // Persist point to DB
        scope.launch {
            val point = TripRoutePoint(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                speedKmh = speedKmh.toFloat(),
                accuracy = accuracy,
                movementType = mode
            )
            database.tripDao().insertRoutePoint(point)

            // Also update start latitude/longitude if not yet set
            val trip = database.tripDao().getTripById(tripId)
            if (trip != null && trip.startLatitude == null) {
                database.tripDao().updateTrip(
                    trip.copy(
                        startLatitude = location.latitude,
                        startLongitude = location.longitude
                    )
                )
            }
        }

        _liveState.update { current ->
            val newTotalDist = current.totalDistanceMeters + deltaMeters
            val newBikeDist = if (mode == "Bike") current.bikeDistanceMeters + deltaMeters else current.bikeDistanceMeters
            val newWalkDist = if (mode == "Walking") current.walkingDistanceMeters + deltaMeters else current.walkingDistanceMeters

            val newMaxSpeed = maxOf(current.maxSpeedKmh, speedKmh)
            val avgSpeed = if (speedSamples.isNotEmpty()) speedSamples.average() else 0.0

            val newPoint = TripRoutePoint(
                tripId = tripId,
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = location.time,
                speedKmh = speedKmh.toFloat(),
                accuracy = accuracy,
                movementType = mode
            )

            current.copy(
                currentSpeedKmh = speedKmh,
                avgSpeedKmh = avgSpeed,
                maxSpeedKmh = newMaxSpeed,
                movementType = mode,
                totalDistanceMeters = newTotalDist,
                bikeDistanceMeters = newBikeDist,
                walkingDistanceMeters = newWalkDist,
                gpsAccuracyMeters = accuracy,
                gpsStatusMessage = gpsStatus,
                currentLatitude = location.latitude,
                currentLongitude = location.longitude,
                routePoints = current.routePoints + newPoint
            )
        }
    }

    fun updateGpsStatus(message: String) {
        _liveState.update { it.copy(gpsStatusMessage = message) }
    }

    private fun startTimer() {
        stopTimer()
        timerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                _liveState.update { it.copy(durationSeconds = it.durationSeconds + 1) }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    companion object {
        @Volatile
        private var INSTANCE: TripTrackingManager? = null

        fun getInstance(context: Context): TripTrackingManager {
            return INSTANCE ?: synchronized(this) {
                val instance = TripTrackingManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
