package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.database.dao.CreditDao
import com.example.data.database.dao.ExpenseDao
import com.example.data.database.dao.SavingsDao
import com.example.data.database.dao.TripDao
import com.example.data.database.entity.*

@Database(
    entities = [
        Trip::class,
        TripRoutePoint::class,
        TripExpense::class,
        Expense::class,
        SavingsAccount::class,
        SavingsTransaction::class,
        CreditAccount::class,
        CreditPayment::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun savingsDao(): SavingsDao
    abstract fun creditDao(): CreditDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tripcalc_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
