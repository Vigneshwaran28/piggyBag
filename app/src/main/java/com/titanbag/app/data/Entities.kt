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
    val userId: String = "default_user",
    // Smart Life Finance System additions
    val lifeAreaId: Int? = null,
    val subcategoryId: Int? = null,
    val purposeId: Int? = null,
    val paidBy: String? = null,
    val spentFor: String? = null,
    val peopleTagged: String? = null,
    val vehicleId: Int? = null,
    val odometer: Double? = null,
    val fuelQuantity: Double? = null,
    val studentName: String? = null
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
    val userId: String = "default_user",
    val endConditionType: String = "never", // never, date, count
    val endConditionValue: String = "" // YYYY-MM-DD or numeric count string
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
    val debtListEnabled: Boolean = false,
    val colorPalette: String = "Default",
    val customColor: String? = null,
    val customIconColor: String? = null,
    val customBgColor: String? = null,
    val godUserRoleEnabled: Boolean = false,
    val bottomTabs: String = "Home,Analytics,Budgets,Accounts,More",
    val viewMode: String = "Classic"
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

@Entity(tableName = "life_areas")
data class LifeArea(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val icon: String,
    val color: String
)

@Entity(
    tableName = "subcategories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Subcategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val icon: String = "",
    val color: String = ""
)

@Entity(
    tableName = "purposes",
    foreignKeys = [
        ForeignKey(
            entity = Subcategory::class,
            parentColumns = ["id"],
            childColumns = ["subcategoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("subcategoryId")]
)
data class Purpose(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subcategoryId: Int,
    val name: String
)

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val registrationNumber: String,
    val nickname: String,
    val type: String, // Bike, Scooter, Car, SUV, Van, Bus, Truck, Lorry, Tractor, EV, Cycle
    val fuelType: String, // Petrol, Diesel, EV, CNG
    val purchaseDate: String,
    val insuranceExpiryDate: String?,
    val pollutionExpiryDate: String?,
    val roadTaxExpiryDate: String?,
    val lastServiceDate: String?,
    val lastOdometer: Double,
    val userId: String = "default_user"
)

@Entity(tableName = "investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Gold, Silver, Stocks, Mutual Funds, ETF, FD, RD, PPF, NPS, EPF, Crypto, Land, House, Business, Bonds, Digital Gold
    val purchaseDate: String,
    val purchasePrice: Double,
    val quantity: Double, // represents weight in grams for Gold/Silver
    val broker: String?,
    val transactionCharges: Double = 0.0,
    val currentPrice: Double = 0.0, // fetched automatically or manually
    val notes: String? = null,
    val currentStatus: String = "Active", // Active, Sold
    val userId: String = "default_user"
)

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val billingCycle: String, // Monthly, Yearly, Custom
    val startDate: String,
    val nextRenewalDate: String,
    val accountId: Int?, // optional linked account ID for auto-debit
    val status: String = "Active", // Active, Inactive
    val userId: String = "default_user"
)

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val type: String, // Medicine, Insurance, Vehicle Service, Subscription, Rent, EMI, School Fees, Birthday, Anniversary, Property Tax, Loan Due, Investment SIP
    val dueDate: String, // YYYY-MM-DD
    val amount: Double? = null,
    val recurrence: String = "None", // None, Daily, Weekly, Monthly, Yearly
    val enabled: Boolean = true,
    val userId: String = "default_user"
)

@Entity(tableName = "gold_silver_prices")
data class GoldSilverPrice(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val goldPrice: Double, // Price per gram
    val silverPrice: Double, // Price per gram
    val fetchedAt: String // ISO timestamp
)

@Entity(
    tableName = "autopays",
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
        Index("categoryId"),
        Index("accountId")
    ]
)
data class AutoPay(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val categoryId: Int,
    val amount: Double,
    val accountId: Int,
    val frequency: String, // Daily, Weekly, Monthly, Every 2 Months, Every 3 Months, Quarterly, Half Yearly, Yearly, Custom
    val customIntervalDays: Int? = null,
    val startDate: String, // YYYY-MM-DD
    val endDate: String? = null,
    val reminderDaysBefore: Int = 0,
    val automaticEntryEnabled: Boolean = true,
    val notes: String = "",
    val lastExecutedDate: String? = null,
    val nextExecutionDate: String, // YYYY-MM-DD
    val status: String = "Active" // Active, Executed, Suspended
)
