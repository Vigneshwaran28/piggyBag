package com.titanbag.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

data class TransactionWithDetails(
    val id: Int,
    val amount: Double,
    val type: String, // income or expense
    val categoryId: Int,
    val accountId: Int,
    val note: String,
    val transactionDate: String,
    val createdAt: String,
    val updatedAt: String,
    val attachmentPath: String?,
    val tags: String,
    val categoryName: String,
    val categoryIcon: String,
    val categoryColor: String,
    val accountName: String,
    val accountIcon: String,
    val accountColor: String,
    val lifeAreaId: Int? = null,
    val subcategoryId: Int? = null,
    val purposeId: Int? = null,
    val paidBy: String? = null,
    val spentFor: String? = null,
    val peopleTagged: String? = null,
    val vehicleId: Int? = null,
    val odometer: Double? = null,
    val fuelQuantity: Double? = null,
    val studentName: String? = null,
    val lifeAreaName: String? = null,
    val lifeAreaIcon: String? = null,
    val lifeAreaColor: String? = null,
    val subcategoryName: String? = null,
    val purposeName: String? = null,
    val vehicleNickname: String? = null
)

@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY name ASC")
    fun getAccountsForUser(userId: String): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Int): Account?

    @Query("SELECT * FROM accounts WHERE name = :name LIMIT 1")
    suspend fun getAccountByName(name: String): Account?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Delete
    suspend fun deleteAccount(account: Account)

    @Query("SELECT COUNT(*) FROM accounts")
    suspend fun getAccountCount(): Int

    @Query("UPDATE accounts SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY orderIndex ASC, name ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY orderIndex ASC, name ASC")
    fun getCategoriesByType(type: String): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByName(name: String, type: String): Category?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Update
    suspend fun updateCategory(category: Category)

    @Update
    suspend fun updateCategories(categories: List<Category>)

    @Delete
    suspend fun deleteCategory(category: Category)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCategoryCount(): Int
}

