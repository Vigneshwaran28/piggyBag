package com.titanbag.app.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(private val db: AppDatabase) {
    private val financeDao = db.financeDao()

    val allBankTransactionsFlow: Flow<List<BankTransaction>> = financeDao.getAllBankTransactionsFlow()
    val allBankAccountsFlow: Flow<List<BankAccount>> = financeDao.getAllBankAccountsFlow()

    suspend fun processIncomingMessage(message: String) {
        val extracted = TransactionExtractor.extract(message) ?: return
        
        financeDao.insertBankTransaction(extracted)
        
        // Update or Create Bank Account
        val accountKey = "${extracted.bankName}_${extracted.accountLastFour}"
        val existingAccount = financeDao.getBankAccount(accountKey)
        
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        if (existingAccount == null) {
            val initialBalance = extracted.balance ?: if (extracted.type == "CREDIT") extracted.amount else 0.0
            financeDao.insertBankAccount(
                BankAccount(
                    accountNumber = accountKey,
                    bankName = extracted.bankName,
                    accountLastFour = extracted.accountLastFour,
                    currentBalance = initialBalance,
                    lastSyncTime = timestamp
                )
            )
        } else {
            var newBalance = existingAccount.currentBalance
            if (extracted.balance != null) {
                newBalance = extracted.balance
            } else {
                if (extracted.type == "CREDIT") {
                    newBalance += extracted.amount
                } else {
                    newBalance -= extracted.amount
                }
            }
            financeDao.updateBankAccountBalance(accountKey, newBalance, timestamp)
        }
    }
}
