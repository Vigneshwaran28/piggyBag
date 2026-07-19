package com.titanbag.app.data

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

data class NotificationEvent(
    val title: String,
    val message: String,
    val destination: String? = null
)

class TitanBagViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = Repository(db)
    private val financeRepository = FinanceRepository(db)

    // --- SHARED PREFERENCES FOR PIN ---
    private val prefs = application.getSharedPreferences("titanbag_prefs", Context.MODE_PRIVATE)

    // --- CURRENT LOCAL USER STATE ---
    val currentUserId = MutableStateFlow("default_user")

    private val sessionManager = SessionManager(application)

    // --- FONT STYLE STATE ---
    private val _fontStyle = MutableStateFlow(sessionManager.getFontStyle())
    val fontStyle = _fontStyle.asStateFlow()

    fun updateFontStyle(style: String) {
        sessionManager.setFontStyle(style)
        _fontStyle.value = style
    }

    // --- VISUAL STYLE STATE ---
    private val _visualStyle = MutableStateFlow(sessionManager.getVisualStyle())
    val visualStyle = _visualStyle.asStateFlow()

    fun updateVisualStyle(style: String) {
        sessionManager.setVisualStyle(style)
        _visualStyle.value = style
    }

    // --- PDF EXPORT OPTIONS STATE ---
    val pdfDateRange = MutableStateFlow(sessionManager.getPdfDateRange())
    val pdfCustomStartDate = MutableStateFlow(sessionManager.getPdfCustomStartDate())
    val pdfCustomEndDate = MutableStateFlow(sessionManager.getPdfCustomEndDate())
    val pdfDateFormat = MutableStateFlow(sessionManager.getPdfDateFormat())
    val pdfTimeFormat = MutableStateFlow(sessionManager.getPdfTimeFormat())
    
    val pdfIncludeNotes = MutableStateFlow(sessionManager.getPdfIncludeNotes())
    val pdfIncludeCategories = MutableStateFlow(sessionManager.getPdfIncludeCategories())
    val pdfIncludeAccount = MutableStateFlow(sessionManager.getPdfIncludeAccount())
    val pdfIncludeRunningBalance = MutableStateFlow(sessionManager.getPdfIncludeRunningBalance())
    val pdfIncludeSummary = MutableStateFlow(sessionManager.getPdfIncludeSummary())
    val pdfIncludeTransactionIds = MutableStateFlow(sessionManager.getPdfIncludeTransactionIds())

    fun updatePdfExportOptions(
        range: String,
        start: Long,
        end: Long,
        dateFormat: String,
        timeFormat: String,
        incNotes: Boolean,
        incCats: Boolean,
        incAcc: Boolean,
        incBalance: Boolean,
        incSummary: Boolean,
        incIds: Boolean
    ) {
        sessionManager.setPdfDateRange(range)
        sessionManager.setPdfCustomStartDate(start)
        sessionManager.setPdfCustomEndDate(end)
        sessionManager.setPdfDateFormat(dateFormat)
        sessionManager.setPdfTimeFormat(timeFormat)
        sessionManager.setPdfIncludeNotes(incNotes)
        sessionManager.setPdfIncludeCategories(incCats)
        sessionManager.setPdfIncludeAccount(incAcc)
        sessionManager.setPdfIncludeRunningBalance(incBalance)
        sessionManager.setPdfIncludeSummary(incSummary)
        sessionManager.setPdfIncludeTransactionIds(incIds)

        pdfDateRange.value = range
        pdfCustomStartDate.value = start
        pdfCustomEndDate.value = end
        pdfDateFormat.value = dateFormat
        pdfTimeFormat.value = timeFormat
        pdfIncludeNotes.value = incNotes
        pdfIncludeCategories.value = incCats
        pdfIncludeAccount.value = incAcc
        pdfIncludeRunningBalance.value = incBalance
        pdfIncludeSummary.value = incSummary
        pdfIncludeTransactionIds.value = incIds
    }

    // --- CUSTOM COLOR STATES ---
    private val _customColorPrimary = MutableStateFlow(sessionManager.getCustomColorPrimary())
    val customColorPrimary = _customColorPrimary.asStateFlow()

    private val _customColorSecondary = MutableStateFlow(sessionManager.getCustomColorSecondary())
    val customColorSecondary = _customColorSecondary.asStateFlow()

    private val _customColorBackground = MutableStateFlow(sessionManager.getCustomColorBackground())
    val customColorBackground = _customColorBackground.asStateFlow()

    fun updateCustomColors(primary: String?, secondary: String?, background: String?) {
        sessionManager.setCustomColorPrimary(primary)
        sessionManager.setCustomColorSecondary(secondary)
        sessionManager.setCustomColorBackground(background)
        _customColorPrimary.value = primary
        _customColorSecondary.value = secondary
        _customColorBackground.value = background
    }

    // --- NAVIGATION SharedFlow ---
    private val _navigationCommand = MutableSharedFlow<String>()
    val navigationCommand = _navigationCommand.asSharedFlow()

    fun navigateTo(destination: String) {
        viewModelScope.launch {
            _navigationCommand.emit(destination)
        }
    }

    // Bank related flows
    val allBankTransactions = financeRepository.allBankTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    val allBankAccounts = financeRepository.allBankAccountsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalBankBalance = allBankAccounts.map { accounts ->
        accounts.sumOf { it.currentBalance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val bankFinancialSummary = allBankTransactions.map { transactions ->
        val credits = transactions.filter { it.type == "CREDIT" }.sumOf { it.amount }
        val debits = transactions.filter { it.type == "DEBIT" }.sumOf { it.amount }
        Triple(credits, debits, transactions.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Triple(0.0, 0.0, 0))

    fun processMessage(message: String) {
        viewModelScope.launch(Dispatchers.IO) {
            financeRepository.processIncomingMessage(message)
        }
    }

    // --- CUSTOM ACCOUNT TYPES STATE ---
    private val _accountTypes = MutableStateFlow<List<String>>(emptyList())
    val accountTypes = _accountTypes.asStateFlow()

    // --- SEARCH HISTORY STATE ---
    private val _searchHistory = MutableStateFlow<List<String>>(emptyList())
    val searchHistory = _searchHistory.asStateFlow()

    fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        val current = _searchHistory.value.toMutableList()
        val trimmed = query.trim()
        current.remove(trimmed)
        current.add(0, trimmed)
        val limited = current.take(10)
        _searchHistory.value = limited
        prefs.edit().putString("search_history", limited.joinToString(",")).apply()
    }

    fun removeSearchQuery(query: String) {
        val current = _searchHistory.value.toMutableList()
        current.remove(query)
        _searchHistory.value = current
        prefs.edit().putString("search_history", current.joinToString(",")).apply()
    }

    fun clearSearchHistory() {
        _searchHistory.value = emptyList()
        prefs.edit().remove("search_history").apply()
    }

    fun addAccountType(newType: String) {
        val current = _accountTypes.value.toMutableSet()
        current.add(newType)
        prefs.edit().putStringSet("custom_account_types", current).apply()
        _accountTypes.value = current.toList().sorted()
    }

    fun editAccountType(oldType: String, newType: String) {
        val current = _accountTypes.value.toMutableSet()
        if (current.contains(oldType)) {
            current.remove(oldType)
            current.add(newType)
            prefs.edit().putStringSet("custom_account_types", current).apply()
            _accountTypes.value = current.toList().sorted()
            
            // Update existing accounts of oldType to newType in database
            viewModelScope.launch {
                val accounts = allAccounts.value
                accounts.forEach { acc ->
                    if (acc.type == oldType) {
                        repository.updateAccount(acc.copy(type = newType))
                    }
                }
            }
        }
    }

    // --- NAVIGATION STATE ---
    private val _currentTab = MutableStateFlow(0) // 0: Records, 1: Analysis, 2: Accounts, 3: More
    val currentTab = _currentTab.asStateFlow()

    fun selectTab(index: Int) {
        _currentTab.value = index
    }

    // --- SCROLL STATE ---
    private val _isScrolling = MutableStateFlow(false)
    val isScrolling = _isScrolling.asStateFlow()

    fun setScrolling(scrolling: Boolean) {
        _isScrolling.value = scrolling
    }
    
    // --- FILE PROCESSING STATE ---
    private val _isProcessingFile = MutableStateFlow(false)
    val isProcessingFile = _isProcessingFile.asStateFlow()

    private val _fileProcessingMessage = MutableStateFlow("")
    val fileProcessingMessage = _fileProcessingMessage.asStateFlow()

    fun setFileProcessing(processing: Boolean, message: String = "") {
        _isProcessingFile.value = processing
        _fileProcessingMessage.value = message
    }
    
    var isRecordsInitialLoadDone = false

    // --- PIN & LOCK STATE ---
    private val _isLocked = MutableStateFlow(false)
    val isLocked = _isLocked.asStateFlow()

    private val _isPinSet = MutableStateFlow(false)
    val isPinSet = _isPinSet.asStateFlow()

    private val saltKey: String by lazy {
        val existing = prefs?.getString("secure_salt", null)
        if (existing != null) return@lazy existing
        val newSalt = UUID.randomUUID().toString()
        prefs?.edit()?.putString("secure_salt", newSalt)?.apply()
        newSalt
    }

    init {
        val pinHash = prefs.getString("pin_hash", null)
        _isPinSet.value = !pinHash.isNullOrEmpty()
        _isLocked.value = !pinHash.isNullOrEmpty()

        // Load custom account types
        val storedTypes = prefs.getStringSet("custom_account_types", null)
        if (storedTypes == null) {
            val defaults = setOf("Cash", "Bank Account", "UPI", "Credit Card", "Debit Card", "Wallet", "Other")
            prefs.edit().putStringSet("custom_account_types", defaults).apply()
            _accountTypes.value = defaults.toList().sorted()
        } else {
            _accountTypes.value = storedTypes.toList().sorted()
        }
        
        // Load search history
        val historyStr = prefs.getString("search_history", "")
        if (!historyStr.isNullOrEmpty()) {
            _searchHistory.value = historyStr.split(",").filter { it.isNotBlank() }
        }

        // Process recurring transactions on view model initialization
        processRecurringTransactions()
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var cats = repository.allCategories.first()
                if (cats.any { it.name == "Food & Dining" } || cats.any { it.name == "Income" }) {
                    cats.forEach { repository.categoryDao.deleteCategory(it) }
                    cats = emptyList()
                }

                if (cats.isEmpty()) {
                    val defaultCats = listOf(
                        Category(name = "Bills", type = "expense", icon = "receipt_long", color = "#FFDFBA", isDefault = false, orderIndex = 1),
                        Category(name = "Car", type = "expense", icon = "directions_car", color = "#FFB3BA", isDefault = false, orderIndex = 2),
                        Category(name = "Clothing", type = "expense", icon = "checkroom", color = "#BDB2FF", isDefault = false, orderIndex = 3),
                        Category(name = "Education", type = "expense", icon = "school", color = "#D8B4FE", isDefault = false, orderIndex = 4),
                        Category(name = "Electronics", type = "expense", icon = "devices", color = "#FFCAD4", isDefault = false, orderIndex = 5),
                        Category(name = "Entertainment", type = "expense", icon = "sports_esports", color = "#A0C4FF", isDefault = false, orderIndex = 6),
                        Category(name = "Food", type = "expense", icon = "restaurant", color = "#D0F4DE", isDefault = false, orderIndex = 7),
                        Category(name = "Health", type = "expense", icon = "medical_services", color = "#FCE1E4", isDefault = false, orderIndex = 8),
                        Category(name = "Home", type = "expense", icon = "home", color = "#E2F0D9", isDefault = false, orderIndex = 9),
                        Category(name = "Insurance", type = "expense", icon = "shield", color = "#FCF6BD", isDefault = false, orderIndex = 10),
                        Category(name = "Shopping", type = "expense", icon = "shopping_bag", color = "#A9DEF9", isDefault = false, orderIndex = 11),
                        Category(name = "Social", type = "expense", icon = "groups", color = "#E4C1F9", isDefault = false, orderIndex = 12),
                        Category(name = "Tax", type = "expense", icon = "account_balance", color = "#D0F4EA", isDefault = false, orderIndex = 13),
                        Category(name = "Mobile", type = "expense", icon = "phone_android", color = "#FFD6A5", isDefault = false, orderIndex = 14),
                        Category(name = "Transportation", type = "expense", icon = "directions_bus", color = "#FFFACD", isDefault = false, orderIndex = 15),
                        Category(name = "Others", type = "expense", icon = "more_horiz", color = "#FAFAD2", isDefault = false, orderIndex = 16),
                        Category(name = "Wifi & Internet", type = "expense", icon = "wifi", color = "#FFEFD5", isDefault = false, orderIndex = 17),
                        Category(name = "Gym", type = "expense", icon = "fitness_center", color = "#FFE4B5", isDefault = false, orderIndex = 18),
                        
                        Category(name = "Others", type = "income", icon = "more_horiz", color = "#BFFCC6", isDefault = false, orderIndex = 19),
                        Category(name = "Awards", type = "income", icon = "emoji_events", color = "#CAFFBF", isDefault = false, orderIndex = 20),
                        Category(name = "Coupons", type = "income", icon = "local_offer", color = "#9BF6FF", isDefault = false, orderIndex = 21),
                        Category(name = "Grants", type = "income", icon = "school", color = "#FFFFBA", isDefault = false, orderIndex = 22),
                        Category(name = "Lottery", type = "income", icon = "casino", color = "#FFCAD4", isDefault = false, orderIndex = 23),
                        Category(name = "Refunds", type = "income", icon = "receipt_long", color = "#A0C4FF", isDefault = false, orderIndex = 24),
                        Category(name = "Rental", type = "income", icon = "home", color = "#D8B4FE", isDefault = false, orderIndex = 25),
                        Category(name = "Salary", type = "income", icon = "payments", color = "#B9FBC0", isDefault = false, orderIndex = 26),
                        Category(name = "Sold Items", type = "income", icon = "storefront", color = "#FBF8CC", isDefault = false, orderIndex = 27)
                    )
                    defaultCats.forEach { repository.categoryDao.insertCategory(it) }

                    repository.settingsDao.insertSettings(Settings(id = 1, themeMode = "system", currency = "₹", pinEnabled = false, biometricEnabled = false, notificationsEnabled = true, colorPalette = "Default"))
                }
                
                // Seed Default Accounts if there are no accounts
                val accounts = repository.allAccounts.first()
                if (accounts.isEmpty()) {
                    repository.accountDao.insertAccount(
                        Account(name = "Cash", type = "Cash", openingBalance = 0.0, currentBalance = 0.0, icon = "wallet", color = "#FFD6A5")
                    )
                    repository.accountDao.insertAccount(
                        Account(name = "Bank Account", type = "Bank Account", openingBalance = 0.0, currentBalance = 0.0, icon = "account_balance", color = "#A0C4FF")
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val savedUserId = prefs.getString("active_local_user_id", "default_user") ?: "default_user"
        currentUserId.value = savedUserId

        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val profiles = repository.getLocalUserProfilesDirect()
                if (profiles.isEmpty()) {
                    seedLocalUsersData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        checkDebtReminders()
        triggerGoldSilverPricesUpdate()
        checkScheduledReminders()
        executeDueAutoPays()
    }

    fun setPin(pin: String) {
        if (pin.length in 4..6) {
            val hash = sha256(pin, saltKey)
            prefs.edit().putString("pin_hash", hash).apply()
            _isPinSet.value = true
            _isLocked.value = false // unlocked upon setting
        }
    }

    fun disablePin() {
        prefs.edit().remove("pin_hash").apply()
        _isPinSet.value = false
        _isLocked.value = false
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return true
        val checkHash = sha256(pin, saltKey)
        val success = (storedHash == checkHash)
        if (success) {
            _isLocked.value = false
        }
        return success
    }

    fun unlockApp() {
        _isLocked.value = false
    }

    fun lockApp() {
        if (_isPinSet.value || (settings.value?.biometricEnabled ?: false)) {
            _isLocked.value = true
        }
    }

    private fun sha256(input: String, salt: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest((input + salt).toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    // --- DATA STREAM DECLARATIONS ---
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allAccounts = currentUserId.flatMapLatest { userId ->
        repository.getAccountsForUser(userId)
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allCategories = repository.allCategories
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allTransactions = currentUserId.flatMapLatest { userId ->
        repository.getTransactionsForUser(userId)
    }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allBudgets = currentUserId.flatMapLatest { userId ->
        repository.getBudgetsForUser(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSavingsGoals = currentUserId.flatMapLatest { userId ->
        repository.getSavingsGoalsForUser(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allRecurringTransactions = currentUserId.flatMapLatest { userId ->
        repository.getRecurringTransactionsForUser(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun getInitialSettings(): Settings {
        val themeMode = prefs.getString("theme_mode", "system") ?: "system"
        val colorPalette = prefs.getString("color_palette", "Default") ?: "Default"
        return Settings(
            themeMode = themeMode,
            colorPalette = colorPalette
        )
    }

    val settings = repository.appSettings
        .onEach { settingsObj ->
            if (settingsObj != null) {
                prefs.edit()
                    .putString("theme_mode", settingsObj.themeMode)
                    .putString("color_palette", settingsObj.colorPalette)
                    .apply()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = getInitialSettings()
        )

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allVehicles = currentUserId.flatMapLatest { userId ->
        repository.allVehicles
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allInvestments = currentUserId.flatMapLatest { userId ->
        repository.allInvestments
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allSubscriptions = currentUserId.flatMapLatest { userId ->
        repository.allSubscriptions
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allReminders = currentUserId.flatMapLatest { userId ->
        repository.allReminders
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLifeAreas = repository.allLifeAreas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allSubcategories = repository.allSubcategories.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allPurposes = repository.allPurposes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allAutoPays = repository.allAutoPays.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val insights = combine(allTransactions, allInvestments, allReminders, settings) { txs, invs, rems, setts ->
        SmartInsightsEngine.generateInsights(txs, invs, rems, setts?.currency ?: "₹")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- SEARCH AND FILTER MUTABLES ---
    val searchQuery = MutableStateFlow("")
    val filterType = MutableStateFlow("All") // All, Income, Expense
    val filterCategoryIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterAccountIds = MutableStateFlow<Set<Int>>(emptySet())
    val filterDateRange = MutableStateFlow<Pair<Long?, Long?>?>(null) // Epoch milliseconds
    val filterTags = MutableStateFlow<Set<String>>(emptySet())
    
    val selectedHomeDateFilter = MutableStateFlow(prefs.getString("home_date_filter_option", "Monthly") ?: "Monthly")

    fun setHomeDateFilterOption(option: String) {
        selectedHomeDateFilter.value = option
        prefs.edit().putString("home_date_filter_option", option).apply()
    }

    val allTags: StateFlow<Set<String>> = allTransactions.map { txList ->
        txList.flatMap { tx ->
            tx.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }.toSet()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptySet()
    )

    fun toggleFilterTag(tag: String) {
        val current = filterTags.value
        filterTags.value = if (current.contains(tag)) current - tag else current + tag
    }

    fun clearFilterTags() {
        filterTags.value = emptySet()
    }

    // REACIVE FILTERED TRANSACTIONS
    val filteredTransactions: StateFlow<List<TransactionWithDetails>> = combine(
        allTransactions,
        searchQuery.debounce(250),
        filterType,
        filterCategoryIds,
        filterAccountIds,
        filterDateRange,
        filterTags
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val txs = flows[0] as List<TransactionWithDetails>
        val query = flows[1] as String
        val type = flows[2] as String
        @Suppress("UNCHECKED_CAST")
        val catIds = flows[3] as Set<Int>
        @Suppress("UNCHECKED_CAST")
        val accIds = flows[4] as Set<Int>
        @Suppress("UNCHECKED_CAST")
        val dateRange = flows[5] as Pair<Long?, Long?>?
        @Suppress("UNCHECKED_CAST")
        val selectedTags = flows[6] as Set<String>

        var result = txs

        // 1. Filter by Search Query
        if (query.isNotBlank()) {
            val lowercaseQuery = query.lowercase().trim()
            result = result.filter {
                it.note.lowercase().contains(lowercaseQuery) ||
                it.categoryName.lowercase().contains(lowercaseQuery) ||
                it.accountName.lowercase().contains(lowercaseQuery) ||
                it.tags.lowercase().contains(lowercaseQuery)
            }
        }

        // 2. Filter by Transaction Type
        if (type != "All") {
            val lowercaseType = type.lowercase()
            result = result.filter { it.type == lowercaseType }
        }

        // 3. Filter by Category IDs
        if (catIds.isNotEmpty()) {
            result = result.filter { catIds.contains(it.categoryId) }
        }

        // 4. Filter by Account IDs
        if (accIds.isNotEmpty()) {
            result = result.filter { accIds.contains(it.accountId) }
        }

        // 5. Filter by Date Range
        if (dateRange != null) {
            val startMs = dateRange.first
            val endMs = dateRange.second
            if (startMs != null && endMs != null) {
                result = result.filter {
                    try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                        val txDate = sdf.parse(it.transactionDate)
                        val txMs = txDate?.time ?: 0L
                        txMs in startMs..endMs
                    } catch (e: Exception) {
                        true
                    }
                }
            }
        }

        // 6. Filter by Custom Tags
        if (selectedTags.isNotEmpty()) {
            result = result.filter { tx ->
                val txTags = tx.tags.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
                selectedTags.any { txTags.contains(it.lowercase()) }
            }
        }

        result
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // --- LOCAL NOTIFICATION FEEDBACK ---
    private val _notificationMessage = MutableSharedFlow<NotificationEvent>()
    val notificationMessage = _notificationMessage.asSharedFlow()

    data class SnackbarEvent(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null
    )

    private val _snackbarEvent = MutableSharedFlow<SnackbarEvent>()
    val snackbarEvent = _snackbarEvent.asSharedFlow()

    fun showSnackbar(message: String) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message = message))
        }
    }

    fun showSnackbar(message: String, actionLabel: String, onAction: () -> Unit) {
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarEvent(message = message, actionLabel = actionLabel, onAction = onAction))
        }
    }

    fun triggerLocalNotification(message: String, title: String = "PiggyBag Alert", destination: String? = null) {
        viewModelScope.launch {
            if (settings.value?.notificationsEnabled == true) {
                _notificationMessage.emit(NotificationEvent(title, message, destination))
            }
        }
    }

    // --- RECURRING TRANSACTIONS AUTO SCHEDULER ---
    fun processRecurringTransactions() {
        viewModelScope.launch {
            repository.processRecurringTransactions()
        }
    }

    // --- MUTATION METHODS ---

    // Transactions
    fun insertTransaction(
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        dateStr: String, // format "yyyy-MM-dd"
        attachmentPath: String? = null,
        tags: String = "",
        lifeAreaId: Int? = null,
        subcategoryId: Int? = null,
        purposeId: Int? = null,
        paidBy: String? = null,
        spentFor: String? = null,
        peopleTagged: String? = null,
        vehicleId: Int? = null,
        odometer: Double? = null,
        fuelQuantity: Double? = null,
        studentName: String? = null
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val txDateTime = if (dateStr.contains("T")) dateStr else dateStr + "T09:00:00"
            val tx = Transaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = txDateTime,
                createdAt = nowStr,
                updatedAt = nowStr,
                attachmentPath = attachmentPath,
                tags = tags,
                userId = currentUserId.value,
                lifeAreaId = lifeAreaId,
                subcategoryId = subcategoryId,
                purposeId = purposeId,
                paidBy = paidBy,
                spentFor = spentFor,
                peopleTagged = peopleTagged,
                vehicleId = vehicleId,
                odometer = odometer,
                fuelQuantity = fuelQuantity,
                studentName = studentName
            )
            repository.insertTransaction(tx)

            // Update Vehicle Odometer if applicable
            if (vehicleId != null && odometer != null) {
                val vehicle = repository.getVehicleById(vehicleId)
                if (vehicle != null && odometer > vehicle.lastOdometer) {
                    repository.updateVehicle(vehicle.copy(lastOdometer = odometer))
                }
            }
            
            // Trigger check for budget limits
            checkBudgetLimits(categoryId, type, amount, dateStr)
        }
    }

    fun updateTransaction(
        id: Int,
        amount: Double,
        type: String,
        categoryId: Int,
        accountId: Int,
        note: String,
        dateStr: String,
        attachmentPath: String? = null,
        tags: String = "",
        lifeAreaId: Int? = null,
        subcategoryId: Int? = null,
        purposeId: Int? = null,
        paidBy: String? = null,
        spentFor: String? = null,
        peopleTagged: String? = null,
        vehicleId: Int? = null,
        odometer: Double? = null,
        fuelQuantity: Double? = null,
        studentName: String? = null
    ) {
        viewModelScope.launch {
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val txDateTime = if (dateStr.contains("T")) dateStr else dateStr + "T09:00:00"
            val tx = Transaction(
                id = id,
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                transactionDate = txDateTime,
                createdAt = nowStr, // Keep simple
                updatedAt = nowStr,
                attachmentPath = attachmentPath,
                tags = tags,
                userId = currentUserId.value,
                lifeAreaId = lifeAreaId,
                subcategoryId = subcategoryId,
                purposeId = purposeId,
                paidBy = paidBy,
                spentFor = spentFor,
                peopleTagged = peopleTagged,
                vehicleId = vehicleId,
                odometer = odometer,
                fuelQuantity = fuelQuantity,
                studentName = studentName
            )
            repository.updateTransaction(tx)

            // Update Vehicle Odometer if applicable
            if (vehicleId != null && odometer != null) {
                val vehicle = repository.getVehicleById(vehicleId)
                if (vehicle != null && odometer > vehicle.lastOdometer) {
                    repository.updateVehicle(vehicle.copy(lastOdometer = odometer))
                }
            }
        }
    }

    fun deleteTransaction(transactionId: Int) {
        viewModelScope.launch {
            repository.deleteTransaction(transactionId)
        }
    }

    fun restoreTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    // Accounts
    fun insertAccount(name: String, type: String, openingBalance: Double, color: String, icon: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            val normalizedName = name.trim().lowercase()
            val userId = currentUserId.value
            val existing = repository.getAccountsForUser(userId).first()
            val isDuplicate = existing.any { it.name.trim().lowercase() == normalizedName }
            if (isDuplicate) {
                showSnackbar("An account with this name already exists.")
                onFailure("An account with this name already exists.")
                return@launch
            }
            val account = Account(
                name = name.trim(),
                type = type,
                openingBalance = openingBalance,
                currentBalance = openingBalance,
                color = color,
                icon = icon,
                userId = userId
            )
            repository.insertAccount(account)
            onSuccess()
        }
    }

    fun updateAccount(id: Int, name: String, type: String, openingBalance: Double, currentBalance: Double, color: String, icon: String, onSuccess: () -> Unit = {}, onFailure: (String) -> Unit = {}) {
        viewModelScope.launch {
            val normalizedName = name.trim().lowercase()
            val userId = currentUserId.value
            val existing = repository.getAccountsForUser(userId).first()
            val isDuplicate = existing.any { it.name.trim().lowercase() == normalizedName && it.id != id }
            if (isDuplicate) {
                showSnackbar("An account with this name already exists.")
                onFailure("An account with this name already exists.")
                return@launch
            }
            val account = Account(
                id = id,
                name = name.trim(),
                type = type,
                openingBalance = openingBalance,
                currentBalance = currentBalance,
                color = color,
                icon = icon,
                userId = userId
            )
            repository.updateAccount(account)
            onSuccess()
        }
    }

    fun deleteAccount(account: Account, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteAccount(account)
            if (success) {
                onSuccess()
            } else {
                onFailure("Cannot delete account: transactions reference this account. Please reassign transactions first.")
            }
        }
    }

    // Categories
    fun insertCategory(name: String, type: String, color: String, icon: String) {
        viewModelScope.launch {
            val category = Category(
                name = name,
                type = type,
                color = color,
                icon = icon,
                isDefault = false
            )
            repository.insertCategory(category)
        }
    }

    fun updateCategory(category: Category) {
        viewModelScope.launch {
            repository.updateCategory(category)
        }
    }

    fun updateCategoryOrders(categories: List<Category>) {
        viewModelScope.launch {
            val updatedCategories = categories.mapIndexed { index, category ->
                category.copy(orderIndex = index)
            }
            repository.updateCategories(updatedCategories)
        }
    }

    fun deleteCategory(category: Category, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        viewModelScope.launch {
            val success = repository.deleteCategory(category)
            if (success) {
                onSuccess()
            } else {
                onFailure("Cannot delete category: transactions reference this category. Please reassign transactions first.")
            }
        }
    }

    // Budgets
    fun insertBudget(
        categoryId: Int?, 
        amount: Double, 
        month: Int, 
        year: Int, 
        budgetType: String = "MONTHLY",
        startDate: Long? = null,
        endDate: Long? = null,
        budgetName: String? = null,
        id: Int = 0
    ) {
        viewModelScope.launch {
            val budget = Budget(
                id = id,
                categoryId = categoryId,
                budgetAmount = amount,
                month = month,
                year = year,
                budgetType = budgetType,
                startDate = startDate,
                endDate = endDate,
                budgetName = budgetName,
                userId = currentUserId.value
            )
            repository.insertBudget(budget)
            triggerLocalNotification("Budget configured successfully!")
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Savings Goals
    fun insertSavingsGoal(title: String, targetAmount: Double, currentAmount: Double, targetDate: String, icon: String, color: String) {
        viewModelScope.launch {
            val goal = SavingsGoal(
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                status = if (currentAmount >= targetAmount) "completed" else "active",
                icon = icon,
                color = color,
                userId = currentUserId.value
            )
            repository.insertSavingsGoal(goal)
            
            if (currentAmount >= targetAmount) {
                triggerLocalNotification("Savings Goal '${title}' completed! 🎉")
            }
        }
    }

    fun updateSavingsGoal(id: Int, title: String, targetAmount: Double, currentAmount: Double, targetDate: String, status: String, icon: String, color: String) {
        viewModelScope.launch {
            val newStatus = if (currentAmount >= targetAmount) "completed" else "active"
            val goal = SavingsGoal(
                id = id,
                title = title,
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate,
                status = newStatus,
                icon = icon,
                color = color,
                userId = currentUserId.value
            )
            repository.updateSavingsGoal(goal)
            if (newStatus == "completed" && status != "completed") {
                triggerLocalNotification("Savings Goal '${title}' completed! 🎉")
            }
        }
    }

    fun addSavingsGoalFunds(goal: SavingsGoal, amount: Double) {
        viewModelScope.launch {
            val newAmount = goal.currentAmount + amount
            val newStatus = if (newAmount >= goal.targetAmount) "completed" else "active"
            repository.updateSavingsGoal(
                goal.copy(
                    currentAmount = newAmount,
                    status = newStatus
                )
            )
            if (newStatus == "completed" && goal.status != "completed") {
                triggerLocalNotification("Savings Goal '${goal.title}' completed! 🎉")
            } else {
                triggerLocalNotification("Added funds to '${goal.title}'")
            }
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    // Recurring Rules
    fun insertRecurringRule(amount: Double, type: String, categoryId: Int, accountId: Int, note: String, frequency: String, startDate: String, endConditionType: String = "never", endConditionValue: String = "") {
        viewModelScope.launch {
            val rule = RecurringTransaction(
                amount = amount,
                type = type,
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                frequency = frequency,
                nextExecutionDate = startDate,
                enabled = true,
                userId = currentUserId.value,
                endConditionType = endConditionType,
                endConditionValue = endConditionValue
            )
            repository.insertRecurringTransaction(rule)
        }
    }

    fun updateRecurringRule(rule: RecurringTransaction) {
        viewModelScope.launch {
            repository.updateRecurringTransaction(rule)
        }
    }

    fun deleteRecurringRule(rule: RecurringTransaction) {
        viewModelScope.launch {
            repository.deleteRecurringTransaction(rule)
        }
    }

    // Settings
    fun updateSettings(
        themeMode: String,
        currency: String,
        notificationsEnabled: Boolean,
        debtListEnabled: Boolean = false,
        bottomTabs: String? = null
    ) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    themeMode = themeMode,
                    currency = currency,
                    notificationsEnabled = notificationsEnabled,
                    debtListEnabled = debtListEnabled,
                    bottomTabs = bottomTabs ?: current.bottomTabs
                )
            )
        }
    }

    // --- VEHICLES ---
    fun insertVehicle(nickname: String, regNo: String, type: String, fuelType: String, purchaseDate: String, insExpiry: String?, polExpiry: String?, roadTaxExpiry: String?, serviceDate: String?, odometer: Double) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                registrationNumber = regNo,
                nickname = nickname,
                type = type,
                fuelType = fuelType,
                purchaseDate = purchaseDate,
                insuranceExpiryDate = insExpiry,
                pollutionExpiryDate = polExpiry,
                roadTaxExpiryDate = roadTaxExpiry,
                lastServiceDate = serviceDate,
                lastOdometer = odometer,
                userId = currentUserId.value
            )
            repository.insertVehicle(vehicle)
        }
    }

    fun updateVehicle(id: Int, nickname: String, regNo: String, type: String, fuelType: String, purchaseDate: String, insExpiry: String?, polExpiry: String?, roadTaxExpiry: String?, serviceDate: String?, odometer: Double) {
        viewModelScope.launch {
            val vehicle = Vehicle(
                id = id,
                registrationNumber = regNo,
                nickname = nickname,
                type = type,
                fuelType = fuelType,
                purchaseDate = purchaseDate,
                insuranceExpiryDate = insExpiry,
                pollutionExpiryDate = polExpiry,
                roadTaxExpiryDate = roadTaxExpiry,
                lastServiceDate = serviceDate,
                lastOdometer = odometer,
                userId = currentUserId.value
            )
            repository.updateVehicle(vehicle)
        }
    }

    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            repository.deleteVehicle(vehicle)
        }
    }

    // --- INVESTMENTS ---
    fun insertInvestment(name: String, type: String, purchaseDate: String, purchasePrice: Double, qty: Double, broker: String?, charges: Double, notes: String?) {
        viewModelScope.launch {
            val investment = Investment(
                name = name,
                type = type,
                purchaseDate = purchaseDate,
                purchasePrice = purchasePrice,
                quantity = qty,
                broker = broker,
                transactionCharges = charges,
                notes = notes,
                userId = currentUserId.value
            )
            val id = repository.insertInvestment(investment)
            
            // Trigger gold/silver fetching if gold/silver
            if (type.equals("Gold", ignoreCase = true) || type.equals("Silver", ignoreCase = true)) {
                triggerGoldSilverPricesUpdate()
            }
        }
    }

    fun updateInvestment(id: Int, name: String, type: String, purchaseDate: String, purchasePrice: Double, qty: Double, broker: String?, charges: Double, currentPrice: Double, status: String, notes: String?) {
        viewModelScope.launch {
            val investment = Investment(
                id = id,
                name = name,
                type = type,
                purchaseDate = purchaseDate,
                purchasePrice = purchasePrice,
                quantity = qty,
                broker = broker,
                transactionCharges = charges,
                currentPrice = currentPrice,
                currentStatus = status,
                notes = notes,
                userId = currentUserId.value
            )
            repository.updateInvestment(investment)
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    // --- SUBSCRIPTIONS ---
    fun insertSubscription(name: String, amount: Double, cycle: String, start: String, nextRenewal: String, accId: Int?) {
        viewModelScope.launch {
            val sub = Subscription(
                name = name,
                amount = amount,
                billingCycle = cycle,
                startDate = start,
                nextRenewalDate = nextRenewal,
                accountId = accId,
                userId = currentUserId.value
            )
            repository.insertSubscription(sub)
        }
    }

    fun updateSubscription(id: Int, name: String, amount: Double, cycle: String, start: String, nextRenewal: String, accId: Int?, status: String) {
        viewModelScope.launch {
            val sub = Subscription(
                id = id,
                name = name,
                amount = amount,
                billingCycle = cycle,
                startDate = start,
                nextRenewalDate = nextRenewal,
                accountId = accId,
                status = status,
                userId = currentUserId.value
            )
            repository.updateSubscription(sub)
        }
    }

    fun deleteSubscription(sub: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(sub)
        }
    }

    // --- REMINDERS ---
    fun insertReminder(title: String, type: String, dueDate: String, amount: Double?, recurrence: String) {
        viewModelScope.launch {
            val reminder = Reminder(
                title = title,
                type = type,
                dueDate = dueDate,
                amount = amount,
                recurrence = recurrence,
                enabled = true,
                userId = currentUserId.value
            )
            repository.insertReminder(reminder)
        }
    }

    fun updateReminder(id: Int, title: String, type: String, dueDate: String, amount: Double?, recurrence: String, enabled: Boolean) {
        viewModelScope.launch {
            val reminder = Reminder(
                id = id,
                title = title,
                type = type,
                dueDate = dueDate,
                amount = amount,
                recurrence = recurrence,
                enabled = enabled,
                userId = currentUserId.value
            )
            repository.updateReminder(reminder)
        }
    }

    fun deleteReminder(reminder: Reminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // --- GOLD & SILVER PRICE FETCHERS ---
    fun triggerGoldSilverPricesUpdate() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                // Fetch direct or use fallback
                val goldRate = GoldSilverFetcher.fetchPricePerGramInInr("Gold", todayStr)
                val silverRate = GoldSilverFetcher.fetchPricePerGramInInr("Silver", todayStr)
                repository.insertPrice(
                    GoldSilverPrice(
                        date = todayStr,
                        goldPrice = goldRate,
                        silverPrice = silverRate,
                        fetchedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(Date())
                    )
                )
                
                // Update active gold & silver investments' current price
                val investments = repository.allInvestments.first()
                investments.forEach { inv ->
                    if (inv.type.equals("Gold", ignoreCase = true)) {
                        repository.updateInvestment(inv.copy(currentPrice = goldRate))
                    } else if (inv.type.equals("Silver", ignoreCase = true)) {
                        repository.updateInvestment(inv.copy(currentPrice = silverRate))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchHistoricalPriceForInvestment(type: String, dateStr: String, onPriceFetched: (Double) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val rate = GoldSilverFetcher.fetchPricePerGramInInr(type, dateStr)
            onPriceFetched(rate)
        }
    }

    // --- REMINDER CHECK SCHEDULER ---
    fun checkScheduledReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val activeReminders = repository.getActiveRemindersDirect()
            activeReminders.forEach { rem ->
                if (rem.dueDate <= todayStr) {
                    // Trigger notification
                    triggerLocalNotification(
                        title = "Reminder: ${rem.title}",
                        message = "Your ${rem.type} is due! Amount: ${settings.value?.currency ?: "₹"}${rem.amount ?: 0.0}. Due date: ${rem.dueDate}",
                        destination = "reminders"
                    )
                    // If it is recurring, update next due date!
                    if (rem.recurrence != "None") {
                        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val d = format.parse(rem.dueDate)
                        if (d != null) {
                            val c = Calendar.getInstance().apply { time = d }
                            when (rem.recurrence) {
                                "Daily" -> c.add(Calendar.DAY_OF_YEAR, 1)
                                "Weekly" -> c.add(Calendar.WEEK_OF_YEAR, 1)
                                "Monthly" -> c.add(Calendar.MONTH, 1)
                                "Yearly" -> c.add(Calendar.YEAR, 1)
                            }
                            repository.updateReminder(rem.copy(dueDate = format.format(c.time)))
                        }
                    } else {
                        // Mark as disabled
                        repository.updateReminder(rem.copy(enabled = false))
                    }
                }
            }
        }
    }

    fun insertAutoPay(autoPay: AutoPay) {
        viewModelScope.launch {
            repository.insertAutoPay(autoPay)
            executeDueAutoPays()
        }
    }

    fun updateAutoPay(autoPay: AutoPay) {
        viewModelScope.launch {
            repository.updateAutoPay(autoPay)
            executeDueAutoPays()
        }
    }

    fun deleteAutoPay(autoPay: AutoPay) {
        viewModelScope.launch {
            repository.deleteAutoPay(autoPay)
        }
    }

    fun executeDueAutoPays() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val activeAutoPays = repository.getActiveAutoPaysDirect()
                activeAutoPays.forEach { ap ->
                    if (ap.nextExecutionDate <= todayStr) {
                        // Check if end date is reached
                        if (ap.endDate != null && todayStr > ap.endDate) {
                            repository.updateAutoPay(ap.copy(status = "Completed"))
                            return@forEach
                        }

                        val account = repository.getAccountById(ap.accountId)
                        if (account != null) {
                            if (account.currentBalance >= ap.amount) {
                                val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                                val note = "${ap.name} (Created by AutoPay, Linked AutoPay ID: ${ap.id}, Execution Timestamp: ${nowStr})"
                                val tx = Transaction(
                                    amount = ap.amount,
                                    type = "expense",
                                    categoryId = ap.categoryId,
                                    accountId = ap.accountId,
                                    note = note,
                                    transactionDate = ap.nextExecutionDate + "T09:00:00",
                                    createdAt = nowStr,
                                    updatedAt = nowStr,
                                    userId = currentUserId.value
                                )
                                repository.insertTransaction(tx)
                                repository.updateAccount(account.copy(currentBalance = account.currentBalance - ap.amount))

                                val nextDate = calculateNextExecutionDate(ap.nextExecutionDate, ap.frequency)
                                repository.updateAutoPay(
                                    ap.copy(
                                        lastExecutedDate = todayStr,
                                        nextExecutionDate = nextDate,
                                        status = if (ap.endDate != null && nextDate > ap.endDate) "Completed" else "Active"
                                    )
                                )

                                triggerLocalNotification(
                                    title = "AutoPay Executed: ${ap.name}",
                                    message = "Successfully paid ${settings.value?.currency ?: "₹"}${ap.amount} from ${account.name}."
                                )
                            } else {
                                triggerLocalNotification(
                                    title = "AutoPay Failed: ${ap.name}",
                                    message = "Insufficient balance in account '${account.name}'. Required: ${settings.value?.currency ?: "₹"}${ap.amount}, Available: ${settings.value?.currency ?: "₹"}${account.currentBalance}."
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun calculateNextExecutionDate(currentDateStr: String, frequency: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return try {
            val date = format.parse(currentDateStr) ?: Date()
            val calendar = Calendar.getInstance().apply { time = date }
            when (frequency) {
                "Daily" -> calendar.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> calendar.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> calendar.add(Calendar.MONTH, 1)
                "Every 2 Months" -> calendar.add(Calendar.MONTH, 2)
                "Every 3 Months" -> calendar.add(Calendar.MONTH, 3)
                "Quarterly" -> calendar.add(Calendar.MONTH, 3)
                "Half Yearly" -> calendar.add(Calendar.MONTH, 6)
                "Yearly" -> calendar.add(Calendar.YEAR, 1)
                else -> calendar.add(Calendar.DAY_OF_YEAR, 30)
            }
            format.format(calendar.time)
        } catch (e: Exception) {
            currentDateStr
        }
    }

    fun updateColorPalette(colorPalette: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    colorPalette = colorPalette
                )
            )
        }
    }

    fun updateCustomColor(colorHex: String) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    colorPalette = "Custom",
                    customColor = colorHex
                )
            )
        }
    }

    fun updateCustomColors(iconColorHex: String?, bgColorHex: String?) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    colorPalette = "Custom",
                    customIconColor = iconColorHex,
                    customBgColor = bgColorHex
                )
            )
        }
    }

    fun updateCustomColorsAll(primary: String?, secondary: String?, background: String?) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(
                current.copy(
                    colorPalette = if (primary != null || secondary != null || background != null) "Custom" else "Default",
                    customColor = primary,
                    customIconColor = secondary,
                    customBgColor = background
                )
            )
        }
    }

    fun updatePinSettings(enabled: Boolean, pin: String? = null) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            if (enabled && !pin.isNullOrEmpty()) {
                setPin(pin)
                repository.updateSettings(current.copy(pinEnabled = true))
            } else {
                disablePin()
                repository.updateSettings(current.copy(pinEnabled = false))
            }
        }
    }

    fun updateBiometricSettings(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettingsDirect() ?: Settings()
            repository.updateSettings(current.copy(biometricEnabled = enabled))
        }
    }

    // --- BUDGET TRIGGER EVALUATION ---
    private suspend fun checkBudgetLimits(categoryId: Int, type: String, amount: Double, dateStr: String) {
        if (type != "expense") return

        try {
            // Get Month and Year from dateStr (format "yyyy-MM-dd")
            val parts = dateStr.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt()

            // 1. Check Category Budget
            val catBudget = repository.getBudgetForCategory(categoryId, month, year)
            if (catBudget != null) {
                // SUM all expenses for this category in this month/year
                val txs = allTransactions.value
                val categorySum = txs.filter {
                    it.categoryId == categoryId && 
                    it.type == "expense" && 
                    it.transactionDate.startsWith("$year-${String.format("%02d", month)}")
                }.sumOf { it.amount } + amount

                if (categorySum > catBudget.budgetAmount) {
                    triggerLocalNotification("Alert: Monthly budget for category exceeded! Limit: ${settings.value?.currency}${catBudget.budgetAmount}")
                }
            }

            // 2. Check Overall Budget (categoryId is null)
            val overallBudget = repository.getBudgetForCategory(null, month, year)
            if (overallBudget != null) {
                val txs = allTransactions.value
                val totalExpenses = txs.filter {
                    it.type == "expense" && 
                    it.transactionDate.startsWith("$year-${String.format("%02d", month)}")
                }.sumOf { it.amount } + amount

                if (totalExpenses > overallBudget.budgetAmount) {
                    triggerLocalNotification("Warning: Total monthly budget exceeded! Limit: ${settings.value?.currency}${overallBudget.budgetAmount}")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- BACKUP & RESTORE IO ---
    fun exportTransactionsToCsv(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        setFileProcessing(true, "Exporting transactions to CSV...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            try {
                val txList = allTransactions.value
                val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outStream))
                writer.write("Transaction ID,Date,Type,Amount,Note,Category,Account\n")
                txList.forEach { tx ->
                    val cleanNote = tx.note.replace("\"", "\"\"")
                    val cleanCategory = tx.categoryName.replace("\"", "\"\"")
                    val cleanAccount = tx.accountName.replace("\"", "\"\"")
                    writer.write("${tx.id},${tx.transactionDate},${tx.type},${tx.amount},\"$cleanNote\",\"$cleanCategory\",\"$cleanAccount\"\n")
                }
                writer.flush()
                writer.close()
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    outStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success)
            }
        }
    }

    fun exportTransactionsToPdf(outStream: java.io.OutputStream, onComplete: (Boolean) -> Unit) {
        setFileProcessing(true, "Exporting statement to PDF...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            var logoBitmap: android.graphics.Bitmap? = null
            try {
                val range = sessionManager.getPdfDateRange()
                val customStart = sessionManager.getPdfCustomStartDate()
                val customEnd = sessionManager.getPdfCustomEndDate()
                val dateFormat = sessionManager.getPdfDateFormat()
                val timeFormat = sessionManager.getPdfTimeFormat()
                
                val showNotes = sessionManager.getPdfIncludeNotes()
                val showCategories = sessionManager.getPdfIncludeCategories()
                val showAccount = sessionManager.getPdfIncludeAccount()
                val showRunningBalance = sessionManager.getPdfIncludeRunningBalance()
                val showSummary = sessionManager.getPdfIncludeSummary()
                val showTxIds = sessionManager.getPdfIncludeTransactionIds()

                val sdfParser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                val today = Date()
                
                // Helper to get start and end milliseconds
                fun getStartEndMs(): Pair<Long, Long> {
                    calendar.time = today
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    val todayStart = calendar.timeInMillis
                    val todayEnd = todayStart + 24 * 60 * 60 * 1000L - 1000L

                    return when (range) {
                        "Today" -> Pair(todayStart, todayEnd)
                        "Yesterday" -> {
                            val start = todayStart - 24 * 60 * 60 * 1000L
                            Pair(start, start + 24 * 60 * 60 * 1000L - 1000L)
                        }
                        "This Week" -> {
                            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                            Pair(calendar.timeInMillis, todayEnd)
                        }
                        "This Month" -> {
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            Pair(calendar.timeInMillis, todayEnd)
                        }
                        "Last Month" -> {
                            calendar.add(Calendar.MONTH, -1)
                            calendar.set(Calendar.DAY_OF_MONTH, 1)
                            val start = calendar.timeInMillis
                            val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                            calendar.set(Calendar.DAY_OF_MONTH, maxDay)
                            calendar.set(Calendar.HOUR_OF_DAY, 23)
                            calendar.set(Calendar.MINUTE, 59)
                            calendar.set(Calendar.SECOND, 59)
                            Pair(start, calendar.timeInMillis)
                        }
                        "Custom" -> {
                            val start = if (customStart > 0) {
                                val c = Calendar.getInstance()
                                c.timeInMillis = customStart
                                c.set(Calendar.HOUR_OF_DAY, 0)
                                c.set(Calendar.MINUTE, 0)
                                c.set(Calendar.SECOND, 0)
                                c.set(Calendar.MILLISECOND, 0)
                                c.timeInMillis
                            } else todayStart
                            val end = if (customEnd > 0) {
                                val c = Calendar.getInstance()
                                c.timeInMillis = customEnd
                                c.set(Calendar.HOUR_OF_DAY, 23)
                                c.set(Calendar.MINUTE, 59)
                                c.set(Calendar.SECOND, 59)
                                c.set(Calendar.MILLISECOND, 999)
                                c.timeInMillis
                            } else todayEnd
                            Pair(start, end)
                        }
                        else -> Pair(0L, Long.MAX_VALUE)
                    }
                }

                val (filterStartMs, filterEndMs) = getStartEndMs()
                val txList = allTransactions.value.filter { tx ->
                    val txDatePart = tx.transactionDate.substringBefore("T")
                    val txTimeMs = try { sdfParser.parse(txDatePart)?.time ?: 0L } catch(e: Exception) { 0L }
                    txTimeMs in filterStartMs..filterEndMs
                }.sortedBy { it.transactionDate }

                // Precompute running balances
                val runningBalances = mutableListOf<Double>()
                var currentBal = 0.0
                txList.forEach { tx ->
                    if (tx.type.lowercase() == "income") {
                        currentBal += tx.amount
                    } else {
                        currentBal -= tx.amount
                    }
                    runningBalances.add(currentBal)
                }

                val pdfDocument = android.graphics.pdf.PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                val margin = 40f
                val rowHeight = 24f
                val headerHeight = 35f

                val context = getApplication<Application>()
                logoBitmap = try {
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    android.graphics.BitmapFactory.decodeResource(context.resources, com.titanbag.app.R.drawable.logo, options)
                    val reqWidth = 64
                    val reqHeight = 64
                    var inSampleSize = 1
                    if (options.outHeight > reqHeight || options.outWidth > reqWidth) {
                        val halfHeight = options.outHeight / 2
                        val halfWidth = options.outWidth / 2
                        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                            inSampleSize *= 2
                        }
                    }
                    val decodeOptions = android.graphics.BitmapFactory.Options().apply {
                        this.inSampleSize = inSampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }
                    val rawBitmap = android.graphics.BitmapFactory.decodeResource(context.resources, com.titanbag.app.R.drawable.logo, decodeOptions)
                    if (rawBitmap != null) {
                        val scaled = android.graphics.Bitmap.createScaledBitmap(rawBitmap, 64, 64, true)
                        if (rawBitmap != scaled) {
                            rawBitmap.recycle()
                        }
                        scaled
                    } else null
                } catch (e: Exception) {
                    null
                }

                fun formatPdfDate(dateStr: String): String {
                    val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = try { parser.parse(dateStr) } catch(e: Exception) { null } ?: return dateStr
                    val pattern = when (dateFormat) {
                        "DD/MM/YYYY" -> "dd/MM/yyyy"
                        "MM/DD/YYYY" -> "MM/dd/yyyy"
                        "YYYY-MM-DD" -> "yyyy-MM-dd"
                        "DD MMM YYYY" -> "dd MMM yyyy"
                        else -> "dd/MM/yyyy"
                    }
                    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
                }

                fun formatPdfTime(timeStr: String): String {
                    val parser = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val time = try { parser.parse(timeStr) } catch(e: Exception) { null } ?: return timeStr
                    val pattern = when (timeFormat) {
                        "12-hour" -> "hh:mm a"
                        "24-hour" -> "HH:mm"
                        else -> "hh:mm a"
                    }
                    return SimpleDateFormat(pattern, Locale.getDefault()).format(time)
                }

                fun android.graphics.Paint.ellipsize(text: String, width: Float): String {
                    if (measureText(text) <= width) return text
                    var result = text
                    while (result.isNotEmpty() && measureText("$result...") > width) {
                        result = result.dropLast(1)
                    }
                    return if (result.isEmpty()) "..." else "$result..."
                }

                // Define Columns
                val columns = mutableListOf<Triple<String, Float, (TransactionWithDetails, Double) -> String>>()
                if (showTxIds) {
                    columns.add(Triple("ID", 0.7f, { tx, bal -> tx.id.toString() }))
                }
                columns.add(Triple("Date", 1.0f, { tx, bal ->
                    val parts = tx.transactionDate.split("T")
                    formatPdfDate(parts.getOrNull(0) ?: "")
                }))
                columns.add(Triple("Time", 0.9f, { tx, bal ->
                    val parts = tx.transactionDate.split("T")
                    formatPdfTime(parts.getOrNull(1)?.take(5) ?: "")
                }))
                if (showAccount) {
                    columns.add(Triple("Account", 1.2f, { tx, bal -> tx.accountName }))
                }
                if (showCategories) {
                    columns.add(Triple("Category", 1.2f, { tx, bal -> tx.categoryName }))
                }
                columns.add(Triple("Type", 0.8f, { tx, bal -> tx.type.uppercase() }))
                columns.add(Triple("Amount", 1.1f, { tx, bal -> String.format("₹%.2f", tx.amount) }))
                if (showRunningBalance) {
                    columns.add(Triple("Balance", 1.2f, { tx, bal -> String.format("₹%.2f", bal) }))
                }
                if (showNotes) {
                    columns.add(Triple("Notes", 1.8f, { tx, bal -> tx.note }))
                }

                val totalWeight = columns.sumOf { it.second.toDouble() }.toFloat()
                val availableWidth = pageWidth - 2 * margin
                val colWidths = columns.map { (it.second / totalWeight) * availableWidth }

                // Distribute transactions to pages
                val pages = mutableListOf<List<TransactionWithDetails>>()
                var currentList = mutableListOf<TransactionWithDetails>()
                txList.forEachIndexed { idx, tx ->
                    val limit = if (pages.isEmpty()) {
                        val page1YStart = if (showSummary) 190f else 130f
                        ((pageHeight - page1YStart - margin - 30f) / rowHeight).toInt()
                    } else {
                        ((pageHeight - 60f - margin - 30f) / rowHeight).toInt()
                    }
                    currentList.add(tx)
                    if (currentList.size >= limit || idx == txList.lastIndex) {
                        pages.add(currentList)
                        currentList = mutableListOf()
                    }
                }
                
                val finalPagesList = if (pages.isEmpty()) listOf(emptyList()) else pages

                finalPagesList.forEachIndexed { pageIndex, pageTxList ->
                    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas
                    
                    val paint = android.graphics.Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    }
                    val boldTextPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 9f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    }
                    val titlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.parseColor("#121212")
                        textSize = 22f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    }
                    val subtitlePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 9f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                    }
                    
                    // Header (Only on page 1)
                    if (pageIndex == 0) {
                        var titleStartX = margin
                        if (logoBitmap != null) {
                            try {
                                val dstRect = android.graphics.RectF(margin, 28f, margin + 32f, 28f + 32f)
                                canvas.drawBitmap(logoBitmap, null, dstRect, paint)
                                titleStartX = margin + 42f
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        canvas.drawText("PIGGYBAG", titleStartX, 54f, titlePaint)
                        val currentDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                        canvas.drawText("Statement Range: $range | Generated on: $currentDate", margin, 78f, subtitlePaint)
                        canvas.drawText("Total Transactions: ${txList.size}", margin, 92f, subtitlePaint)
                        
                        // Draw decorative bar
                        paint.color = android.graphics.Color.parseColor("#006B5D")
                        canvas.drawRect(margin, 102f, pageWidth - margin, 105f, paint)

                        // Draw Summary Box
                        if (showSummary && txList.isNotEmpty()) {
                            val boxTop = 115f
                            val boxHeight = 55f
                            val boxWidth = pageWidth - 2 * margin
                            
                            paint.color = android.graphics.Color.parseColor("#F0F7F6")
                            canvas.drawRoundRect(margin, boxTop, margin + boxWidth, boxTop + boxHeight, 8f, 8f, paint)
                            
                            paint.color = android.graphics.Color.parseColor("#B2DFDB")
                            paint.style = android.graphics.Paint.Style.STROKE
                            paint.strokeWidth = 1f
                            canvas.drawRoundRect(margin, boxTop, margin + boxWidth, boxTop + boxHeight, 8f, 8f, paint)
                            paint.style = android.graphics.Paint.Style.FILL
                            
                            val totalIncome = txList.filter { it.type.lowercase() == "income" }.sumOf { it.amount }
                            val totalExpense = txList.filter { it.type.lowercase() == "expense" }.sumOf { it.amount }
                            val netSavings = totalIncome - totalExpense
                            
                            val colW = boxWidth / 3
                            val labelY = boxTop + 20f
                            val valueY = boxTop + 42f
                            
                            val center1 = margin + colW / 2
                            val center2 = margin + colW + colW / 2
                            val center3 = margin + 2 * colW + colW / 2
                            
                            val summaryLabelPaint = android.graphics.Paint(subtitlePaint).apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 8f
                                color = android.graphics.Color.parseColor("#555555")
                            }
                            val summaryValuePaint = android.graphics.Paint(boldTextPaint).apply {
                                textAlign = android.graphics.Paint.Align.CENTER
                                textSize = 11f
                            }
                            
                            canvas.drawText("TOTAL INCOME", center1, labelY, summaryLabelPaint)
                            summaryValuePaint.color = android.graphics.Color.parseColor("#00796B")
                            canvas.drawText(String.format("₹%.2f", totalIncome), center1, valueY, summaryValuePaint)
                            
                            canvas.drawText("TOTAL EXPENSE", center2, labelY, summaryLabelPaint)
                            summaryValuePaint.color = android.graphics.Color.parseColor("#D32F2F")
                            canvas.drawText(String.format("₹%.2f", totalExpense), center2, valueY, summaryValuePaint)
                            
                            canvas.drawText("NET SAVINGS", center3, labelY, summaryLabelPaint)
                            summaryValuePaint.color = if (netSavings >= 0) android.graphics.Color.parseColor("#00796B") else android.graphics.Color.parseColor("#D32F2F")
                            canvas.drawText(String.format("₹%.2f", netSavings), center3, valueY, summaryValuePaint)
                        }
                    } else {
                        var titlePageStartX = margin
                        if (logoBitmap != null) {
                            try {
                                val dstRect = android.graphics.RectF(margin, 25f, margin + 18f, 25f + 18f)
                                canvas.drawBitmap(logoBitmap, null, dstRect, paint)
                                titlePageStartX = margin + 24f
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        canvas.drawText("PIGGYBAG - Statement Range: $range (Page ${pageIndex + 1})", titlePageStartX, 38f, boldTextPaint)
                        paint.color = android.graphics.Color.parseColor("#006B5D")
                        canvas.drawRect(margin, 46f, pageWidth - margin, 48f, paint)
                    }
                    
                    // Draw Table Header
                    val headerY = if (pageIndex == 0) {
                        if (showSummary) 185f else 120f
                    } else 55f
                    
                    paint.color = android.graphics.Color.parseColor("#F0F4F4")
                    canvas.drawRect(margin, headerY, pageWidth - margin, headerY + headerHeight, paint)
                    
                    val headerTextY = headerY + 22f
                    var currentHeaderX = margin
                    columns.forEachIndexed { i, col ->
                        canvas.drawText(col.first, currentHeaderX + 4f, headerTextY, boldTextPaint)
                        currentHeaderX += colWidths[i]
                    }
                    
                    // Draw rows
                    var currentY = headerY + headerHeight
                    pageTxList.forEachIndexed { rowIndex, tx ->
                        val isEven = rowIndex % 2 == 0
                        if (isEven) {
                            paint.color = android.graphics.Color.parseColor("#FAFAFA")
                            canvas.drawRect(margin, currentY, pageWidth - margin, currentY + rowHeight, paint)
                        }
                        
                        val rowTextY = currentY + 16f
                        var cellX = margin
                        
                        val txIndex = txList.indexOf(tx)
                        val runningBal = runningBalances.getOrElse(txIndex) { 0.0 }
                        
                        columns.forEachIndexed { colIndex, col ->
                            val cellText = col.third(tx, runningBal)
                            val allowedWidth = colWidths[colIndex] - 8f
                            val truncated = textPaint.ellipsize(cellText, allowedWidth)
                            
                            val drawPaint = if (col.first == "Type") {
                                android.graphics.Paint(textPaint).apply {
                                    color = if (tx.type.lowercase() == "income") {
                                        android.graphics.Color.parseColor("#00796B")
                                    } else {
                                        android.graphics.Color.parseColor("#D32F2F")
                                    }
                                    typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                                }
                            } else {
                                textPaint
                            }
                            canvas.drawText(truncated, cellX + 4f, rowTextY, drawPaint)
                            cellX += colWidths[colIndex]
                        }
                        currentY += rowHeight
                    }
                    
                    // Footer
                    val footerY = pageHeight - 20f
                    canvas.drawText("Page ${pageIndex + 1} of ${finalPagesList.size}", pageWidth - margin - 60f, footerY, subtitlePaint)
                    pdfDocument.finishPage(page)
                }
                
                pdfDocument.writeTo(outStream)
                pdfDocument.close()
                success = true
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try {
                    logoBitmap?.recycle()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                try {
                    outStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success)
            }
        }
    }

    fun importTransactionsFromCsv(inStream: java.io.InputStream, onComplete: (Boolean, String) -> Unit) {
        setFileProcessing(true, "Importing transactions from CSV...")
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            var success = false
            var message = ""
            try {
                val reader = java.io.BufferedReader(java.io.InputStreamReader(inStream))
                val lines = reader.readLines()
                if (lines.isEmpty()) {
                    onComplete(false, "CSV file is empty")
                    return@launch
                }

                // Check header to identify columns or map them
                val header = lines.first().lowercase()
                val hasHeader = header.contains("amount") || header.contains("type") || header.contains("date")
                val dataLines = if (hasHeader) lines.drop(1) else lines

                var successCount = 0
                var errorCount = 0

                // Parse each line
                dataLines.forEach { line ->
                    if (line.trim().isEmpty()) return@forEach
                    try {
                        val tokens = parseCsvLine(line)
                        if (tokens.size >= 5) {
                            val dateStrRaw: String
                            val typeRaw: String
                            val amountRaw: String
                            val noteRaw: String
                            val categoryRaw: String
                            val accountRaw: String

                            if (tokens.size >= 7 && hasHeader && (header.startsWith("transaction id") || header.contains("id"))) {
                                dateStrRaw = tokens[1].trim()
                                typeRaw = tokens[2].trim()
                                amountRaw = tokens[3].trim()
                                noteRaw = tokens[4].trim()
                                categoryRaw = tokens[5].trim()
                                accountRaw = tokens[6].trim()
                            } else {
                                dateStrRaw = tokens.getOrNull(0)?.trim() ?: ""
                                typeRaw = tokens.getOrNull(1)?.trim() ?: "expense"
                                amountRaw = tokens.getOrNull(2)?.trim() ?: "0.0"
                                noteRaw = tokens.getOrNull(3)?.trim() ?: ""
                                categoryRaw = tokens.getOrNull(4)?.trim() ?: "Other"
                                accountRaw = tokens.getOrNull(5)?.trim() ?: "Cash"
                            }

                            val amount = amountRaw.toDoubleOrNull() ?: 0.0
                            val type = if (typeRaw.lowercase().contains("income")) "income" else "expense"
                            val note = noteRaw.removeSurrounding("\"")
                            val catName = categoryRaw.removeSurrounding("\"").ifBlank { "Other" }
                            val accName = accountRaw.removeSurrounding("\"").ifBlank { "Cash" }

                            // Format Date
                            var finalDate = dateStrRaw.removeSurrounding("\"")
                            if (finalDate.isBlank()) {
                                finalDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                            } else if (!finalDate.contains("T")) {
                                finalDate = "${finalDate}T09:00:00"
                            }

                            // 1. Find or create Account
                            var account = repository.getAccountByNameDirect(accName)
                            if (account == null) {
                                val newAcc = Account(
                                    name = accName,
                                    type = "Other",
                                    openingBalance = 0.0,
                                    currentBalance = 0.0,
                                    color = "#4CAF50",
                                    icon = "wallet"
                                )
                                val accId = repository.insertAccountDirect(newAcc)
                                account = newAcc.copy(id = accId.toInt())
                            }

                            // 2. Find or create Category
                            var category = repository.getCategoryByNameDirect(catName, type)
                            if (category == null) {
                                val newCat = Category(
                                    name = catName,
                                    type = type,
                                    color = if (type == "income") "#4CAF50" else "#F44336",
                                    icon = if (type == "income") "trending_up" else "trending_down"
                                )
                                val catId = repository.insertCategoryDirect(newCat)
                                category = newCat.copy(id = catId.toInt())
                            }

                            // 3. Insert transaction
                            val tx = Transaction(
                                amount = amount,
                                type = type,
                                categoryId = category.id,
                                accountId = account.id,
                                note = note,
                                transactionDate = finalDate,
                                createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                                updatedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
                            )
                            repository.insertTransaction(tx)
                            successCount++
                        } else {
                            errorCount++
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        errorCount++
                    }
                }
                success = true
                message = "Successfully imported $successCount transactions. Failed: $errorCount"
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
                message = "Import failed: ${e.localizedMessage}"
            } finally {
                try {
                    inStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                setFileProcessing(false)
                onComplete(success, message)
            }
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var curVal = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (inQuotes) {
                if (ch == '\"') {
                    if (i + 1 < line.length && line[i + 1] == '\"') {
                        curVal.append('\"')
                        i++
                    } else {
                        inQuotes = false
                    }
                } else {
                    curVal.append(ch)
                }
            } else {
                if (ch == '\"') {
                    inQuotes = true
                } else if (ch == ',') {
                    result.add(curVal.toString())
                    curVal = StringBuilder()
                } else {
                    curVal.append(ch)
                }
            }
            i++
        }
        result.add(curVal.toString())
        return result
    }

    // --- MANAGEMENT HUB: USER PROFILES & SEEDING ---
    val allLocalUserProfiles = repository.getAllLocalUserProfilesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val partnerConnections = currentUserId.flatMapLatest { userId ->
        repository.getPartnerConnectionsForUserFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val userGroups = currentUserId.flatMapLatest { userId ->
        repository.getGroupsForUserFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val allDebtRecords = currentUserId.flatMapLatest { userId ->
        repository.getDebtRecordsFlow(userId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun switchLocalUser(userId: String, showFeedback: Boolean = true) {
        viewModelScope.launch {
            currentUserId.value = userId
            prefs.edit().putString("active_local_user_id", userId).apply()
            if (showFeedback) {
                showSnackbar("Switched active profile")
            }
        }
    }

    fun createLocalUserProfile(name: String, email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val userId = UUID.randomUUID().toString()
            val shareCode = generateUniquePartnerShareCode()
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val profile = LocalUserProfile(userId, name, email, shareCode, nowStr)
            repository.insertLocalUserProfile(profile)
            
            // Switch user immediately on main thread
            launch(Dispatchers.Main) {
                switchLocalUser(userId)
                showSnackbar("Profile created successfully!")
            }
        }
    }

    private suspend fun generateUniquePartnerShareCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        var code: String
        do {
            val grp1 = (1..3).map { chars.random() }.joinToString("")
            val grp2 = (1..4).map { chars.random() }.joinToString("")
            val grp3 = (1..4).map { chars.random() }.joinToString("")
            val grp4 = (1..4).map { chars.random() }.joinToString("")
            val grp5 = (1..4).map { chars.random() }.joinToString("")
            code = "$grp1-$grp2-$grp3-$grp4-$grp5"
        } while (repository.getLocalUserProfileByShareCode(code) != null)
        return code
    }

    private suspend fun seedLocalUsersData() {
        val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val primaryProfile = LocalUserProfile("default_user", "Primary User", "user@titanbag.app", generateUniquePartnerShareCode(), nowStr)

        repository.insertLocalUserProfile(primaryProfile)

        // Find a default expense category
        val allCats = repository.categoryDao.getAllCategories().first()
        val foodCat = allCats.firstOrNull { it.name.lowercase().contains("food") } ?: allCats.firstOrNull { it.type == "expense" }
        
        val foodCatId = foodCat?.id ?: 1

        // Seed Primary Cash account if none exist
        if (repository.accountDao.getAccountCount() == 0) {
            repository.insertAccount(
                Account(name = "My Cash", type = "Cash", openingBalance = 0.0, currentBalance = 0.0, icon = "wallet", color = "#FFD6A5", userId = "default_user")
            )
        }
    }

    // --- PARTNER SHARING ---
    fun connectPartnerLocal(partnerShareCode: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val codeClean = partnerShareCode.replace("-", "").trim().uppercase()
            val currentUser = repository.getLocalUserProfileById(currentUserId.value)
            
            if (currentUser == null) {
                launch(Dispatchers.Main) { onResult(false, "Active profile error") }
                return@launch
            }

            if (currentUser.partnerShareCode == codeClean) {
                launch(Dispatchers.Main) { onResult(false, "Cannot connect to your own share code") }
                return@launch
            }

            val partner = repository.getLocalUserProfileByShareCode(codeClean)
            if (partner == null) {
                launch(Dispatchers.Main) { onResult(false, "Partner code does not exist") }
                return@launch
            }

            val existing = repository.getPartnerConnection(currentUser.id, partner.id)
            if (existing != null) {
                launch(Dispatchers.Main) { onResult(false, "You are already connected to this partner") }
                return@launch
            }

            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val connection = PartnerConnection(
                userId = currentUser.id,
                partnerUserId = partner.id,
                connectedDate = nowStr,
                status = "connected"
            )
            repository.insertPartnerConnection(connection)
            launch(Dispatchers.Main) { onResult(true, "Successfully connected with ${partner.name}!") }
        }
    }

    fun disconnectPartnerLocal(partnerUserId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePartnerConnection(currentUserId.value, partnerUserId)
            launch(Dispatchers.Main) { showSnackbar("Disconnected partner connection") }
        }
    }

    fun getPartnerTransactions(partnerUserId: String): Flow<List<TransactionWithDetails>> {
        return repository.getTransactionsForUser(partnerUserId)
    }

    // --- GROUP EXPENSE SPLIT ---
    fun createGroupLocal(
        title: String,
        description: String = "",
        startDate: String = "",
        endDate: String = "",
        destination: String = "",
        photos: String = "",
        receipts: String = "",
        budget: Double = 0.0,
        currency: String = "₹",
        notes: String = "",
        onResult: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val groupId = UUID.randomUUID().toString()
            val groupPin = generateUniqueGroupPin()
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val activeUser = repository.getLocalUserProfileById(currentUserId.value)
            val displayName = activeUser?.name ?: "Organizer"

            val group = Group(
                id = groupId,
                title = title,
                groupPin = groupPin,
                createdBy = currentUserId.value,
                createdDate = nowStr,
                status = "Running",
                description = description,
                startDate = startDate,
                endDate = endDate,
                destination = destination,
                photos = photos,
                receipts = receipts,
                budget = budget,
                currency = currency,
                notes = notes
            )
            repository.insertGroup(group)

            // Insert creator as a member
            val memberId = UUID.randomUUID().toString()
            val member = GroupMember(
                id = memberId,
                groupId = groupId,
                userId = currentUserId.value,
                displayName = displayName,
                joinedDate = nowStr
            )
            repository.insertGroupMember(member)

            launch(Dispatchers.Main) { onResult(true, groupPin) }
        }
    }

    fun updateGroupLocal(group: Group) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertGroup(group)
            launch(Dispatchers.Main) { showSnackbar("Group details updated") }
        }
    }

    private suspend fun generateUniqueGroupPin(): String {
        var pin: String
        do {
            pin = (100000..999999).random().toString()
        } while (repository.getGroupByPin(pin) != null)
        return pin
    }

    fun joinGroupLocal(groupPin: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val pinClean = groupPin.trim()
            val group = repository.getGroupByPin(pinClean)
            if (group == null) {
                launch(Dispatchers.Main) { onResult(false, "No group found with this PIN") }
                return@launch
            }

            val existingMember = repository.getGroupMemberByGroupAndUser(group.id, currentUserId.value)
            if (existingMember != null) {
                launch(Dispatchers.Main) { onResult(false, "You are already a member of this group") }
                return@launch
            }

            val activeUser = repository.getLocalUserProfileById(currentUserId.value)
            val displayName = activeUser?.name ?: "Guest Member"
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

            val member = GroupMember(
                id = UUID.randomUUID().toString(),
                groupId = group.id,
                userId = currentUserId.value,
                displayName = displayName,
                joinedDate = nowStr
            )
            repository.insertGroupMember(member)

            launch(Dispatchers.Main) { onResult(true, group.title) }
        }
    }

    fun addGroupExpenseLocal(
        groupId: String,
        amount: Double,
        description: String,
        dateStr: String,
        category: String = "",
        subcategory: String = "",
        receipt: String = "",
        location: String = "",
        paymentMethod: String = "",
        tags: String = "",
        participantsIncluded: String = "",
        splitType: String = "Equal",
        shares: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseId = UUID.randomUUID().toString()
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
            val date = if (dateStr.contains("T")) dateStr else dateStr + "T09:00:00"
            val expense = GroupExpense(
                id = expenseId,
                groupId = groupId,
                userId = currentUserId.value,
                amount = amount,
                description = description,
                expenseDate = date,
                createdAt = nowStr,
                category = category,
                subcategory = subcategory,
                receipt = receipt,
                location = location,
                paymentMethod = paymentMethod,
                lastModified = nowStr,
                tags = tags,
                participantsIncluded = participantsIncluded,
                splitType = splitType,
                shares = shares
            )
            repository.insertGroupExpense(expense)
            launch(Dispatchers.Main) { showSnackbar("Group expense added") }

            // Alert / notify other users
            triggerLocalNotification(
                message = "New expense '${description}' of ₹${amount.toInt()} shared. Group members notified!",
                title = "Group Split Update"
            )

            // Simulate another member adding an expense after a brief delay
            kotlinx.coroutines.delay(4000)
            val members = repository.getMembersForGroupDirect(groupId).filter { it.userId != currentUserId.value }
            if (members.isNotEmpty()) {
                val other = members.random()
                val mockExpenses = listOf(
                    "Shared cab ride" to 120.0,
                    "Evening snacks" to 250.0,
                    "Common groceries" to 340.0,
                    "Soft drinks split" to 180.0
                )
                val expenseMock = mockExpenses.random()
                val mockExpenseId = UUID.randomUUID().toString()
                
                val mockExpense = GroupExpense(
                    id = mockExpenseId,
                    groupId = groupId,
                    userId = other.userId,
                    amount = expenseMock.second,
                    description = expenseMock.first,
                    expenseDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                    createdAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date()),
                    splitType = "Equal",
                    participantsIncluded = ""
                )
                repository.insertGroupExpense(mockExpense)
                
                triggerLocalNotification(
                    message = "${other.displayName} added a new expense: '${expenseMock.first}' of ₹${expenseMock.second.toInt()}.",
                    title = "Group Expense Alert"
                )
            }
        }
    }

    fun updateGroupExpenseLocal(expense: GroupExpense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateGroupExpense(expense)
            launch(Dispatchers.Main) { showSnackbar("Group expense updated") }
        }
    }

    fun deleteGroupExpenseLocal(expense: GroupExpense) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteGroupExpense(expense)
            launch(Dispatchers.Main) { showSnackbar("Group expense deleted") }
        }
    }

    fun getGroupExpensesFlow(groupId: String): Flow<List<GroupExpenseWithMember>> {
        return repository.getGroupExpensesFlow(groupId)
    }

    fun getGroupMembersFlow(groupId: String): Flow<List<GroupMember>> {
        return repository.getMembersForGroupFlow(groupId)
    }

    data class GroupMemberBalance(
        val memberId: String,
        val displayName: String,
        var balance: Double
    )

    data class SettlementPayment(
        val fromId: String,
        val fromName: String,
        val toId: String,
        val toName: String,
        val amount: Double
    )

    fun calculateGroupSettlements(
        members: List<GroupMember>,
        expenses: List<GroupExpense>
    ): List<SettlementPayment> {
        if (members.isEmpty()) return emptyList()

        val netBalances = members.associate { it.userId to 0.0 }.toMutableMap()

        expenses.forEach { exp ->
            val currentCredit = netBalances[exp.userId] ?: 0.0
            netBalances[exp.userId] = currentCredit + exp.amount

            val participants = if (exp.participantsIncluded.isNotBlank()) {
                exp.participantsIncluded.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else {
                members.map { it.userId }
            }

            val shareValues = if (exp.shares.isNotBlank()) {
                exp.shares.split(",").map { it.trim().toDoubleOrNull() ?: 0.0 }
            } else {
                emptyList()
            }

            if (participants.isNotEmpty()) {
                when (exp.splitType) {
                    "Equal", "Only Selected Members" -> {
                        val shareAmt = exp.amount / participants.size
                        participants.forEach { pId ->
                            netBalances[pId] = (netBalances[pId] ?: 0.0) - shareAmt
                        }
                    }
                    "Percentage" -> {
                        participants.forEachIndexed { i, pId ->
                            val percent = shareValues.getOrNull(i) ?: 0.0
                            val shareAmt = exp.amount * (percent / 100.0)
                            netBalances[pId] = (netBalances[pId] ?: 0.0) - shareAmt
                        }
                    }
                    "Custom Amount" -> {
                        participants.forEachIndexed { i, pId ->
                            val shareAmt = shareValues.getOrNull(i) ?: 0.0
                            netBalances[pId] = (netBalances[pId] ?: 0.0) - shareAmt
                        }
                    }
                    "Shares" -> {
                        val totalShares = shareValues.sum()
                        participants.forEachIndexed { i, pId ->
                            val shVal = shareValues.getOrNull(i) ?: 0.0
                            val shareAmt = if (totalShares > 0) exp.amount * (shVal / totalShares) else 0.0
                            netBalances[pId] = (netBalances[pId] ?: 0.0) - shareAmt
                        }
                    }
                    else -> {
                        val shareAmt = exp.amount / participants.size
                        participants.forEach { pId ->
                            netBalances[pId] = (netBalances[pId] ?: 0.0) - shareAmt
                        }
                    }
                }
            }
        }

        val balances = members.map { member ->
            val net = netBalances[member.userId] ?: 0.0
            GroupMemberBalance(
                memberId = member.userId,
                displayName = member.displayName,
                balance = net
            )
        }

        val debtors = balances.filter { it.balance < -0.01 }
            .sortedBy { it.balance }
            .toMutableList()
        val creditors = balances.filter { it.balance > 0.01 }
            .sortedByDescending { it.balance }
            .toMutableList()

        val settlements = mutableListOf<SettlementPayment>()
        var debtorIndex = 0
        var creditorIndex = 0

        while (debtorIndex < debtors.size && creditorIndex < creditors.size) {
            val debtor = debtors[debtorIndex]
            val creditor = creditors[creditorIndex]

            val amountToPay = minOf(-debtor.balance, creditor.balance)
            if (amountToPay > 0.01) {
                settlements.add(
                    SettlementPayment(
                        fromId = debtor.memberId,
                        fromName = debtor.displayName,
                        toId = creditor.memberId,
                        toName = creditor.displayName,
                        amount = amountToPay
                    )
                )
            }

            debtor.balance += amountToPay
            creditor.balance -= amountToPay

            if (debtor.balance >= -0.01) debtorIndex++
            if (creditor.balance <= 0.01) creditorIndex++
        }

        return settlements
    }

    fun getSettlementsForGroupFlow(groupId: String): Flow<List<GroupSettlement>> {
        return repository.getSettlementsForGroupFlow(groupId)
    }

    suspend fun getSettlementsForGroupDirect(groupId: String): List<GroupSettlement> {
        return repository.getSettlementsForGroupDirect(groupId)
    }

    fun updateGroupSettlementStatus(settlementId: String, newStatus: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val settlements = repository.getSettlementsForGroupDirect(repository.groupDao.getGroupsForUserFlow(currentUserId.value).firstOrNull()?.firstOrNull()?.id ?: "")
            val settlement = settlements.find { it.id == settlementId }
            if (settlement != null) {
                repository.updateSettlement(settlement.copy(status = newStatus))
                launch(Dispatchers.Main) { showSnackbar("Settlement status updated") }
            }
        }
    }

    fun finalizeGroup(groupId: String, members: List<GroupMember>, expenses: List<GroupExpense>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.deleteSettlementsForGroup(groupId)
                
                val list = calculateGroupSettlements(members, expenses)
                val settlements = list.map {
                    GroupSettlement(
                        id = java.util.UUID.randomUUID().toString(),
                        groupId = groupId,
                        fromUserId = it.fromId,
                        fromUserName = it.fromName,
                        toUserId = it.toId,
                        toUserName = it.toName,
                        amount = it.amount,
                        status = "Pending"
                    )
                }
                repository.insertSettlements(settlements)
                
                val group = repository.groupDao.getGroupById(groupId)
                if (group != null) {
                    repository.groupDao.insertGroup(group.copy(status = "Completed"))
                }
                launch(Dispatchers.Main) { showSnackbar("Group finalized and settlements generated!") }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reopenGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = repository.groupDao.getGroupById(groupId)
            if (group != null) {
                repository.groupDao.insertGroup(group.copy(status = "Running"))
                repository.deleteSettlementsForGroup(groupId)
                launch(Dispatchers.Main) { showSnackbar("Group event reopened for modifications!") }
            }
        }
    }

    fun archiveGroup(groupId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = repository.groupDao.getGroupById(groupId)
            if (group != null) {
                repository.groupDao.insertGroup(group.copy(status = "Archived"))
                launch(Dispatchers.Main) { showSnackbar("Group event archived successfully!") }
            }
        }
    }

    // --- DEBT LIST ---
    fun insertDebtRecordLocal(
        personName: String,
        amount: Double,
        action: String,
        borrowedDate: String,
        remainderBoolean: Boolean,
        dateTimestamp: String?,
        modeOfTransaction: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val record = DebtRecord(
                userId = currentUserId.value,
                personName = personName,
                amount = amount,
                action = action,
                borrowedDate = borrowedDate,
                remainderBoolean = remainderBoolean,
                dateTimestamp = dateTimestamp,
                returnedDate = null,
                status = "Pending",
                modeOfTransaction = modeOfTransaction
            )
            repository.insertDebtRecord(record)
            launch(Dispatchers.Main) { showSnackbar("Debt record created successfully!") }
        }
    }

    fun updateDebtRecordLocal(record: DebtRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDebtRecord(record)
        }
    }

    fun deleteDebtRecordLocal(record: DebtRecord) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDebtRecord(record)
            launch(Dispatchers.Main) { showSnackbar("Debt record deleted") }
        }
    }

    private fun checkDebtReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val activeUserId = currentUserId.value
                    val records = repository.getDebtRecordsDirect(activeUserId)
                    val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US).format(Date())
                    
                    records.forEach { record ->
                        if (record.remainderBoolean && record.status.lowercase() == "pending" && record.dateTimestamp != null) {
                            if (record.dateTimestamp <= nowStr) {
                                val actionDesc = if (record.action.lowercase() == "debt") "you borrowed" else "you lent"
                                triggerLocalNotification(
                                    message = "Debt Alert: ${record.personName} (${actionDesc} ₹${record.amount.toInt()}). Date: ${record.borrowedDate}",
                                    title = "Debt Reminder",
                                    destination = "debt_list"
                                )
                                repository.updateDebtRecord(record.copy(remainderBoolean = false))
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                kotlinx.coroutines.delay(10000) // check every 10 seconds
            }
        }
    }
}

