package com.titanbag.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts"
)
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Cash, Bank Account, UPI, Credit Card, Debit Card, Wallet, Other
    val openingBalance: Double,
    val currentBalance: Double,
    val icon: String, // String identifier for icon, e.g. "wallet", "credit_card"
    val color: String, // Hex color, e.g. "#FF5722"
    val userId: String = "default_user"
)

@Entity(
    tableName = "categories"
)
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // income or expense
    val icon: String, // String identifier for icon, e.g. "food", "salary"
    val color: String, // Hex color, e.g. "#4CAF50"
    val isDefault: Boolean = false,
    val orderIndex: Int = 0
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index("transactionDate"),
        Index("categoryId"),
        Index("accountId")
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // income or expense
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val transactionDate: String, // ISO-8601 string (e.g. "2026-06-25T09:20:00")
    val createdAt: String,
    val updatedAt: String,
    val attachmentPath: String? = null, // Optional attachment path
    val tags: String = "", // Comma-separated custom tags
    val userId: String = "default_user"
)

@Entity(
    tableName = "budgets"
)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int?, // Nullable for overall budget
    val budgetAmount: Double,
    val month: Int, // 1-12
    val year: Int,
    val budgetType: String = "MONTHLY", // "MONTHLY", "WEEKLY", "CUSTOM"
    val startDate: Long? = null,
    val endDate: Long? = null,
    val budgetName: String? = null,
    val userId: String = "default_user"
)

@Entity(
    tableName = "savings_goals"
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDate: String, // ISO-8601 date string (e.g. "2026-12-31")
    val status: String, // active or completed
    val icon: String,
    val color: String,
    val userId: String = "default_user"
)

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("nextExecutionDate"),
        Index("categoryId"),
        Index("accountId")
    ]
)
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // income or expense
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val frequency: String, // daily, weekly, monthly, yearly
    val nextExecutionDate: String, // ISO-8601 date string
    val enabled: Boolean = true,
    val userId: String = "default_user"
)

@Entity(
    tableName = "settings"
)
data class Settings(
    @PrimaryKey val id: Int = 1, // Single row constraint (always 1)
    val themeMode: String = "system", // system, light, dark
    val currency: String = "₹", // currency symbol or code: $, ₹, €, £, etc.
    val pinEnabled: Boolean = false,
    val biometricEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val debtListEnabled: Boolean = true,
    val colorPalette: String = "Default",
    val customColor: String? = null,
    val customIconColor: String? = null,
    val customBgColor: String? = null
)

@Entity(
    tableName = "bank_transactions",
    indices = [Index("externalId", unique = true)]
)
data class BankTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val externalId: String, // Unique ID from message or hash
    val bankName: String,
    val accountLastFour: String,
    val amount: Double,
    val type: String, // CREDIT or DEBIT
    val date: String, // ISO-8601
    val description: String,
    val balance: Double? = null,
    val sourceMessage: String,
    val isCategorized: Boolean = false
)

@Entity(
    tableName = "bank_accounts"
)
data class BankAccount(
    @PrimaryKey val accountNumber: String, // Masked or last 4 + bank name
    val bankName: String,
    val accountLastFour: String,
    val currentBalance: Double,
    val lastSyncTime: String
)

@Entity(
    tableName = "debts"
)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val personName: String,
    val amount: Double,
    val date: Long, // Borrowed/Lent date timestamp
    val action: String, // "Debt" (Borrowed) or "Credit" (Lent)
    val reminderEnabled: Boolean,
    val reminderDateTime: Long?, // Timestamp for reminder alert
    val returnedDate: Long? = null, // Date when settled
    val status: String, // "Pending", "Completed"
    val note: String? = null,
    val userId: String = "default_user"
)
