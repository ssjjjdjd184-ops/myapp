package com.example.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "credit_payments",
    foreignKeys = [
        ForeignKey(
            entity = CreditAccount::class,
            parentColumns = ["id"],
            childColumns = ["creditId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["creditId"])]
)
data class CreditPayment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val creditId: Long,
    val dateMillis: Long = System.currentTimeMillis(),
    val amount: Double,
    val notes: String = ""
)
