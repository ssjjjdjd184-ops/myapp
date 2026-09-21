package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.*
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.CreditRepository
import com.example.data.repository.ExpenseRepository
import com.example.data.repository.SavingsRepository
import com.example.data.repository.TripRepository
import kotlinx.coroutines.flow.*
import java.util.Calendar

data class TripReportData(
    val tripsCount: Int = 0,
    val totalDistanceMeters: Double = 0.0,
    val bikeDistanceMeters: Double = 0.0,
    val walkingDistanceMeters: Double = 0.0,
    val totalDurationSeconds: Long = 0L,
    val avgSpeedKmh: Double = 0.0,
    val maxSpeedKmh: Double = 0.0,
    val tripExpenses: Double = 0.0,
    val avgCostPerKm: Double = 0.0
)

data class ExpenseReportData(
    val totalExpenses: Double = 0.0,
    val categoryTotals: Map<String, Double> = emptyMap(),
    val dailyTotals: Map<String, Double> = emptyMap()
)

data class SavingsReportData(
    val totalSavings: Double = 0.0,
    val deposits: Double = 0.0,
    val withdrawals: Double = 0.0,
    val interestEarned: Double = 0.0,
    val targetProgress: Float = 0f
)

data class CreditReportData(
    val moneyLent: Double = 0.0,
    val paymentsReceived: Double = 0.0,
    val outstanding: Double = 0.0,
    val interest: Double = 0.0,
    val overdue: Double = 0.0
)

