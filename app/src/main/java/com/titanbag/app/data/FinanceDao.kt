package com.titanbag.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM bank_transactions ORDER BY date DESC")
    fun getAllBankTransactionsFlow(): Flow<List<BankTransaction>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBankTransaction(transaction: BankTransaction)

    @Query("SELECT * FROM bank_accounts")
    fun getAllBankAccountsFlow(): Flow<List<BankAccount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBankAccount(account: BankAccount)

    @Query("SELECT * FROM bank_accounts WHERE accountNumber = :accountNumber")
    suspend fun getBankAccount(accountNumber: String): BankAccount?

    @Query("UPDATE bank_accounts SET currentBalance = :balance, lastSyncTime = :syncTime WHERE accountNumber = :accountNumber")
    suspend fun updateBankAccountBalance(accountNumber: String, balance: Double, syncTime: String)
}
