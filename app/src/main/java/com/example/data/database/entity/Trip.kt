package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trips")
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val startOdometer: Double = 0.0,
    val endOdometer: Double = 0.0,
    val totalGpsDistanceMeters: Double = 0.0,
    val bikeDistanceMeters: Double = 0.0,
    val walkingDistanceMeters: Double = 0.0,
    val durationSeconds: Long = 0L,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val fuelCost: Double = 0.0,
    val tollCost: Double = 0.0,
    val parkingCost: Double = 0.0,
    val otherCost: Double = 0.0,
    val notes: String = "",
    val startLatitude: Double? = null,
    val startLongitude: Double? = null,
    val endLatitude: Double? = null,
    val endLongitude: Double? = null,
    val status: String = "COMPLETED" // NOT_STARTED, RUNNING, PAUSED, COMPLETED
) {
    val totalTripExpense: Double
        get() = fuelCost + tollCost + parkingCost + otherCost

    val odometerDistance: Double
        get() = if (endOdometer >= startOdometer) endOdometer - startOdometer else 0.0

    val costPerKm: Double
        get() {
            val distKm = if (totalGpsDistanceMeters > 0) totalGpsDistanceMeters / 1000.0 else odometerDistance
            return if (distKm > 0.01) totalTripExpense / distKm else 0.0
        }
}
