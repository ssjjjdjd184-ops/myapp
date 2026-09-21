package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.database.dao.CreditDao
import com.example.data.database.dao.ExpenseDao
import com.example.data.database.dao.SavingsDao
import com.example.data.database.dao.TripDao
import com.example.data.database.entity.*
import kotlinx.coroutines.flow.Flow

/**
 * Unified repository class providing offline data persistence operations
 * across Trips, GPS Route Points, Expenses, Savings Accounts, and Credit Accounts.
 */
class AppRepository(
    val tripDao: TripDao,
    val expenseDao: ExpenseDao,
    val savingsDao: SavingsDao,
    val creditDao: CreditDao
) {
    constructor(database: AppDatabase) : this(
        tripDao = database.tripDao(),
        expenseDao = database.expenseDao(),
        savingsDao = database.savingsDao(),
        creditDao = database.creditDao()
    )

    // ==========================================
    // TRIP & ROUTE PERSISTENCE
    // ==========================================
    val allTrips: Flow<List<Trip>> = tripDao.getAllTrips()

    fun getTripsBetween(startMillis: Long, endMillis: Long): Flow<List<Trip>> =
        tripDao.getTripsBetween(startMillis, endMillis)

    suspend fun getTripById(id: Long): Trip? = tripDao.getTripById(id)

    fun getTripByIdFlow(id: Long): Flow<Trip?> = tripDao.getTripByIdFlow(id)

    suspend fun insertTrip(trip: Trip): Long = tripDao.insertTrip(trip)

    suspend fun updateTrip(trip: Trip) = tripDao.updateTrip(trip)

    suspend fun deleteTrip(trip: Trip) = tripDao.deleteTrip(trip)

    suspend fun deleteTripById(id: Long) = tripDao.deleteTripById(id)

    // Route points
    suspend fun insertRoutePoint(point: TripRoutePoint): Long = tripDao.insertRoutePoint(point)

    suspend fun insertRoutePoints(points: List<TripRoutePoint>) = tripDao.insertRoutePoints(points)

    fun getRoutePointsForTrip(tripId: Long): Flow<List<TripRoutePoint>> =
        tripDao.getRoutePointsForTrip(tripId)

    suspend fun getRoutePointsList(tripId: Long): List<TripRoutePoint> =
        tripDao.getRoutePointsList(tripId)

    suspend fun deleteRoutePointsForTrip(tripId: Long) = tripDao.deleteRoutePointsForTrip(tripId)

    // Trip Expenses
    suspend fun insertTripExpense(expense: TripExpense): Long = tripDao.insertTripExpense(expense)

    suspend fun updateTripExpense(expense: TripExpense) = tripDao.updateTripExpense(expense)

    suspend fun deleteTripExpense(expense: TripExpense) = tripDao.deleteTripExpense(expense)

    fun getTripExpenses(tripId: Long): Flow<List<TripExpense>> = tripDao.getTripExpenses(tripId)

    suspend fun getTripExpensesList(tripId: Long): List<TripExpense> =
        tripDao.getTripExpensesList(tripId)

    // ==========================================
    // EXPENSE PERSISTENCE
    // ==========================================
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesBetween(startMillis: Long, endMillis: Long): Flow<List<Expense>> =
        expenseDao.getExpensesBetween(startMillis, endMillis)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)

    // ==========================================
    // SAVINGS ACCOUNT PERSISTENCE
    // ==========================================
    val allSavingsAccounts: Flow<List<SavingsAccount>> = savingsDao.getAllAccounts()
    val allSavingsTransactions: Flow<List<SavingsTransaction>> = savingsDao.getAllTransactions()

    fun getSavingsAccountByIdFlow(id: Long): Flow<SavingsAccount?> = savingsDao.getAccountByIdFlow(id)

    suspend fun getSavingsAccountById(id: Long): SavingsAccount? = savingsDao.getAccountById(id)

    suspend fun insertSavingsAccount(account: SavingsAccount): Long = savingsDao.insertAccount(account)

    suspend fun insertSavingsAccounts(accounts: List<SavingsAccount>) = savingsDao.insertAccounts(accounts)

    suspend fun updateSavingsAccount(account: SavingsAccount) = savingsDao.updateAccount(account)

    suspend fun deleteSavingsAccount(account: SavingsAccount) = savingsDao.deleteAccount(account)

    suspend fun deleteSavingsAccountById(id: Long) = savingsDao.deleteAccountById(id)

    fun getSavingsTransactionsForAccount(accountId: Long): Flow<List<SavingsTransaction>> =
        savingsDao.getTransactionsForAccount(accountId)

    suspend fun getSavingsTransactionsForAccountList(accountId: Long): List<SavingsTransaction> =
        savingsDao.getTransactionsForAccountList(accountId)

    suspend fun addSavingsTransaction(transaction: SavingsTransaction) {
        val account = savingsDao.getAccountById(transaction.accountId) ?: return
        val newBalance = if (transaction.type == "Deposit") {
            account.currentBalance + transaction.amount
        } else {
            account.currentBalance - transaction.amount
        }
        savingsDao.insertTransaction(transaction)
        savingsDao.updateAccount(account.copy(currentBalance = newBalance))
    }

    suspend fun deleteSavingsTransaction(transaction: SavingsTransaction) {
        val account = savingsDao.getAccountById(transaction.accountId)
        if (account != null) {
            val adjustedBalance = if (transaction.type == "Deposit") {
                account.currentBalance - transaction.amount
            } else {
                account.currentBalance + transaction.amount
            }
            savingsDao.updateAccount(account.copy(currentBalance = adjustedBalance))
        }
        savingsDao.deleteTransaction(transaction)
    }

    // ==========================================
    // CREDIT ACCOUNT PERSISTENCE
    // ==========================================
    val allCreditAccounts: Flow<List<CreditAccount>> = creditDao.getAllCreditAccounts()
    val allCreditPayments: Flow<List<CreditPayment>> = creditDao.getAllPayments()

    fun getCreditAccountByIdFlow(id: Long): Flow<CreditAccount?> = creditDao.getCreditAccountByIdFlow(id)

    suspend fun getCreditAccountById(id: Long): CreditAccount? = creditDao.getCreditAccountById(id)

    suspend fun insertCreditAccount(account: CreditAccount): Long = creditDao.insertCreditAccount(account)

    suspend fun insertCreditAccounts(accounts: List<CreditAccount>) = creditDao.insertCreditAccounts(accounts)

    suspend fun updateCreditAccount(account: CreditAccount) = creditDao.updateCreditAccount(account)

    suspend fun deleteCreditAccount(account: CreditAccount) = creditDao.deleteCreditAccount(account)

    suspend fun deleteCreditAccountById(id: Long) = creditDao.deleteCreditAccountById(id)

    fun getCreditPayments(creditId: Long): Flow<List<CreditPayment>> =
        creditDao.getPaymentsForCredit(creditId)

    suspend fun getCreditPaymentsList(creditId: Long): List<CreditPayment> =
        creditDao.getPaymentsForCreditList(creditId)

    suspend fun addCreditPayment(payment: CreditPayment): Long = creditDao.insertPayment(payment)

    suspend fun updateCreditPayment(payment: CreditPayment) = creditDao.updatePayment(payment)

    suspend fun deleteCreditPayment(payment: CreditPayment) = creditDao.deletePayment(payment)

    suspend fun deleteCreditPaymentById(id: Long) = creditDao.deletePaymentById(id)

    // ==========================================
    // MAINTENANCE & CLEANUP
    // ==========================================
    suspend fun clearAllData() {
        tripDao.clearAllTrips()
        expenseDao.clearAllExpenses()
        savingsDao.clearAllSavings()
        creditDao.clearAllCredit()
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val instance = AppRepository(db)
                INSTANCE = instance
                instance
            }
        }
    }
}
