package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlin.math.pow

@Entity(tableName = "credit_accounts")
data class CreditAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val personName: String,
    val contact: String = "",
    val principalAmount: Double,
    val interestRate: Double = 0.0, // annual %
    val interestType: String = "No Interest", // "No Interest", "Simple Interest", "Compound Interest"
    val interestFrequency: String = "Monthly", // "Daily", "Monthly", "Quarterly", "Yearly"
    val startDateMillis: Long = System.currentTimeMillis(),
    val dueDateMillis: Long,
    val notes: String = ""
) {
    fun calculateAccruedInterest(asOfMillis: Long = System.currentTimeMillis()): Double {
        if (interestType == "No Interest" || interestRate <= 0.0 || principalAmount <= 0.0) return 0.0
        val durationMillis = (asOfMillis - startDateMillis).coerceAtLeast(0L)
        val timeYears = durationMillis / (365.25 * 24.0 * 3600.0 * 1000.0)
        if (timeYears <= 0.0) return 0.0

        return when (interestType) {
            "Simple Interest" -> {
                principalAmount * (interestRate / 100.0) * timeYears
            }
            "Compound Interest" -> {
                val n = when (interestFrequency) {
                    "Daily" -> 365.0
                    "Monthly" -> 12.0
                    "Quarterly" -> 4.0
                    "Yearly" -> 1.0
                    else -> 12.0
                }
                val amount = principalAmount * (1.0 + (interestRate / 100.0) / n).pow(n * timeYears)
                (amount - principalAmount).coerceAtLeast(0.0)
            }
            else -> 0.0
        }
    }

    fun getStatus(totalPaid: Double, asOfMillis: Long = System.currentTimeMillis()): String {
        val interest = calculateAccruedInterest(asOfMillis)
        val totalDue = principalAmount + interest
        val remaining = totalDue - totalPaid
        return when {
            remaining <= 0.05 -> "PAID"
            asOfMillis > dueDateMillis -> "OVERDUE"
            else -> "ACTIVE"
        }
    }
}
