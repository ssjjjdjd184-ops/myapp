package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.entity.Expense
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class ExpensesViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val expenseRepo = ExpenseRepository(database.expenseDao())
    val prefManager = PreferenceManager.getInstance(application)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _selectedDateFilter = MutableStateFlow("This month") // "All", "Today", "This week", "This month"
    val selectedDateFilter: StateFlow<String> = _selectedDateFilter.asStateFlow()

    val filteredExpenses: StateFlow<List<Expense>> = combine(
        expenseRepo.allExpenses,
        _searchQuery,
        _selectedCategory,
        _selectedDateFilter
    ) { expenses, query, category, dateFilter ->
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val weekStart = calendar.timeInMillis

        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = calendar.timeInMillis

        expenses.filter { exp ->
            val matchesQuery = query.isBlank() ||
                    exp.description.contains(query, ignoreCase = true) ||
                    exp.notes.contains(query, ignoreCase = true) ||
                    exp.category.contains(query, ignoreCase = true)

            val matchesCategory = category == "All" || exp.category.equals(category, ignoreCase = true)

            val matchesDate = when (dateFilter) {
                "Today" -> exp.dateMillis >= todayStart
                "This week" -> exp.dateMillis >= weekStart
                "This month" -> exp.dateMillis >= monthStart
                else -> true
            }

            matchesQuery && matchesCategory && matchesDate
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalAmount: StateFlow<Double> = filteredExpenses.map { list ->
        list.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(cat: String) {
        _selectedCategory.value = cat
    }

    fun setSelectedDateFilter(filter: String) {
        _selectedDateFilter.value = filter
    }

    fun addExpense(
        category: String,
        description: String,
        amount: Double,
        paymentMethod: String,
        dateMillis: Long,
        notes: String
    ) {
        viewModelScope.launch {
            val expense = Expense(
                category = category,
                description = description,
                amount = amount,
                paymentMethod = paymentMethod,
                dateMillis = dateMillis,
                notes = notes
            )
            expenseRepo.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepo.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepo.deleteExpense(expense)
        }
    }
}
