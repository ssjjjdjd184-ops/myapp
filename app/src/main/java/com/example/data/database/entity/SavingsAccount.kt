package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "savings_accounts")
data class SavingsAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    val startingAmount: Double = 0.0,
    val currentBalance: Double = 0.0,
    val targetAmount: Double = 0.0,
    val interestRate: Double = 0.0, // annual %
    val interestType: String = "No Interest", // "No Interest", "Simple Interest", "Compound Interest"
    val interestFrequency: String = "Monthly", // "Daily", "Monthly", "Quarterly", "Yearly"
    val startDateMillis: Long = System.currentTimeMillis(),
    val maturityDateMillis: Long? = null,
    val allowOverdraft: Boolean = false,
    val notes: String = ""
) {
    fun calculateInterest(asOfMillis: Long = System.currentTimeMillis()): Double {
        if (interestType == "No Interest" || interestRate <= 0.0 || currentBalance <= 0.0) return 0.0
        val durationMillis = (asOfMillis - startDateMillis).coerceAtLeast(0L)
        val timeYears = durationMillis / (365.25 * 24.0 * 3600.0 * 1000.0)
        if (timeYears <= 0.0) return 0.0

        return when (interestType) {
            "Simple Interest" -> {
                currentBalance * (interestRate / 100.0) * timeYears
            }
            "Compound Interest" -> {
                val n = when (interestFrequency) {
                    "Daily" -> 365.0
                    "Monthly" -> 12.0
                    "Quarterly" -> 4.0
                    "Yearly" -> 1.0
                    else -> 12.0
                }
                val amount = currentBalance * (1.0 + (interestRate / 100.0) / n).pow(n * timeYears)
                (amount - currentBalance).coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }

    val progressPercentage: Float
        get() = if (targetAmount > 0) ((currentBalance / targetAmount) * 100).toFloat().coerceIn(0f, 100f) else 100f

    val remainingTarget: Double
        get() = (targetAmount - currentBalance).coerceAtLeast(0.0)
}
