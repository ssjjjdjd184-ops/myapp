package com.example.data.database.dao

import androidx.room.*
import com.example.data.database.entity.CreditAccount
import com.example.data.database.entity.CreditPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditDao {
    @Query("SELECT * FROM credit_accounts ORDER BY id DESC")
    fun getAllCreditAccounts(): Flow<List<CreditAccount>>

    @Query("SELECT * FROM credit_accounts WHERE id = :id")
    suspend fun getCreditAccountById(id: Long): CreditAccount?

    @Query("SELECT * FROM credit_accounts WHERE id = :id")
    fun getCreditAccountByIdFlow(id: Long): Flow<CreditAccount?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditAccount(account: CreditAccount): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCreditAccounts(accounts: List<CreditAccount>)

    @Update
    suspend fun updateCreditAccount(account: CreditAccount)

    @Delete
    suspend fun deleteCreditAccount(account: CreditAccount)

    @Query("DELETE FROM credit_accounts WHERE id = :id")
    suspend fun deleteCreditAccountById(id: Long)

    // Payments
    @Query("SELECT * FROM credit_payments WHERE creditId = :creditId ORDER BY dateMillis DESC")
    fun getPaymentsForCredit(creditId: Long): Flow<List<CreditPayment>>

    @Query("SELECT * FROM credit_payments WHERE creditId = :creditId ORDER BY dateMillis DESC")
    suspend fun getPaymentsForCreditList(creditId: Long): List<CreditPayment>

    @Query("SELECT * FROM credit_payments ORDER BY dateMillis DESC")
    fun getAllPayments(): Flow<List<CreditPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: CreditPayment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<CreditPayment>)

    @Update
    suspend fun updatePayment(payment: CreditPayment)

    @Delete
    suspend fun deletePayment(payment: CreditPayment)

    @Query("DELETE FROM credit_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Long)

    @Query("SELECT * FROM credit_accounts")
    suspend fun getAllCreditAccountsSnapshot(): List<CreditAccount>

    @Query("SELECT * FROM credit_payments")
    suspend fun getAllPaymentsSnapshot(): List<CreditPayment>

    @Query("DELETE FROM credit_accounts")
    suspend fun clearAllCredit()
}
