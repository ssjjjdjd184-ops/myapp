package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.DecimalFormat

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("tripcalc_preferences", Context.MODE_PRIVATE)

    private val _currency = MutableStateFlow(prefs.getString(KEY_CURRENCY, DEFAULT_CURRENCY) ?: DEFAULT_CURRENCY)
    val currency: StateFlow<String> = _currency.asStateFlow()

    private val _distanceUnit = MutableStateFlow(prefs.getString(KEY_DISTANCE_UNIT, DEFAULT_DISTANCE_UNIT) ?: DEFAULT_DISTANCE_UNIT)
    val distanceUnit: StateFlow<String> = _distanceUnit.asStateFlow()

    private val _speedUnit = MutableStateFlow(prefs.getString(KEY_SPEED_UNIT, DEFAULT_SPEED_UNIT) ?: DEFAULT_SPEED_UNIT)
    val speedUnit: StateFlow<String> = _speedUnit.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, DEFAULT_THEME_MODE) ?: DEFAULT_THEME_MODE)
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setCurrency(newCurrency: String) {
        prefs.edit().putString(KEY_CURRENCY, newCurrency).apply()
        _currency.value = newCurrency
    }

    fun setDistanceUnit(newUnit: String) {
        prefs.edit().putString(KEY_DISTANCE_UNIT, newUnit).apply()
        _distanceUnit.value = newUnit
    }

    fun setSpeedUnit(newUnit: String) {
        prefs.edit().putString(KEY_SPEED_UNIT, newUnit).apply()
        _speedUnit.value = newUnit
    }

    fun setThemeMode(newMode: String) {
        prefs.edit().putString(KEY_THEME_MODE, newMode).apply()
        _themeMode.value = newMode
    }

    fun formatMoney(amount: Double): String {
        val symbol = when (_currency.value) {
            "₹ INR" -> "₹"
            "$ USD" -> "$"
            "€ EUR" -> "€"
            "£ GBP" -> "£"
            else -> "₹"
        }
        val formatter = DecimalFormat("#,##,##0.00")
        return "$symbol${formatter.format(amount)}"
    }

    fun formatDistance(meters: Double): String {
        val isMiles = _distanceUnit.value == "mi"
        val dist = if (isMiles) (meters / 1609.344) else (meters / 1000.0)
        val unit = if (isMiles) "mi" else "km"
        return String.format("%.2f %s", dist, unit)
    }

    fun formatSpeed(speedKmh: Double): String {
        val isMph = _speedUnit.value == "mph"
        val speed = if (isMph) (speedKmh * 0.621371) else speedKmh
        val unit = if (isMph) "mph" else "km/h"
        return String.format("%.1f %s", speed.coerceAtLeast(0.0), unit)
    }

    companion object {
        const val KEY_CURRENCY = "pref_currency"
        const val KEY_DISTANCE_UNIT = "pref_distance_unit"
        const val KEY_SPEED_UNIT = "pref_speed_unit"
        const val KEY_THEME_MODE = "pref_theme_mode"

        const val DEFAULT_CURRENCY = "₹ INR"
        const val DEFAULT_DISTANCE_UNIT = "km"
        const val DEFAULT_SPEED_UNIT = "km/h"
        const val DEFAULT_THEME_MODE = "System"

        @Volatile
        private var INSTANCE: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return INSTANCE ?: synchronized(this) {
                val instance = PreferenceManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