@Dao
interface TransactionDao {
    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor, 
               a.name as accountName, a.icon as accountIcon, a.color as accountColor,
               la.name as lifeAreaName, la.icon as lifeAreaIcon, la.color as lifeAreaColor,
               sub.name as subcategoryName, p.name as purposeName, v.nickname as vehicleNickname
        FROM transactions t 
        INNER JOIN categories c ON t.categoryId = c.id 
        INNER JOIN accounts a ON t.accountId = a.id
        LEFT JOIN life_areas la ON t.lifeAreaId = la.id
        LEFT JOIN subcategories sub ON t.subcategoryId = sub.id
        LEFT JOIN purposes p ON t.purposeId = p.id
        LEFT JOIN vehicles v ON t.vehicleId = v.id
        ORDER BY t.transactionDate DESC
    """)
    fun getAllTransactions(): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor, 
               a.name as accountName, a.icon as accountIcon, a.color as accountColor,
               la.name as lifeAreaName, la.icon as lifeAreaIcon, la.color as lifeAreaColor,
               sub.name as subcategoryName, p.name as purposeName, v.nickname as vehicleNickname
        FROM transactions t 
        INNER JOIN categories c ON t.categoryId = c.id 
        INNER JOIN accounts a ON t.accountId = a.id 
        LEFT JOIN life_areas la ON t.lifeAreaId = la.id
        LEFT JOIN subcategories sub ON t.subcategoryId = sub.id
        LEFT JOIN purposes p ON t.purposeId = p.id
        LEFT JOIN vehicles v ON t.vehicleId = v.id
        WHERE t.userId = :userId
        ORDER BY t.transactionDate DESC
    """)
    fun getTransactionsForUser(userId: String): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor, 
               a.name as accountName, a.icon as accountIcon, a.color as accountColor,
               la.name as lifeAreaName, la.icon as lifeAreaIcon, la.color as lifeAreaColor,
               sub.name as subcategoryName, p.name as purposeName, v.nickname as vehicleNickname
        FROM transactions t 
        INNER JOIN categories c ON t.categoryId = c.id 
        INNER JOIN accounts a ON t.accountId = a.id 
        LEFT JOIN life_areas la ON t.lifeAreaId = la.id
        LEFT JOIN subcategories sub ON t.subcategoryId = sub.id
        LEFT JOIN purposes p ON t.purposeId = p.id
        LEFT JOIN vehicles v ON t.vehicleId = v.id
        WHERE t.accountId = :accountId
        ORDER BY t.transactionDate DESC
    """)
    fun getTransactionsForAccount(accountId: Int): Flow<List<TransactionWithDetails>>

    @Query("""
        SELECT t.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor, 
               a.name as accountName, a.icon as accountIcon, a.color as accountColor,
               la.name as lifeAreaName, la.icon as lifeAreaIcon, la.color as lifeAreaColor,
               sub.name as subcategoryName, p.name as purposeName, v.nickname as vehicleNickname
        FROM transactions t 
        INNER JOIN categories c ON t.categoryId = c.id 
        INNER JOIN accounts a ON t.accountId = a.id 
        LEFT JOIN life_areas la ON t.lifeAreaId = la.id
        LEFT JOIN subcategories sub ON t.subcategoryId = sub.id
        LEFT JOIN purposes p ON t.purposeId = p.id
        LEFT JOIN vehicles v ON t.vehicleId = v.id
        WHERE t.id = :id
    """)
    suspend fun getTransactionById(id: Int): TransactionWithDetails?

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getRawTransactionById(id: Int): Transaction?

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun getTransactionCountForCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM transactions WHERE accountId = :accountId")
    suspend fun getTransactionCountForAccount(accountId: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM transactions")
    suspend fun deleteAllTransactions()

    @Query("UPDATE transactions SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE userId = :userId")
    fun getBudgetsForUser(userId: String): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE month = :month AND year = :year")
    fun getBudgetsForPeriod(month: Int, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND month = :month AND year = :year LIMIT 1")
    suspend fun getBudgetForCategory(categoryId: Int?, month: Int, year: Int): Budget?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budgets")
    suspend fun deleteAllBudgets()

    @Query("UPDATE budgets SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY targetDate ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE userId = :userId ORDER BY targetDate ASC")
    fun getSavingsGoalsForUser(userId: String): Flow<List<SavingsGoal>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: Int): SavingsGoal?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(savingsGoal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(savingsGoal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(savingsGoal: SavingsGoal)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()

    @Query("UPDATE savings_goals SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE userId = :userId")
    fun getRecurringTransactionsForUser(userId: String): Flow<List<RecurringTransaction>>

    @Query("SELECT * FROM recurring_transactions WHERE enabled = 1")
    suspend fun getEnabledRecurringTransactions(): List<RecurringTransaction>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringTransactionById(id: Int): RecurringTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(recurringTransaction: RecurringTransaction): Long

    @Update
    suspend fun updateRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Delete
    suspend fun deleteRecurringTransaction(recurringTransaction: RecurringTransaction)

    @Query("DELETE FROM recurring_transactions")
    suspend fun deleteAllRecurringTransactions()

    @Query("UPDATE recurring_transactions SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserId(oldUserId: String, newUserId: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<Settings?>

    @Query("SELECT * FROM settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsDirect(): Settings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings): Long

    @Update
    suspend fun updateSettings(settings: Settings)
}

@Dao
interface LifeAreaDao {
    @Query("SELECT * FROM life_areas ORDER BY name ASC")
    fun getAllLifeAreas(): Flow<List<LifeArea>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifeArea(lifeArea: LifeArea): Long

    @Update
    suspend fun updateLifeArea(lifeArea: LifeArea)

    @Delete
    suspend fun deleteLifeArea(lifeArea: LifeArea)
}

@Dao
interface SubcategoryDao {
    @Query("SELECT * FROM subcategories ORDER BY name ASC")
    fun getAllSubcategories(): Flow<List<Subcategory>>

    @Query("SELECT * FROM subcategories WHERE categoryId = :catId ORDER BY name ASC")
    fun getSubcategoriesForCategory(catId: Int): Flow<List<Subcategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubcategory(sub: Subcategory): Long

    @Update
    suspend fun updateSubcategory(sub: Subcategory)

    @Delete
    suspend fun deleteSubcategory(sub: Subcategory)
}

@Dao
interface PurposeDao {
    @Query("SELECT * FROM purposes ORDER BY name ASC")
    fun getAllPurposes(): Flow<List<Purpose>>

    @Query("SELECT * FROM purposes WHERE subcategoryId = :subId ORDER BY name ASC")
    fun getPurposesForSubcategory(subId: Int): Flow<List<Purpose>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPurpose(purpose: Purpose): Long

    @Update
    suspend fun updatePurpose(purpose: Purpose)

    @Delete
    suspend fun deletePurpose(purpose: Purpose)
}

@Dao
interface VehicleDao {
    @Query("SELECT * FROM vehicles ORDER BY nickname ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getVehicleById(id: Int): Vehicle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVehicle(vehicle: Vehicle): Long

    @Update
    suspend fun updateVehicle(vehicle: Vehicle)

    @Delete
    suspend fun deleteVehicle(vehicle: Vehicle)
}

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY purchaseDate DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Int): Investment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY nextRenewalDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Int): Subscription?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE enabled = 1 ORDER BY dueDate ASC")
    fun getActiveRemindersDirect(): List<Reminder>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)
}

@Dao
interface GoldSilverPriceDao {
    @Query("SELECT * FROM gold_silver_prices WHERE date = :date LIMIT 1")
    suspend fun getPriceForDate(date: String): GoldSilverPrice?

    @Query("SELECT * FROM gold_silver_prices ORDER BY date DESC")
    fun getAllCachedPrices(): Flow<List<GoldSilverPrice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrice(price: GoldSilverPrice)
}

@Dao
interface AutoPayDao {
    @Query("SELECT * FROM autopays ORDER BY nextExecutionDate ASC")
    fun getAllAutoPays(): Flow<List<AutoPay>>

    @Query("SELECT * FROM autopays WHERE status = 'Active' ORDER BY nextExecutionDate ASC")
    suspend fun getActiveAutoPaysDirect(): List<AutoPay>

    @Query("SELECT * FROM autopays WHERE id = :id LIMIT 1")
    suspend fun getAutoPayById(id: Int): AutoPay?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAutoPay(autoPay: AutoPay): Long

    @Update
    suspend fun updateAutoPay(autoPay: AutoPay)

    @Delete
    suspend fun deleteAutoPay(autoPay: AutoPay)
}
