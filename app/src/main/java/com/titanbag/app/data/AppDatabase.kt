package com.titanbag.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Account::class,
        Category::class,
        Transaction::class,
        Budget::class,
        SavingsGoal::class,
        RecurringTransaction::class,
        Settings::class,
        UserEntity::class,
        PartnerEntity::class,
        JournalEntity::class,
        SyncQueueEntity::class,
        BankTransaction::class,
        BankAccount::class,
        SharedJournalEntity::class,
        SharedJournalTransactionEntity::class,
        LocalUserProfile::class,
        PartnerConnection::class,
        Group::class,
        GroupMember::class,
        GroupExpense::class,
        DebtRecord::class,
        LifeArea::class,
        Subcategory::class,
        Purpose::class,
        Vehicle::class,
        Investment::class,
        Subscription::class,
        Reminder::class,
        GoldSilverPrice::class,
        GroupSettlement::class,
        AutoPay::class
    ],
    version = 29,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun settingsDao(): SettingsDao
    abstract fun cloudUserDao(): CloudUserDao
    abstract fun cloudPartnerDao(): CloudPartnerDao
    abstract fun cloudJournalDao(): CloudJournalDao
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun financeDao(): FinanceDao
    abstract fun sharedJournalDao(): SharedJournalDao
    abstract fun sharedJournalTransactionDao(): SharedJournalTransactionDao
    abstract fun localUserProfileDao(): LocalUserProfileDao
    abstract fun partnerConnectionDao(): PartnerConnectionDao
    abstract fun groupDao(): GroupDao
    abstract fun groupMemberDao(): GroupMemberDao
    abstract fun groupExpenseDao(): GroupExpenseDao
    abstract fun debtRecordDao(): DebtRecordDao
    abstract fun lifeAreaDao(): LifeAreaDao
    abstract fun subcategoryDao(): SubcategoryDao
    abstract fun purposeDao(): PurposeDao
    abstract fun vehicleDao(): VehicleDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun reminderDao(): ReminderDao
    abstract fun goldSilverPriceDao(): GoldSilverPriceDao
    abstract fun groupSettlementDao(): GroupSettlementDao
    abstract fun autoPayDao(): AutoPayDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                // Add the colorPalette column with a default value of 'Default'
                db.execSQL("ALTER TABLE settings ADD COLUMN colorPalette TEXT NOT NULL DEFAULT 'Default'")
            }
        }

        val MIGRATION_15_16 = object : androidx.room.migration.Migration(15, 16) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE budgets ADD COLUMN budgetType TEXT NOT NULL DEFAULT 'MONTHLY'")
                db.execSQL("ALTER TABLE budgets ADD COLUMN startDate INTEGER")
                db.execSQL("ALTER TABLE budgets ADD COLUMN endDate INTEGER")
                db.execSQL("DROP INDEX IF EXISTS index_budgets_categoryId_month_year")
                
                // Migrate existing budgets
                db.execSQL("""
                    UPDATE budgets 
                    SET budgetType = 'MONTHLY',
                        startDate = CAST(strftime('%s', printf('%04d-%02d-01 00:00:00', year, month)) AS INTEGER) * 1000,
                        endDate = CAST(strftime('%s', datetime(printf('%04d-%02d-01 00:00:00', year, month), '+1 month', '-1 second')) AS INTEGER) * 1000
                """.trimIndent())
            }
        }

        val MIGRATION_16_17 = object : androidx.room.migration.Migration(16, 17) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE budgets RENAME COLUMN periodType TO budgetType")
                } catch (e: Exception) {
                    try {
                        db.execSQL("ALTER TABLE budgets ADD COLUMN budgetType TEXT NOT NULL DEFAULT 'MONTHLY'")
                    } catch (e2: Exception) {
                        // Already exists
                    }
                }
                try {
                    db.execSQL("ALTER TABLE budgets ADD COLUMN startDate INTEGER")
                } catch (e: Exception) {}
                try {
                    db.execSQL("ALTER TABLE budgets ADD COLUMN endDate INTEGER")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_17_18 = object : androidx.room.migration.Migration(17, 18) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE budgets ADD COLUMN budgetName TEXT")
                } catch (e: Exception) {}
            }
        }

        val MIGRATION_28_29 = object : androidx.room.migration.Migration(28, 29) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                try {
                    db.execSQL("ALTER TABLE settings ADD COLUMN viewMode TEXT NOT NULL DEFAULT 'Classic'")
                } catch (e: Exception) {}
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "titanbag_database"
                )
                .addCallback(DatabaseCallback())
                .addMigrations(MIGRATION_4_5, MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_28_29)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedDatabase(db)
            }

            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                seedDatabase(db)
            }

            private fun seedDatabase(db: SupportSQLiteDatabase) {
                try {
                    // Seed Life Areas
                    val lifeAreas = listOf(
                        "Personal" to ("person" to "#FF9800"),
                        "Family" to ("people" to "#E91E63"),
                        "Home" to ("home" to "#3F51B5"),
                        "Office" to ("work" to "#009688"),
                        "Business" to ("storefront" to "#4CAF50"),
                        "Friends" to ("group" to "#00BCD4"),
                        "Relationship" to ("favorite" to "#FF5722"),
                        "Children" to ("child_care" to "#9C27B0"),
                        "Parents" to ("elderly" to "#FFEB3B"),
                        "Investment" to ("trending_up" to "#4CAF50"),
                        "Vehicle" to ("directions_car" to "#607D8B"),
                        "Education" to ("school" to "#2196F3"),
                        "Medical" to ("medical_services" to "#F44336"),
                        "Pets" to ("pets" to "#795548"),
                        "Travel" to ("flight" to "#03A9F4"),
                        "Emergency" to ("report_problem" to "#FF5722"),
                        "Charity" to ("favorite" to "#E91E63")
                    )
                    lifeAreas.forEach { (name, info) ->
                        db.execSQL("INSERT OR IGNORE INTO life_areas (name, icon, color) VALUES ('$name', '${info.first}', '${info.second}')")
                    }

                    // Seed Categories (Income) - Editable/Deletable by setting isDefault to 0
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (1, 'Salary', 'income', 'payments', '#BFFCC6', 0, 1)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (2, 'Business', 'income', 'storefront', '#CAFFBF', 0, 2)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (3, 'Investments', 'income', 'trending_up', '#9BF6FF', 0, 3)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (4, 'Gifts', 'income', 'card_giftcard', '#FFFFBA', 0, 4)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (5, 'Freelance', 'income', 'computer', '#FFCAD4', 0, 5)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (6, 'Rental', 'income', 'home', '#A0C4FF', 0, 6)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (7, 'Refunds', 'income', 'receipt_long', '#D8B4FE', 0, 7)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (8, 'Dividends', 'income', 'account_balance', '#B9FBC0', 0, 8)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (9, 'Bonus', 'income', 'star', '#FBF8CC', 0, 9)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (10, 'Other', 'income', 'more_horiz', '#E8AEB2', 0, 10)")

                    // Seed Categories (Expense) - Editable/Deletable by setting isDefault to 0
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (11, 'Food & Dining', 'expense', 'restaurant', '#FFDFBA', 0, 1)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (12, 'Shopping', 'expense', 'shopping_bag', '#FFB3BA', 0, 2)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (13, 'Transport', 'expense', 'directions_car', '#BDB2FF', 0, 3)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (14, 'Bills & Utilities', 'expense', 'receipt_long', '#D8B4FE', 0, 4)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (15, 'Health & Fitness', 'expense', 'medical_services', '#FFCAD4', 0, 5)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (16, 'Entertainment', 'expense', 'sports_esports', '#A0C4FF', 0, 6)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (17, 'Groceries', 'expense', 'shopping_cart', '#D0F4DE', 0, 7)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (18, 'Rent & Housing', 'expense', 'home', '#FCE1E4', 0, 8)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (19, 'Education', 'expense', 'school', '#E2F0D9', 0, 9)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (20, 'Insurance & Taxes', 'expense', 'shield', '#FCF6BD', 0, 10)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (21, 'Electronics', 'expense', 'devices', '#A9DEF9', 0, 11)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (22, 'Subscriptions', 'expense', 'subscriptions', '#E4C1F9', 0, 12)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (23, 'Travel', 'expense', 'flight', '#D0F4EA', 0, 13)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (24, 'Personal Care', 'expense', 'spa', '#FFD6A5', 0, 14)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (25, 'Alcohol', 'expense', 'local_bar', '#FFFACD', 0, 15)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (26, 'Apparel', 'expense', 'checkroom', '#FAFAD2', 0, 16)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (27, 'Appliances', 'expense', 'kitchen', '#FFEFD5', 0, 17)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (28, 'Art', 'expense', 'palette', '#FFE4B5', 0, 18)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (29, 'Auto Maintenance', 'expense', 'build', '#FFDAB9', 0, 19)")
                    db.execSQL("INSERT OR IGNORE INTO categories (id, name, type, icon, color, isDefault, orderIndex) VALUES (30, 'Other', 'expense', 'more_horiz', '#E8AEB2', 0, 20)")

                    // Seed Subcategories & Purposes
                    // Food & Dining (Category ID: 11)
                    val foodSubcats = listOf("Breakfast", "Lunch", "Dinner", "Snacks", "Tea", "Coffee", "Bakery", "Restaurant", "Street Food", "Fast Food", "Swiggy", "Zomato", "Birthday Cake")
                    foodSubcats.forEachIndexed { idx, name ->
                        val subId = 100 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 11, '$name')")
                    }

                    // Transport (Category ID: 13)
                    val transSubcats = listOf("Fuel", "Maintenance", "Insurance", "Road Tax", "Pollution", "Accessories", "Car Wash", "Parking", "Toll", "Fine")
                    transSubcats.forEachIndexed { idx, name ->
                        val subId = 200 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 13, '$name')")
                        if (name == "Fuel") {
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Petrol')")
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Diesel')")
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'EV Charging')")
                        } else if (name == "Maintenance") {
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Tyre')")
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Oil')")
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Engine Repair')")
                            db.execSQL("INSERT OR IGNORE INTO purposes (subcategoryId, name) VALUES ($subId, 'Service')")
                        }
                    }

                    // Groceries (Category ID: 17)
                    val grocerySubcats = listOf("Vegetables", "Rice", "Oil", "Milk")
                    grocerySubcats.forEachIndexed { idx, name ->
                        val subId = 300 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 17, '$name')")
                    }

                    // Bills & Utilities (Category ID: 14)
                    val billSubcats = listOf("Electricity", "Water", "Internet", "Gas Cylinder")
                    billSubcats.forEachIndexed { idx, name ->
                        val subId = 400 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 14, '$name')")
                    }

                    // Health & Fitness (Category ID: 15)
                    val healthSubcats = listOf("Doctor", "Hospital", "Medicine", "Lab Test", "Health Insurance", "Dental", "Eye Care", "Emergency", "Surgery", "Vaccination")
                    healthSubcats.forEachIndexed { idx, name ->
                        val subId = 500 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 15, '$name')")
                    }

                    // Education (Category ID: 19)
                    val eduSubcats = listOf("School Fees", "College Fees", "Books", "Uniform", "Hostel", "Exam Fees", "Tuition", "Coaching", "Online Course", "Laptop", "Stationery", "Transport", "Scholarship")
                    eduSubcats.forEachIndexed { idx, name ->
                        val subId = 600 + idx
                        db.execSQL("INSERT OR IGNORE INTO subcategories (id, categoryId, name) VALUES ($subId, 19, '$name')")
                    }

                    // Seed Default Accounts
                    db.execSQL("INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color) VALUES (1, 'Cash', 'Cash', 0.0, 0.0, 'wallet', '#FFD6A5')")
                    db.execSQL("INSERT INTO accounts (id, name, type, openingBalance, currentBalance, icon, color) VALUES (2, 'Bank Account', 'Bank Account', 0.0, 0.0, 'account_balance', '#A0C4FF')")

                    // Seed Settings
                    db.execSQL("INSERT OR IGNORE INTO settings (id, themeMode, currency, pinEnabled, biometricEnabled, notificationsEnabled, colorPalette, godUserRoleEnabled, viewMode) VALUES (1, 'system', '₹', 0, 0, 1, 'Default', 0, 'Classic')")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
