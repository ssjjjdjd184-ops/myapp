package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.SavingsAccount
import com.example.data.database.entity.SavingsTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsDao {
    @Query("SELECT * FROM savings_accounts ORDER BY id DESC")
    fun getAllAccounts(): Flow<List<SavingsAccount>>

    @Query("SELECT * FROM savings_accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): SavingsAccount?

    @Query("SELECT * FROM savings_accounts WHERE id = :id")
    fun getAccountByIdFlow(id: Long): Flow<SavingsAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: SavingsAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<SavingsAccount>)

    @Update
    suspend fun updateAccount(account: SavingsAccount)

    @Delete
    suspend fun deleteAccount(account: SavingsAccount)

    @Query("DELETE FROM savings_accounts WHERE id = :id")
    suspend fun deleteAccountById(id: Long)

    // Transactions
    @Query("SELECT * FROM savings_transactions WHERE accountId = :accountId ORDER BY dateMillis DESC")
    fun getTransactionsForAccount(accountId: Long): Flow<List<SavingsTransaction>>

    @Query("SELECT * FROM savings_transactions WHERE accountId = :accountId ORDER BY dateMillis DESC")
    suspend fun getTransactionsForAccountList(accountId: Long): List<SavingsTransaction>

    @Query("SELECT * FROM savings_transactions ORDER BY dateMillis DESC")
    fun getAllTransactions(): Flow<List<SavingsTransaction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: SavingsTransaction): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<SavingsTransaction>)

    @Update
    suspend fun updateTransaction(transaction: SavingsTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: SavingsTransaction)

    @Query("DELETE FROM savings_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("SELECT * FROM savings_accounts")
    suspend fun getAllAccountsSnapshot(): List<SavingsAccount>

    @Query("SELECT * FROM savings_transactions")
    suspend fun getAllTransactionsSnapshot(): List<SavingsTransaction>

    @Query("DELETE FROM savings_accounts")
    suspend fun clearAllSavings()
}
