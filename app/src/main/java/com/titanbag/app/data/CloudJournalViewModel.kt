package com.titanbag.app.data

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
import com.titanbag.app.data.api.CloudApiClient
import com.titanbag.app.data.api.JournalFullDetailsResponse
import com.titanbag.app.data.api.SharedJournalDto
import com.titanbag.app.data.api.SharedJournalTransactionDto

class CloudJournalViewModel(application: Application) : AndroidViewModel(application) {

    val sessionManager = SessionManager(application)
    private val db = AppDatabase.getDatabase(application)
    private val apiClient = CloudApiClient(sessionManager)
    val repository = CloudRepository(db, apiClient, sessionManager)

    // Auth & Profile Flow
    val currentUser = repository.currentUserFlow
    val partnerRelation = repository.partnerFlow
    val allSharedJournals = repository.allSharedJournalsFlow
    
    private val _selectedJournalDetails = MutableStateFlow<JournalFullDetailsResponse?>(null)
    val selectedJournalDetails = _selectedJournalDetails.asStateFlow()

    private val _isJournalLoading = MutableStateFlow(false)
    val isJournalLoading = _isJournalLoading.asStateFlow()
    
    // UI Filters
    val selectedOwnerFilter = MutableStateFlow("Combined") // Combined, Me, Partner
    val selectedTimeFilter = MutableStateFlow("All") // All, Today, Week, Month, Year
    val selectedCategoryFilter = MutableStateFlow("All") // All, Food, Transport, etc.

    // Status State Flows
    private val _authState = MutableStateFlow<CloudAuthState>(CloudAuthState.CheckingSession)
    val authState = _authState.asStateFlow()

    private val _isAuthProcessing = MutableStateFlow(false)
    val isAuthProcessing = _isAuthProcessing.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError = _authError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _lastSyncError = MutableStateFlow<SyncError?>(null)
    val lastSyncError = _lastSyncError.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun setAuthError(message: String?) {
        _authError.value = message
    }

    fun setAuthProcessing(processing: Boolean) {
        _isAuthProcessing.value = processing
    }

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
        checkSession()
    }

    private fun checkSession() {
        viewModelScope.launch {
            repository.currentUserFlow.collect { user ->
                val hasToken = sessionManager.getAuthToken() != null
                if (user != null && hasToken) {
                    _authState.value = CloudAuthState.LoggedIn(user)
                    if (sessionManager.isAutoSyncEnabled()) {
                        scheduleAutoSync()
                    }
                } else {
                    // If one exists but not the other, we are in an inconsistent state
                    if (user != null || hasToken) {
                        repository.logout()
                    }
                    _authState.value = CloudAuthState.LoggedOut
                }
            }
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
        _authState.value = CloudAuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val deviceId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            val result = repository.register(username, email, password, displayName, deviceId)
            _isAuthProcessing.value = false
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onResult(true, null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Registration failed"
                _authError.value = msg
                _authState.value = CloudAuthState.LoginFailed(msg)
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    fun login(identifier: String, password: String, onResult: (Boolean, String?) -> Unit) {
        _isAuthProcessing.value = true
        _authError.value = null
        _authState.value = CloudAuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val deviceId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            val result = repository.login(identifier, password, deviceId)
            _isAuthProcessing.value = false
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onResult(true, null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Login failed"
                _authError.value = msg
                _authState.value = CloudAuthState.LoginFailed(msg)
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    fun loginWithGoogle(idToken: String, displayName: String? = null, profilePhoto: String? = null, onResult: (Boolean, String?) -> Unit) {
        _isAuthProcessing.value = true
        _authError.value = null
        _authState.value = CloudAuthState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            val deviceId = android.provider.Settings.Secure.getString(
                getApplication<Application>().contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            ) ?: "unknown_device"
            val result = repository.loginWithGoogle(idToken, displayName, profilePhoto, deviceId)
            _isAuthProcessing.value = false
            if (result.isSuccess) {
                launch(Dispatchers.Main) { onResult(true, null) }
            } else {
                val msg = result.exceptionOrNull()?.message ?: "Google login failed"
                _authError.value = msg
                _authState.value = CloudAuthState.LoginFailed(msg)
                launch(Dispatchers.Main) { onResult(false, msg) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.logout()
            _authState.value = CloudAuthState.LoggedOut
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

    fun blockPartner(onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.blockPartner()
            if (result.isSuccess) {
                val successMsg = result.getOrNull() ?: "Partner blocked"
                _partnerSuccess.value = successMsg
                launch(Dispatchers.Main) { onResult(true, successMsg) }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Block failed"
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
        _lastSyncError.value = null
        _syncMessage.value = "Synchronizing journals..."
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.sync()
            _isSyncing.value = false
            if (result.isSuccess) {
                _syncMessage.value = "Sync successful!"
            } else {
                val error = result.exceptionOrNull()
                val syncError = when {
                    !sessionManager.isLoggedIn() -> SyncError.NotLoggedIn()
                    error?.message?.contains("Unable to connect", ignoreCase = true) == true -> SyncError.Network(error.stackTraceToString())
                    error?.message?.contains("401", ignoreCase = true) == true -> SyncError.Authentication(error.stackTraceToString())
                    else -> SyncError.Unknown(error?.message ?: "Unknown error")
                }
                _lastSyncError.value = syncError
                _syncMessage.value = "Sync failed: ${syncError.message}"
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

    // --- SHARED JOURNALS ACTIONS ---

    fun loadSharedJournals() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.fetchSharedJournals()
        }
    }

    fun createSharedJournal(title: String, description: String?, startDate: String?, endDate: String?, currency: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.createSharedJournal(title, description, startDate, endDate, currency)
            launch(Dispatchers.Main) { onResult(result.isSuccess) }
        }
    }

    fun joinJournal(token: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.joinSharedJournal(token)
            launch(Dispatchers.Main) {
                onResult(result.isSuccess, result.exceptionOrNull()?.message)
            }
        }
    }

    fun selectJournal(journalId: String) {
        _isJournalLoading.value = true
        _selectedJournalDetails.value = null
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.fetchJournalDetails(journalId)
            _isJournalLoading.value = false
            if (result.isSuccess) {
                _selectedJournalDetails.value = result.getOrNull()
            }
        }
    }

    fun addSharedJournalTransaction(journalId: String, amount: Double, category: String, description: String?, date: String, notes: String?, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.addSharedJournalTransaction(journalId, amount, category, description, date, notes)
            launch(Dispatchers.Main) { onResult(result.isSuccess) }
        }
    }
}
