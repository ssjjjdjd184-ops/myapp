package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dateMillis: Long,
    val category: String, // Fuel, Food, Travel, Parking, Toll, Shopping, Bills, Maintenance, Medical, Education, Home, Other
    val description: String,
    val amount: Double,
    val paymentMethod: String, // Cash, UPI, Bank, Card, Other
    val notes: String = ""
)
