package com.example.location

class MovementClassifier {
    private var lastMode: String = "Stopped"

    fun classify(speedKmh: Double): String {
        val newMode = when {
            speedKmh < 1.5 -> "Stopped"
            speedKmh < 10.0 -> "Walking"
            speedKmh > 12.0 -> "Bike"
            else -> {
                // Between 10 and 12 km/h: keep previous classification (hysteresis)
                if (lastMode == "Stopped") "Walking" else lastMode
            }
        }
        lastMode = newMode
        return newMode
    }

    fun reset() {
        lastMode = "Stopped"
    }
}
