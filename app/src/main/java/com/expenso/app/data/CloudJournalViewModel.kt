package com.expenso.app.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CloudJournalViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = SessionManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val apiClient = CloudApiClient(sessionManager)
    val repository = CloudRepository(db, apiClient, sessionManager)

    // Auth & Profile Flow
    val currentUser = repository.currentUserFlow
    val partnerRelation = repository.partnerFlow
    
    // UI Filters
    val selectedOwnerFilter = MutableStateFlow("Combined") // Combined, Me, Partner
    val selectedTimeFilter = MutableStateFlow("All") // All, Today, Week, Month, Year
    val selectedCategoryFilter = MutableStateFlow("All") // All, Food, Transport, etc.

    // Status State Flows
    private val _isAuthProcessing = MutableStateFlow(false)
    val isAuthProcessing = _isAuthProcessing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    private val _partnerError = MutableStateFlow<String?>(null)
    val partnerError = _partnerError.asStateFlow()

    private val _partnerSuccess = MutableStateFlow<String?>(null)
    val partnerSuccess = _partnerSuccess.asStateFlow()

    // Sync Queue Size Flow
    val pendingQueueCount = db.syncQueueDao().getPendingItemsFlow()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun AppDatabase.syncQueueDao() = this.syncQueueDao() // helper accessor

    init {
        // Schedule auto sync when app starts if enabled
        if (sessionManager.isLoggedIn() && sessionManager.isAutoSyncEnabled()) {
            scheduleAutoSync()
        }
    }

    // Reactive Filtered Journals
    val filteredJournals: StateFlow<List<JournalEntity>> = combine(
        repository.allJournalsFlow,
        selectedOwnerFilter,
        selectedTimeFilter,
        selectedCategoryFilter,
        currentUser
    ) { journals, owner, time, category, user ->
        var list = journals
        val currentUserId = user?.id ?: ""

        // 1. Filter by Owner
        list = when (owner) {
            "Me" -> list.filter { it.ownerId == currentUserId }
            "Partner" -> list.filter { it.ownerId != currentUserId }
            else -> list
        }

        // 2. Filter by Category
        if (category != "All") {
            list = list.filter { it.category.lowercase() == category.lowercase() }
        }

        // 3. Filter by Time
        val cal = Calendar.getInstance()
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        list = when (time) {
            "Today" -> list.filter { it.date.startsWith(todayStr) }
            "Week" -> {
                cal.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgoStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
                list.filter { it.date >= weekAgoStr }
            }
            "Month" -> {
                val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
                list.filter { it.date.startsWith(currentMonthPrefix) }
            }
            "Year" -> {
                val currentYearPrefix = SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                list.filter { it.date.startsWith(currentYearPrefix) }
            }
            else -> list
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flow derived helper: database extension helper to get pending items flow
    private fun SyncQueueDao.getPendingItemsFlow(): Flow<List<SyncQueueEntity>> = flow {
        while (true) {
            emit(getPendingItems())
            kotlinx.coroutines.delay(2000)
        }
    }

    // Monthly summary stats
    val monthlySummary = filteredJournals.map { journals ->
        var income = 0.0
        var expense = 0.0
        journals.forEach {
            if (it.category.lowercase() == "salary") {
                income += it.amount
            } else {
                expense += it.amount
            }
        }
        Pair(income, expense)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Pair(0.0, 0.0))

    // Auth APIs
    fun register(username: String, email: String, password: String, displayName: String, onResult: (Boolean, String?) -> Unit) {
        _isAuthProcessing.value = true
        _authError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.register(username, email, password, displayName)
            _isAuthProcessing.value = false
            if (result.isSuccess) {
                if (sessionManager.isAutoSyncEnabled()) scheduleAutoSync()
                launch(Dispatchers.Main) { onResult(true, null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Registration failed"
                _authError.value = msg
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    fun login(identifier: String, password: String, onResult: (Boolean, String?) -> Unit) {
        _isAuthProcessing.value = true
        _authError.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.login(identifier, password)
            _isAuthProcessing.value = false
            if (result.isSuccess) {
                if (sessionManager.isAutoSyncEnabled()) scheduleAutoSync()
                launch(Dispatchers.Main) { onResult(true, null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Login failed"
                _authError.value = msg
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logout()
            cancelSyncWork()
        }
    }

    // Partner APIs
    fun connectPartner(shareCode: String, onResult: (Boolean, String?) -> Unit) {
        _partnerError.value = null
        _partnerSuccess.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.connectPartner(shareCode)
            if (result.isSuccess) {
                val successMsg = result.getOrNull() ?: "Connected successfully"
                _partnerSuccess.value = successMsg
                repository.sync()
                launch(Dispatchers.Main) { onResult(true, successMsg) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Connection failed"
                _partnerError.value = errorMsg
                launch(Dispatchers.Main) { onResult(false, errorMsg) }
            }
        }
    }

    fun disconnectPartner(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.disconnectPartner()
            if (result.isSuccess) {
                val successMsg = result.getOrNull() ?: "Disconnected successfully"
                _partnerSuccess.value = successMsg
                launch(Dispatchers.Main) { onResult(true, successMsg) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Disconnection failed"
                _partnerError.value = errorMsg
                launch(Dispatchers.Main) { onResult(false, errorMsg) }
            }
        }
    }

    // CRUD Journal
    fun createJournal(title: String, amount: Double, category: String, notes: String, paymentMethod: String, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createJournal(title, amount, category, notes, paymentMethod, date)
            if (sessionManager.isAutoSyncEnabled()) {
                syncNow()
            }
        }
    }

    fun updateJournal(id: String, title: String, amount: Double, category: String, notes: String, paymentMethod: String, date: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateJournal(id, title, amount, category, notes, paymentMethod, date)
            if (sessionManager.isAutoSyncEnabled()) {
                syncNow()
            }
        }
    }

    fun deleteJournal(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteJournal(id)
            if (sessionManager.isAutoSyncEnabled()) {
                syncNow()
            }
        }
    }

    // Force Trigger Sync
    fun syncNow() {
        if (_isSyncing.value) return
        _isSyncing.value = true
        _syncMessage.value = "Synchronizing journals..."
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.sync()
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Sync successful!"
            } else {
                _syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    // SharedPreferences Actions
    fun updateBaseUrl(url: String) {
        sessionManager.setBaseUrl(url)
    }

    fun updateAutoSync(enabled: Boolean) {
        sessionManager.setAutoSyncEnabled(enabled)
        if (enabled) {
            scheduleAutoSync()
        } else {
            cancelSyncWork()
        }
    }

    // WorkManager background scheduler
    private fun scheduleAutoSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniquePeriodicWork(
                "CloudJournalSyncWork",
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
    }

    private fun cancelSyncWork() {
        WorkManager.getInstance(getApplication()).cancelUniqueWork("CloudJournalSyncWork")
    }
}
