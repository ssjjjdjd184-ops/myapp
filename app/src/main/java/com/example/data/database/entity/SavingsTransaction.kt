package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_transactions",
    foreignKeys = [
        ForeignKey(
            entity = SavingsAccount::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class SavingsTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val accountId: Long,
    val dateMillis: Long,
    val amount: Double,
    val type: String, // "Deposit", "Withdrawal"
    val notes: String = ""
)
