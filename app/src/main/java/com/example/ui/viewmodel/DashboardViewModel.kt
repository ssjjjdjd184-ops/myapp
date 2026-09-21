package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.CreditAccount
import com.example.data.database.entity.Expense
import com.example.data.database.entity.SavingsAccount
import com.example.data.database.entity.Trip
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.CreditRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SavingsRepository
import com.example.data.repository.TripRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class DashboardUiState(
    // Trips
    val todayTripsCount: Int = 0,
    val todayTotalDistanceMeters: Double = 0.0,
    val todayBikeDistanceMeters: Double = 0.0,
    val todayWalkingDistanceMeters: Double = 0.0,
    val todayTripExpenses: Double = 0.0,
    val recentTrips: List<Trip> = emptyList(),

    // Expenses
    val todayExpensesTotal: Double = 0.0,
    val monthExpensesTotal: Double = 0.0,
    val recentExpenses: List<Expense> = emptyList(),

    // Savings
    val currentSavingsTotal: Double = 0.0,
    val totalDeposits: Double = 0.0,
    val totalWithdrawals: Double = 0.0,
    val interestEarned: Double = 0.0,
    val targetProgressPercent: Float = 0f,
    val savingsGoals: List<SavingsAccount> = emptyList(),

    // Credit
    val totalMoneyLent: Double = 0.0,
    val totalPaymentsReceived: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val interestDue: Double = 0.0,
    val overdueAmount: Double = 0.0,
    val upcomingCreditDue: List<CreditAccount> = emptyList()
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val tripRepo = TripRepository(database.tripDao())
    private val expenseRepo = ExpenseRepository(database.expenseDao())
    private val savingsRepo = SavingsRepository(database.savingsDao())
    private val creditRepo = CreditRepository(database.creditDao())
    val prefManager = PreferenceManager.getInstance(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        val flowPart1 = combine(tripRepo.allTrips, expenseRepo.allExpenses, savingsRepo.allAccounts) { trips, expenses, savings ->
            Triple(trips, expenses, savings)
        }
        val flowPart2 = combine(savingsRepo.allTransactions, creditRepo.allCreditAccounts, creditRepo.allPayments) { txs, credits, payments ->
            Triple(txs, credits, payments)
        }

        combine(flowPart1, flowPart2) { p1, p2 ->
            calculateDashboard(p1.first, p1.second, p1.third, p2.first, p2.second, p2.third)
        }.onEach { state ->
            _uiState.value = state
        }.launchIn(viewModelScope)
    }

    private fun calculateDashboard(
        trips: List<Trip>,
        expenses: List<Expense>,
        savingsAccounts: List<SavingsAccount>,
        savingsTxs: List<com.example.data.database.entity.SavingsTransaction>,
        credits: List<CreditAccount>,
        creditPayments: List<com.example.data.database.entity.CreditPayment>
    ): DashboardUiState {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // Today start/end
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStartMillis = calendar.timeInMillis

        // Month start
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStartMillis = calendar.timeInMillis

        // Trip calculations
        val todayTrips = trips.filter { it.startTimeMillis >= todayStartMillis }
        val todayDistance = todayTrips.sumOf { it.totalGpsDistanceMeters }
        val todayBikeDist = todayTrips.sumOf { it.bikeDistanceMeters }
        val todayWalkDist = todayTrips.sumOf { it.walkingDistanceMeters }
        val todayTripExpenses = todayTrips.sumOf { it.totalTripExpense }

        // General Expenses calculations
        val todayExpenses = expenses.filter { it.dateMillis >= todayStartMillis }.sumOf { it.amount }
        val monthExpenses = expenses.filter { it.dateMillis >= monthStartMillis }.sumOf { it.amount }

        // Savings calculations
        val totalSavingsBalance = savingsAccounts.sumOf { it.currentBalance }
        val totalDeposits = savingsTxs.filter { it.type == "Deposit" }.sumOf { it.amount }
        val totalWithdrawals = savingsTxs.filter { it.type == "Withdrawal" }.sumOf { it.amount }
        val totalInterest = savingsAccounts.sumOf { it.calculateInterest(now) }
        val totalTarget = savingsAccounts.sumOf { it.targetAmount }
        val progress = if (totalTarget > 0) ((totalSavingsBalance / totalTarget) * 100).toFloat().coerceIn(0f, 100f) else 100f

        // Credit calculations
        val totalLent = credits.sumOf { it.principalAmount }
        val totalPaid = creditPayments.sumOf { it.amount }
        var outstanding = 0.0
        var totalInterestDue = 0.0
        var overdue = 0.0

        for (credit in credits) {
            val paymentsForThis = creditPayments.filter { it.creditId == credit.id }.sumOf { it.amount }
            val accruedInterest = credit.calculateAccruedInterest(now)
            val totalDue = credit.principalAmount + accruedInterest
            val remaining = (totalDue - paymentsForThis).coerceAtLeast(0.0)

            outstanding += remaining
            totalInterestDue += accruedInterest
            if (now > credit.dueDateMillis && remaining > 0.05) {
                overdue += remaining
            }
        }

        val upcomingCredits = credits.filter { it.dueDateMillis >= now }
            .sortedBy { it.dueDateMillis }
            .take(3)

        return DashboardUiState(
            todayTripsCount = todayTrips.size,
            todayTotalDistanceMeters = todayDistance,
            todayBikeDistanceMeters = todayBikeDist,
            todayWalkingDistanceMeters = todayWalkDist,
            todayTripExpenses = todayTripExpenses,
            recentTrips = trips.take(4),

            todayExpensesTotal = todayExpenses,
            monthExpensesTotal = monthExpenses,
            recentExpenses = expenses.take(5),

            currentSavingsTotal = totalSavingsBalance,
            totalDeposits = totalDeposits,
            totalWithdrawals = totalWithdrawals,
            interestEarned = totalInterest,
            targetProgressPercent = progress,
            savingsGoals = savingsAccounts.take(4),

            totalMoneyLent = totalLent,
            totalPaymentsReceived = totalPaid,
            outstandingBalance = outstanding,
            interestDue = totalInterestDue,
            overdueAmount = overdue,
            upcomingCreditDue = upcomingCredits
        )
    }
}
