package com.titanbag.app.data

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object TransactionExtractor {

    private val amountPattern = Pattern.compile("(?i)(?:rs|inr|₹)\\.?\\s*([\\d,]+\\.?\\d*)")
    private val accountPattern = Pattern.compile("(?i)(?:a/c|acc|account)\\s*(?:no\\.?\\s*)?(?:[x*]*(\\d{4}))")
    private val balancePattern = Pattern.compile("(?i)(?:bal|balance|avail bal|available balance)\\s*(?:is)?\\s*(?:rs|inr|₹)\\.?\\s*([\\d,]+\\.?\\d*)")
    
    private val banks = listOf("HDFC", "SBI", "ICICI", "AXIS", "KOTAK", "PNB", "BOB", "CANARA")

    fun extract(message: String): BankTransaction? {
        val amountMatcher = amountPattern.matcher(message)
        if (!amountMatcher.find()) return null
        
        val amount = amountMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        
        val type = if (message.contains("credited", ignoreCase = true) || message.contains("received", ignoreCase = true)) {
            "CREDIT"
        } else if (message.contains("debited", ignoreCase = true) || message.contains("spent", ignoreCase = true) || message.contains("paid", ignoreCase = true)) {
            "DEBIT"
        } else {
            return null // Not a clear financial transaction
        }

        val accMatcher = accountPattern.matcher(message)
        val accLastFour = if (accMatcher.find()) accMatcher.group(1) ?: "0000" else "0000"
        
        val bankName = banks.find { message.contains(it, ignoreCase = true) } ?: "Other Bank"
        
        val balMatcher = balancePattern.matcher(message)
        val balance = if (balMatcher.find()) balMatcher.group(1)?.replace(",", "")?.toDoubleOrNull() else null

        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        // Create a unique ID using hash of message and date to prevent duplicates
        val externalId = (message.hashCode().toString() + Date().time.toString()).takeLast(16)

        return BankTransaction(
            externalId = externalId,
            bankName = bankName,
            accountLastFour = accLastFour,
            amount = amount,
            type = type,
            date = isoDate,
            description = message.take(100),
            balance = balance,
            sourceMessage = message
        )
    }
}
