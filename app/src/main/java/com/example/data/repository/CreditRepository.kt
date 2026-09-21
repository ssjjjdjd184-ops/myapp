package com.example.data.repository

import com.example.data.database.dao.CreditDao
import com.example.data.database.entity.CreditAccount
import com.example.data.database.entity.CreditPayment
import kotlinx.coroutines.flow.Flow

class CreditRepository(private val creditDao: CreditDao) {
    val allCreditAccounts: Flow<List<CreditAccount>> = creditDao.getAllCreditAccounts()
    val allPayments: Flow<List<CreditPayment>> = creditDao.getAllPayments()

    fun getCreditAccountByIdFlow(id: Long): Flow<CreditAccount?> = creditDao.getCreditAccountByIdFlow(id)

    suspend fun getCreditAccountById(id: Long): CreditAccount? = creditDao.getCreditAccountById(id)

    suspend fun insertCreditAccount(account: CreditAccount): Long = creditDao.insertCreditAccount(account)

    suspend fun updateCreditAccount(account: CreditAccount) = creditDao.updateCreditAccount(account)

    suspend fun deleteCreditAccount(account: CreditAccount) = creditDao.deleteCreditAccount(account)

    suspend fun deleteCreditAccountById(id: Long) = creditDao.deleteCreditAccountById(id)

    fun getPaymentsForCredit(creditId: Long): Flow<List<CreditPayment>> =
        creditDao.getPaymentsForCredit(creditId)

    suspend fun getPaymentsForCreditList(creditId: Long): List<CreditPayment> =
        creditDao.getPaymentsForCreditList(creditId)

    suspend fun addPayment(payment: CreditPayment): Long = creditDao.insertPayment(payment)

    suspend fun updatePayment(payment: CreditPayment) = creditDao.updatePayment(payment)

    suspend fun deletePayment(payment: CreditPayment) = creditDao.deletePayment(payment)

    suspend fun deletePaymentById(id: Long) = creditDao.deletePaymentById(id)
}
