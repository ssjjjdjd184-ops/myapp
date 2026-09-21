package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.Trip
import com.example.data.database.entity.TripExpense
import com.example.data.database.entity.TripRoutePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDao {
    @Query("SELECT * FROM trips ORDER BY startTimeMillis DESC")
    fun getAllTrips(): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE startTimeMillis >= :startMillis AND startTimeMillis <= :endMillis ORDER BY startTimeMillis DESC")
    fun getTripsBetween(startMillis: Long, endMillis: Long): Flow<List<Trip>>

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): Trip?

    @Query("SELECT * FROM trips WHERE id = :tripId")
    fun getTripByIdFlow(tripId: Long): Flow<Trip?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTripById(tripId: Long)

    // Route points
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoint(point: TripRoutePoint): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoutePoints(points: List<TripRoutePoint>)

    @Query("SELECT * FROM trip_route_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    fun getRoutePointsForTrip(tripId: Long): Flow<List<TripRoutePoint>>

    @Query("SELECT * FROM trip_route_points WHERE tripId = :tripId ORDER BY timestamp ASC")
    suspend fun getRoutePointsList(tripId: Long): List<TripRoutePoint>

    @Query("DELETE FROM trip_route_points WHERE tripId = :tripId")
    suspend fun deleteRoutePointsForTrip(tripId: Long)

    // Trip expenses
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTripExpense(expense: TripExpense): Long

    @Update
    suspend fun updateTripExpense(expense: TripExpense)

    @Delete
    suspend fun deleteTripExpense(expense: TripExpense)

    @Query("SELECT * FROM trip_expenses WHERE tripId = :tripId ORDER BY dateMillis ASC")
    fun getTripExpenses(tripId: Long): Flow<List<TripExpense>>

    @Query("SELECT * FROM trip_expenses WHERE tripId = :tripId ORDER BY dateMillis ASC")
    suspend fun getTripExpensesList(tripId: Long): List<TripExpense>

    @Query("SELECT * FROM trips")
    suspend fun getAllTripsSnapshot(): List<Trip>

    @Query("SELECT * FROM trip_route_points")
    suspend fun getAllRoutePointsSnapshot(): List<TripRoutePoint>

    @Query("SELECT * FROM trip_expenses")
    suspend fun getAllTripExpensesSnapshot(): List<TripExpense>

    @Query("DELETE FROM trips")
    suspend fun clearAllTrips()
}
