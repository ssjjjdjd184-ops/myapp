package com.example.ui.navigation

sealed class Screen(val route: String, val title: String) {
    data object Dashboard : Screen("dashboard", "Dashboard")
    data object Trips : Screen("trips", "Trips")
    data object Expenses : Screen("expenses", "Expenses")
    data object Savings : Screen("savings", "Savings")
    data object Credit : Screen("credit", "Credit")
    data object Reports : Screen("reports", "Reports")
    data object LiveTrip : Screen("live_trip", "Live Trip")
    data object TripMap : Screen("trip_map", "Trip Map")
    data object Settings : Screen("settings", "Settings")
}