data class ReportsUiState(
    val tripReport: TripReportData = TripReportData(),
    val expenseReport: ExpenseReportData = ExpenseReportData(),
    val savingsReport: SavingsReportData = SavingsReportData(),
    val creditReport: CreditReportData = CreditReportData()
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val tripRepo = TripRepository(database.tripDao())
    private val expenseRepo = ExpenseRepository(database.expenseDao())
    private val savingsRepo = SavingsRepository(database.savingsDao())
    private val creditRepo = CreditRepository(database.creditDao())
    val prefManager = PreferenceManager.getInstance(application)

    private val _selectedTimeFilter = MutableStateFlow("This month")
    val selectedTimeFilter: StateFlow<String> = _selectedTimeFilter.asStateFlow()

    private val flowGroup1 = combine(tripRepo.allTrips, expenseRepo.allExpenses, savingsRepo.allAccounts) { trips, expenses, savings ->
        Triple(trips, expenses, savings)
    }

    private val flowGroup2 = combine(savingsRepo.allTransactions, creditRepo.allCreditAccounts, creditRepo.allPayments, _selectedTimeFilter) { txs, credits, payments, filter ->
        ReportsDataGroup(txs, credits, payments, filter)
    }

    val uiState: StateFlow<ReportsUiState> = combine(flowGroup1, flowGroup2) { g1, g2 ->
        buildReport(g1.first, g1.second, g1.third, g2.txs, g2.credits, g2.payments, g2.filter)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun setTimeFilter(filter: String) {
        _selectedTimeFilter.value = filter
    }

    private fun buildReport(
        trips: List<Trip>,
        expenses: List<Expense>,
        savingsAccounts: List<SavingsAccount>,
        savingsTxs: List<SavingsTransaction>,
        credits: List<CreditAccount>,
        creditPayments: List<CreditPayment>,
        filter: String
    ): ReportsUiState {
        val (startMillis, endMillis) = getDateRange(filter)

        val filteredTrips = trips.filter { it.startTimeMillis in startMillis..endMillis }
        val filteredExpenses = expenses.filter { it.dateMillis in startMillis..endMillis }
        val filteredSavingsTxs = savingsTxs.filter { it.dateMillis in startMillis..endMillis }
        val filteredCreditPayments = creditPayments.filter { it.dateMillis in startMillis..endMillis }

        // Trip report
        val tripDist = filteredTrips.sumOf { it.totalGpsDistanceMeters }
        val bikeDist = filteredTrips.sumOf { it.bikeDistanceMeters }
        val walkDist = filteredTrips.sumOf { it.walkingDistanceMeters }
        val duration = filteredTrips.sumOf { it.durationSeconds }
        val speeds = filteredTrips.map { it.avgSpeedKmh }.filter { it > 0.1 }
        val avgSpeed = if (speeds.isNotEmpty()) speeds.average() else 0.0
        val maxSpeed = filteredTrips.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
        val tripCost = filteredTrips.sumOf { it.totalTripExpense }
        val distKm = tripDist / 1000.0
        val costPerKm = if (distKm > 0.01) tripCost / distKm else 0.0

        val tripData = TripReportData(
            tripsCount = filteredTrips.size,
            totalDistanceMeters = tripDist,
            bikeDistanceMeters = bikeDist,
            walkingDistanceMeters = walkDist,
            totalDurationSeconds = duration,
            avgSpeedKmh = avgSpeed,
            maxSpeedKmh = maxSpeed,
            tripExpenses = tripCost,
            avgCostPerKm = costPerKm
        )

        // Expense report
        val totalExp = filteredExpenses.sumOf { it.amount }
        val catMap = filteredExpenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
        val dayMap = filteredExpenses.groupBy { sdf.format(java.util.Date(it.dateMillis)) }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        val expData = ExpenseReportData(
            totalExpenses = totalExp,
            categoryTotals = catMap,
            dailyTotals = dayMap
        )

        // Savings report
        val totalSavings = savingsAccounts.sumOf { it.currentBalance }
        val deposits = filteredSavingsTxs.filter { it.type == "Deposit" }.sumOf { it.amount }
        val withdrawals = filteredSavingsTxs.filter { it.type == "Withdrawal" }.sumOf { it.amount }
        val interest = savingsAccounts.sumOf { it.calculateInterest(System.currentTimeMillis()) }
        val totalTarget = savingsAccounts.sumOf { it.targetAmount }
        val progress = if (totalTarget > 0) ((totalSavings / totalTarget) * 100).toFloat().coerceIn(0f, 100f) else 100f

        val savData = SavingsReportData(
            totalSavings = totalSavings,
            deposits = deposits,
            withdrawals = withdrawals,
            interestEarned = interest,
            targetProgress = progress
        )

        // Credit report
        val now = System.currentTimeMillis()
        val totalLent = credits.sumOf { it.principalAmount }
        val totalPaid = filteredCreditPayments.sumOf { it.amount }
        var totalOutstanding = 0.0
        var totalInterest = 0.0
        var totalOverdue = 0.0

        for (c in credits) {
            val paidAll = creditPayments.filter { it.creditId == c.id }.sumOf { it.amount }
            val accInterest = c.calculateAccruedInterest(now)
            val due = c.principalAmount + accInterest
            val remaining = (due - paidAll).coerceAtLeast(0.0)

            totalOutstanding += remaining
            totalInterest += accInterest
            if (now > c.dueDateMillis && remaining > 0.05) {
                totalOverdue += remaining
            }
        }

        val credData = CreditReportData(
            moneyLent = totalLent,
            paymentsReceived = totalPaid,
            outstanding = totalOutstanding,
            interest = totalInterest,
            overdue = totalOverdue
        )

        return ReportsUiState(
            tripReport = tripData,
            expenseReport = expData,
            savingsReport = savData,
            creditReport = credData
        )
    }

    private fun getDateRange(filter: String): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val end = System.currentTimeMillis()

        when (filter) {
            "Today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return Pair(cal.timeInMillis, end)
            }
            "Yesterday" -> {
                cal.add(Calendar.DAY_OF_YEAR, -1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                return Pair(start, cal.timeInMillis)
            }
            "This week" -> {
                cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return Pair(cal.timeInMillis, end)
            }
            "This month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return Pair(cal.timeInMillis, end)
            }
            "Last month" -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                val start = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                return Pair(start, cal.timeInMillis)
            }
            else -> { // All time
                return Pair(0L, end + 86400000L)
            }
        }
    }
}

private data class ReportsDataGroup(
    val txs: List<SavingsTransaction>,
    val credits: List<CreditAccount>,
    val payments: List<CreditPayment>,
    val filter: String
)

