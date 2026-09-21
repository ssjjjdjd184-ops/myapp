package com.example.data.repository

import com.example.data.database.dao.TripDao
import com.example.data.database.entity.Trip
import com.example.data.database.entity.TripExpense
import com.example.data.database.entity.TripRoutePoint
import kotlinx.coroutines.flow.Flow

class TripRepository(private val tripDao: TripDao) {
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripsBetween(startMillis: Long, endMillis: Long): Flow<List<Trip>> =
        tripDao.getTripsBetween(startMillis, endMillis)

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    fun getTripByIdFlow(id: Long): Flow<Trip?> = tripDao.getTripByIdFlow(id)

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    suspend fun deleteTripById(id: Long) = tripDao.deleteTripById(id)

    // Route points
    suspend fun insertRoutePoint(point: TripRoutePoint) = tripDao.insertRoutePoint(point)

    suspend fun insertRoutePoints(points: List<TripRoutePoint>) = tripDao.insertRoutePoints(points)

    fun getRoutePointsForTrip(tripId: Long): Flow<List<TripRoutePoint>> =
        tripDao.getRoutePointsForTrip(tripId)

    suspend fun getRoutePointsList(tripId: Long): List<TripRoutePoint> =
        tripDao.getRoutePointsList(tripId)

    // Trip expenses
    suspend fun insertTripExpense(expense: TripExpense) = tripDao.insertTripExpense(expense)

    suspend fun updateTripExpense(expense: TripExpense) = tripDao.updateTripExpense(expense)

    suspend fun deleteTripExpense(expense: TripExpense) = tripDao.deleteTripExpense(expense)

    fun getTripExpenses(tripId: Long): Flow<List<TripExpense>> = tripDao.getTripExpenses(tripId)

    suspend fun getTripExpensesList(tripId: Long): List<TripExpense> =
        tripDao.getTripExpensesList(tripId)
}
