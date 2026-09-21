package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.CreditAccount
import com.example.data.database.entity.CreditPayment
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.CreditRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CreditItemUiModel(
    val account: CreditAccount,
    val totalPaid: Double,
    val accruedInterest: Double,
    val totalDue: Double,
    val remainingBalance: Double,
    val status: String // ACTIVE, OVERDUE, PAID
)

class CreditViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val creditRepo = CreditRepository(database.creditDao())
    val prefManager = PreferenceManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("All") // "All", "ACTIVE", "OVERDUE", "PAID"
    val statusFilter: StateFlow<String> = _statusFilter.asStateFlow()

    private val _selectedAccount = MutableStateFlow<CreditAccount?>(null)
    val selectedAccount: StateFlow<CreditAccount?> = _selectedAccount.asStateFlow()

    private val _selectedAccountPayments = MutableStateFlow<List<CreditPayment>>(emptyList())
    val selectedAccountPayments: StateFlow<List<CreditPayment>> = _selectedAccountPayments.asStateFlow()

    val creditList: StateFlow<List<CreditItemUiModel>> = combine(
        creditRepo.allCreditAccounts,
        creditRepo.allPayments,
        _searchQuery,
        _statusFilter
    ) { accounts, payments, query, status ->
        val now = System.currentTimeMillis()
        accounts.map { acc ->
            val paid = payments.filter { it.creditId == acc.id }.sumOf { it.amount }
            val interest = acc.calculateAccruedInterest(now)
            val due = acc.principalAmount + interest
            val remaining = (due - paid).coerceAtLeast(0.0)
            val computedStatus = acc.getStatus(paid, now)

            CreditItemUiModel(
                account = acc,
                totalPaid = paid,
                accruedInterest = interest,
                totalDue = due,
                remainingBalance = remaining,
                status = computedStatus
            )
        }.filter { model ->
            val matchesQuery = query.isBlank() ||
                    model.account.personName.contains(query, ignoreCase = true) ||
                    model.account.contact.contains(query, ignoreCase = true) ||
                    model.account.notes.contains(query, ignoreCase = true)
            val matchesStatus = status == "All" || model.status.equals(status, ignoreCase = true)
            matchesQuery && matchesStatus
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalLent: StateFlow<Double> = creditRepo.allCreditAccounts.map { list ->
        list.sumOf { it.principalAmount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalCollected: StateFlow<Double> = creditRepo.allPayments.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(status: String) {
        _statusFilter.value = status
    }

    fun selectAccount(account: CreditAccount) {
        _selectedAccount.value = account
        viewModelScope.launch {
            _selectedAccountPayments.value = creditRepo.getPaymentsForCreditList(account.id)
        }
    }

    fun addCreditAccount(
        personName: String,
        contact: String,
        principalAmount: Double,
        interestRate: Double,
        interestType: String,
        interestFrequency: String,
        startDateMillis: Long,
        dueDateMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val account = CreditAccount(
                personName = personName,
                contact = contact,
                principalAmount = principalAmount,
                interestRate = interestRate,
                interestType = interestType,
                interestFrequency = interestFrequency,
                startDateMillis = startDateMillis,
                dueDateMillis = dueDateMillis,
                notes = notes
            )
            creditRepo.insertCreditAccount(account)
        }
    }

    fun updateCreditAccount(account: CreditAccount) {
        viewModelScope.launch {
            creditRepo.updateCreditAccount(account)
            if (_selectedAccount.value?.id == account.id) {
                _selectedAccount.value = account
            }
        }
    }

    fun deleteCreditAccount(account: CreditAccount) {
        viewModelScope.launch {
            creditRepo.deleteCreditAccount(account)
            if (_selectedAccount.value?.id == account.id) {
                _selectedAccount.value = null
                _selectedAccountPayments.value = emptyList()
            }
        }
    }

    fun addPayment(
        creditId: Long,
        amount: Double,
        dateMillis: Long,
        notes: String,
        onError: (String) -> Unit
    ) {
        if (amount <= 0) {
            onError("Payment amount must be greater than zero")
            return
        }

        viewModelScope.launch {
            val payment = CreditPayment(
                creditId = creditId,
                dateMillis = dateMillis,
                amount = amount,
                notes = notes
            )
            creditRepo.addPayment(payment)
            _selectedAccountPayments.value = creditRepo.getPaymentsForCreditList(creditId)
        }
    }

    fun deletePayment(payment: CreditPayment) {
        viewModelScope.launch {
            creditRepo.deletePayment(payment)
            _selectedAccountPayments.value = creditRepo.getPaymentsForCreditList(payment.creditId)
        }
    }
}
