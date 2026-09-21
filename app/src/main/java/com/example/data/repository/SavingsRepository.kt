package com.example.data.repository

import com.example.data.database.dao.SavingsDao
import com.example.data.database.entity.SavingsAccount
import com.example.data.database.entity.SavingsTransaction
import kotlinx.coroutines.flow.Flow

class SavingsRepository(private val savingsDao: SavingsDao) {
    val allAccounts: Flow<List<SavingsAccount>> = savingsDao.getAllAccounts()
    val allTransactions: Flow<List<SavingsTransaction>> = savingsDao.getAllTransactions()

    fun getAccountByIdFlow(id: Long): Flow<SavingsAccount?> = savingsDao.getAccountByIdFlow(id)

    suspend fun getAccountById(id: Long): SavingsAccount? = savingsDao.getAccountById(id)

    suspend fun insertAccount(account: SavingsAccount): Long = savingsDao.insertAccount(account)

    suspend fun updateAccount(account: SavingsAccount) = savingsDao.updateAccount(account)

    suspend fun deleteAccount(account: SavingsAccount) = savingsDao.deleteAccount(account)

    suspend fun deleteAccountById(id: Long) = savingsDao.deleteAccountById(id)

    fun getTransactionsForAccount(accountId: Long): Flow<List<SavingsTransaction>> =
        savingsDao.getTransactionsForAccount(accountId)

    suspend fun getTransactionsForAccountList(accountId: Long): List<SavingsTransaction> =
        savingsDao.getTransactionsForAccountList(accountId)

    suspend fun addTransaction(transaction: SavingsTransaction) {
        val account = savingsDao.getAccountById(transaction.accountId) ?: return
        val newBalance = if (transaction.type == "Deposit") {
            account.currentBalance + transaction.amount
        } else {
            account.currentBalance - transaction.amount
        }
        savingsDao.insertTransaction(transaction)
        savingsDao.updateAccount(account.copy(currentBalance = newBalance))
    }

    suspend fun deleteTransaction(transaction: SavingsTransaction) {
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
}
