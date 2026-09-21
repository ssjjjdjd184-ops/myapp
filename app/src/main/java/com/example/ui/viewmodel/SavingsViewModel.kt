package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.SavingsAccount
import com.example.data.database.entity.SavingsTransaction
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.SavingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SavingsViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val savingsRepo = SavingsRepository(database.savingsDao())
    val prefManager = PreferenceManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedAccount = MutableStateFlow<SavingsAccount?>(null)
    val selectedAccount: StateFlow<SavingsAccount?> = _selectedAccount.asStateFlow()

    private val _selectedAccountTransactions = MutableStateFlow<List<SavingsTransaction>>(emptyList())
    val selectedAccountTransactions: StateFlow<List<SavingsTransaction>> = _selectedAccountTransactions.asStateFlow()

    val accountsList: StateFlow<List<SavingsAccount>> = combine(
        savingsRepo.allAccounts,
        _searchQuery
    ) { accounts, query ->
        if (query.isBlank()) {
            accounts
        } else {
            accounts.filter { it.name.contains(query, ignoreCase = true) || it.notes.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSavings: StateFlow<Double> = savingsRepo.allAccounts.map { list ->
        list.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalInterestEarned: StateFlow<Double> = savingsRepo.allAccounts.map { list ->
        val now = System.currentTimeMillis()
        list.sumOf { it.calculateInterest(now) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectAccount(account: SavingsAccount) {
        _selectedAccount.value = account
        viewModelScope.launch {
            _selectedAccountTransactions.value = savingsRepo.getTransactionsForAccountList(account.id)
        }
    }

    fun addAccount(
        name: String,
        startingAmount: Double,
        targetAmount: Double,
        interestRate: Double,
        interestType: String,
        interestFrequency: String,
        startDateMillis: Long,
        maturityDateMillis: Long?,
        allowOverdraft: Boolean,
        notes: String
    ) {
        viewModelScope.launch {
            val account = SavingsAccount(
                name = name,
                startingAmount = startingAmount,
                currentBalance = startingAmount,
                targetAmount = targetAmount,
                interestRate = interestRate,
                interestType = interestType,
                interestFrequency = interestFrequency,
                startDateMillis = startDateMillis,
                maturityDateMillis = maturityDateMillis,
                allowOverdraft = allowOverdraft,
                notes = notes
            )
            val id = savingsRepo.insertAccount(account)
            if (startingAmount > 0) {
                savingsRepo.addTransaction(
                    SavingsTransaction(
                        accountId = id,
                        dateMillis = startDateMillis,
                        amount = startingAmount,
                        type = "Deposit",
                        notes = "Initial balance"
                    )
                )
            }
        }
    }

    fun updateAccount(account: SavingsAccount) {
        viewModelScope.launch {
            savingsRepo.updateAccount(account)
            if (_selectedAccount.value?.id == account.id) {
                _selectedAccount.value = account
            }
        }
    }

    fun deleteAccount(account: SavingsAccount) {
        viewModelScope.launch {
            savingsRepo.deleteAccount(account)
            if (_selectedAccount.value?.id == account.id) {
                _selectedAccount.value = null
                _selectedAccountTransactions.value = emptyList()
            }
        }
    }

    fun addTransaction(
        account: SavingsAccount,
        amount: Double,
        type: String,
        dateMillis: Long,
        notes: String,
        onError: (String) -> Unit
    ) {
        if (amount <= 0) {
            onError("Amount must be greater than zero")
            return
        }

        if (type == "Withdrawal" && !account.allowOverdraft && amount > account.currentBalance) {
            onError("Withdrawal exceeds current balance of ${prefManager.formatMoney(account.currentBalance)}")
            return
        }

        viewModelScope.launch {
            val tx = SavingsTransaction(
                accountId = account.id,
                dateMillis = dateMillis,
                amount = amount,
                type = type,
                notes = notes
            )
            savingsRepo.addTransaction(tx)
            val updated = savingsRepo.getAccountById(account.id)
            _selectedAccount.value = updated
            if (updated != null) {
                _selectedAccountTransactions.value = savingsRepo.getTransactionsForAccountList(updated.id)
            }
        }
    }

    fun deleteTransaction(transaction: SavingsTransaction) {
        viewModelScope.launch {
            savingsRepo.deleteTransaction(transaction)
            val updated = savingsRepo.getAccountById(transaction.accountId)
            _selectedAccount.value = updated
            if (updated != null) {
                _selectedAccountTransactions.value = savingsRepo.getTransactionsForAccountList(updated.id)
            }
        }
    }
}
