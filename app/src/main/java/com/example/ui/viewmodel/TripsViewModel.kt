package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.Trip
import com.example.data.database.entity.TripExpense
import com.example.data.database.entity.TripRoutePoint
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.TripRepository
import com.example.service.TripLiveState
import com.example.service.TripTrackingManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class TripsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val tripRepo = TripRepository(database.tripDao())
    private val trackingManager = TripTrackingManager.getInstance(application)
    val prefManager = PreferenceManager.getInstance(application)

    val liveState: StateFlow<TripLiveState> = trackingManager.liveState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTrip = MutableStateFlow<Trip?>(null)
    val selectedTrip: StateFlow<Trip?> = _selectedTrip.asStateFlow()

    private val _selectedTripRoutePoints = MutableStateFlow<List<TripRoutePoint>>(emptyList())
    val selectedTripRoutePoints: StateFlow<List<TripRoutePoint>> = _selectedTripRoutePoints.asStateFlow()

    private val _selectedTripExpenses = MutableStateFlow<List<TripExpense>>(emptyList())
    val selectedTripExpenses: StateFlow<List<TripExpense>> = _selectedTripExpenses.asStateFlow()

    val tripsList: StateFlow<List<Trip>> = combine(
        tripRepo.allTrips,
        _searchQuery
    ) { trips, query ->
        if (query.isBlank()) {
            trips
        } else {
            trips.filter { it.name.contains(query, ignoreCase = true) || it.notes.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startTrip(name: String, startOdometer: Double, notes: String) {
        trackingManager.startTrip(name, startOdometer, notes)
    }

    fun pauseTrip() {
        trackingManager.pauseTrip()
    }

    fun resumeTrip() {
        trackingManager.resumeTrip()
    }

    fun stopTrip(
        endOdometer: Double,
        fuelCost: Double,
        tollCost: Double,
        parkingCost: Double,
        otherCost: Double,
        notes: String,
        onStopped: (Trip) -> Unit
    ) {
        viewModelScope.launch {
            val trip = trackingManager.stopTrip(endOdometer, fuelCost, tollCost, parkingCost, otherCost, notes)
            if (trip != null) {
                // If fuel, toll, parking, or other costs were provided, also record TripExpense items
                if (fuelCost > 0) {
                    tripRepo.insertTripExpense(TripExpense(tripId = trip.id, title = "Fuel", category = "Fuel", amount = fuelCost))
                }
                if (tollCost > 0) {
                    tripRepo.insertTripExpense(TripExpense(tripId = trip.id, title = "Toll", category = "Toll", amount = tollCost))
                }
                if (parkingCost > 0) {
                    tripRepo.insertTripExpense(TripExpense(tripId = trip.id, title = "Parking", category = "Parking", amount = parkingCost))
                }
                if (otherCost > 0) {
                    tripRepo.insertTripExpense(TripExpense(tripId = trip.id, title = "Other Cost", category = "Other", amount = otherCost))
                }
                onStopped(trip)
            }
        }
    }

    fun selectTrip(trip: Trip) {
        _selectedTrip.value = trip
        viewModelScope.launch {
            _selectedTripRoutePoints.value = tripRepo.getRoutePointsList(trip.id)
            _selectedTripExpenses.value = tripRepo.getTripExpensesList(trip.id)
        }
    }

    fun updateTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepo.updateTrip(trip)
            if (_selectedTrip.value?.id == trip.id) {
                _selectedTrip.value = trip
            }
        }
    }

    fun deleteTrip(trip: Trip) {
        viewModelScope.launch {
            tripRepo.deleteTrip(trip)
            if (_selectedTrip.value?.id == trip.id) {
                _selectedTrip.value = null
                _selectedTripRoutePoints.value = emptyList()
                _selectedTripExpenses.value = emptyList()
            }
        }
    }

    fun addTripExpense(tripId: Long, title: String, category: String, amount: Double, notes: String) {
        viewModelScope.launch {
            val expense = TripExpense(
                tripId = tripId,
                title = title,
                category = category,
                amount = amount,
                notes = notes
            )
            tripRepo.insertTripExpense(expense)
            // Recalculate trip costs
            val currentTrip = tripRepo.getTripById(tripId)
            if (currentTrip != null) {
                val allExpenses = tripRepo.getTripExpensesList(tripId)
                val fuel = allExpenses.filter { it.category == "Fuel" }.sumOf { it.amount }
                val toll = allExpenses.filter { it.category == "Toll" }.sumOf { it.amount }
                val parking = allExpenses.filter { it.category == "Parking" }.sumOf { it.amount }
                val other = allExpenses.filter { it.category == "Other" }.sumOf { it.amount }
                val updated = currentTrip.copy(fuelCost = fuel, tollCost = toll, parkingCost = parking, otherCost = other)
                tripRepo.updateTrip(updated)
                _selectedTrip.value = updated
            }
            _selectedTripExpenses.value = tripRepo.getTripExpensesList(tripId)
        }
    }

    fun deleteTripExpense(expense: TripExpense) {
        viewModelScope.launch {
            tripRepo.deleteTripExpense(expense)
            val currentTrip = tripRepo.getTripById(expense.tripId)
            if (currentTrip != null) {
                val allExpenses = tripRepo.getTripExpensesList(expense.tripId)
                val fuel = allExpenses.filter { it.category == "Fuel" }.sumOf { it.amount }
                val toll = allExpenses.filter { it.category == "Toll" }.sumOf { it.amount }
                val parking = allExpenses.filter { it.category == "Parking" }.sumOf { it.amount }
                val other = allExpenses.filter { it.category == "Other" }.sumOf { it.amount }
                val updated = currentTrip.copy(fuelCost = fuel, tollCost = toll, parkingCost = parking, otherCost = other)
                tripRepo.updateTrip(updated)
                _selectedTrip.value = updated
            }
            _selectedTripExpenses.value = tripRepo.getTripExpensesList(expense.tripId)
        }
    }
}
