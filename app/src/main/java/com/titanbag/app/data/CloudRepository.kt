package com.titanbag.app.data

import com.titanbag.app.data.api.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class CloudRepository(
    private val db: AppDatabase,
    private val apiClient: CloudApiClient,
    private val sessionManager: SessionManager
) {

    val userDao = db.cloudUserDao()
    val partnerDao = db.cloudPartnerDao()
    val journalDao = db.cloudJournalDao()
    val queueDao = db.syncQueueDao()
    val sharedJournalDao = db.sharedJournalDao()
    val sharedJournalTxDao = db.sharedJournalTransactionDao()

    val allJournalsFlow: Flow<List<JournalEntity>> = journalDao.getAllJournalsFlow()
    val allSharedJournalsFlow: Flow<List<SharedJournalEntity>> = sharedJournalDao.getAllJournalsFlow()
    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val partnerFlow: Flow<PartnerEntity?> = partnerDao.getPartnerRelationFlow()

    private fun getCurrentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    // --- SHARED JOURNALS METHODS ---

    suspend fun fetchSharedJournals(): Result<List<SharedJournalEntity>> {
        return try {
            val api = apiClient.getService()
            val response = api.getMySharedJournals()
            if (response.isSuccessful && response.body() != null) {
                val dtos = response.body()!!
                val entities = dtos.map { dto ->
                    SharedJournalEntity(
                        id = dto.id,
                        creatorId = dto.creatorId,
                        title = dto.title,
                        description = dto.description,
                        startDate = dto.startDate,
                        endDate = dto.endDate,
                        currency = dto.currency,
                        joinToken = dto.joinToken,
                        role = dto.role,
                        memberCount = dto.memberCount,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                }
                sharedJournalDao.clearAll()
                sharedJournalDao.insertJournals(entities)
                Result.success(entities)
            } else {
                Result.failure(Exception("Failed to fetch shared journals"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createSharedJournal(title: String, description: String?, startDate: String?, endDate: String?, currency: String): Result<SharedJournalEntity> {
        return try {
            val api = apiClient.getService()
            val response = api.createSharedJournal(CreateJournalRequest(title, description, startDate, endDate, currency))
            if (response.isSuccessful && response.body() != null) {
                val dto = response.body()!!
                val entity = SharedJournalEntity(
                    id = dto.id,
                    creatorId = dto.creatorId,
                    title = dto.title,
                    description = dto.description,
                    startDate = dto.startDate,
                    endDate = dto.endDate,
                    currency = dto.currency,
                    joinToken = dto.joinToken,
                    role = dto.role,
                    memberCount = dto.memberCount,
                    createdAt = dto.createdAt,
                    updatedAt = dto.updatedAt
                )
                sharedJournalDao.insertJournal(entity)
                Result.success(entity)
            } else {
                Result.failure(Exception("Failed to create journal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinSharedJournal(token: String): Result<Boolean> {
        return try {
            val api = apiClient.getService()
            val response = api.joinSharedJournal(JoinJournalRequest(token))
            if (response.isSuccessful) {
                fetchSharedJournals()
                Result.success(true)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to join journal"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchJournalDetails(journalId: String): Result<JournalFullDetailsResponse> {
        return try {
            val api = apiClient.getService()
            val response = api.getJournalDetails(journalId)
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Update transactions locally
                val txEntities = data.transactions.map { dto ->
                    SharedJournalTransactionEntity(
                        id = dto.id,
                        journalId = dto.journalId,
                        paidBy = dto.paidBy,
                        paidByName = dto.paidByName,
                        amount = dto.amount,
                        category = dto.category,
                        description = dto.description,
                        date = dto.date,
                        type = dto.type,
                        notes = dto.notes,
                        createdAt = dto.createdAt,
                        updatedAt = dto.updatedAt
                    )
                }
                sharedJournalTxDao.deleteTransactionsForJournal(journalId)
                sharedJournalTxDao.insertTransactions(txEntities)
                Result.success(data)
            } else {
                Result.failure(Exception("Failed to fetch journal details"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addSharedJournalTransaction(journalId: String, amount: Double, category: String, description: String?, date: String, notes: String?): Result<Boolean> {
        return try {
            val api = apiClient.getService()
            val requestDto = SharedJournalTransactionDto(
                id = "", // Server generates
                journalId = journalId,
                paidBy = "", // Server identifies from token
                paidByName = "",
                amount = amount,
                category = category,
                description = description,
                date = date,
                type = "expense",
                notes = notes,
                createdAt = "",
                updatedAt = ""
            )
            val response = api.addJournalTransaction(journalId, requestDto)
            if (response.isSuccessful) {
                fetchJournalDetails(journalId)
                Result.success(true)
            } else {
                Result.failure(Exception("Failed to add transaction"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun register(username: String, email: String, password: String, displayName: String, deviceId: String): Result<UserEntity> {
        return try {
            val api = apiClient.getService()
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            val response = api.register(RegisterRequest(username, email, password, displayName, deviceModel, deviceManufacturer, deviceId))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                return handleAuthSuccess(auth)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identifier: String, password: String, deviceId: String): Result<UserEntity> {
        return try {
            val api = apiClient.getService()
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            val response = api.login(LoginRequest(identifier, password, deviceModel, deviceManufacturer, deviceId))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                return handleAuthSuccess(auth)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginWithGoogle(idToken: String, displayName: String? = null, profilePhoto: String? = null, deviceId: String): Result<UserEntity> {
        return try {
            val api = apiClient.getService()
            val deviceModel = android.os.Build.MODEL
            val deviceManufacturer = android.os.Build.MANUFACTURER
            val response = api.loginWithGoogle(GoogleLoginRequest(idToken, displayName, profilePhoto, deviceModel, deviceManufacturer, deviceId))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                return handleAuthSuccess(auth)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Google login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleAuthSuccess(auth: AuthResponse): Result<UserEntity> {
        val userEntity = UserEntity(
            id = auth.user.id,
            username = auth.user.username,
            email = auth.user.email,
            displayName = auth.user.displayName,
            profilePhoto = auth.user.profilePhoto,
            authToken = auth.token,
            partnerShareCode = auth.user.partnerShareCode,
            createdAt = auth.user.createdAt,
            updatedAt = auth.user.updatedAt,
            lastLogin = auth.user.lastLogin
        )
        userDao.clearUser()
        userDao.insertUser(userEntity)
        sessionManager.saveSession(
            userId = auth.user.id,
            username = auth.user.username,
            email = auth.user.email,
            displayName = auth.user.displayName,
            shareCode = auth.user.partnerShareCode,
            token = auth.token
        )
        
        // Save photo to session if available
        auth.user.profilePhoto?.let { photoUrl ->
            sessionManager.saveProfilePhoto(photoUrl)
        }

        // Manage LocalUserProfile and migrate local data to the new cloud userId
        try {
            val profileDao = db.localUserProfileDao()
            val localProfiles = profileDao.getAllProfilesDirect()
            val cloudUserId = auth.user.id
            val cloudEmail = auth.user.email
            val cloudName = auth.user.displayName ?: auth.user.username
            val cloudShareCode = auth.user.partnerShareCode
            val nowStr = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

            val existingProfile = localProfiles.find {
                it.id == cloudUserId || 
                it.email.equals(cloudEmail, ignoreCase = true) ||
                it.name.equals(cloudName, ignoreCase = true) ||
                it.name.equals(auth.user.username, ignoreCase = true)
            }

            if (existingProfile != null) {
                if (existingProfile.id != cloudUserId) {
                    val oldId = existingProfile.id
                    profileDao.deleteProfile(existingProfile)
                    val updatedProfile = LocalUserProfile(
                        id = cloudUserId,
                        name = cloudName,
                        email = cloudEmail,
                        partnerShareCode = cloudShareCode,
                        createdDate = existingProfile.createdDate
                    )
                    profileDao.insertProfile(updatedProfile)

                    // Migrate local tables
                    db.accountDao().updateUserId(oldId, cloudUserId)
                    db.transactionDao().updateUserId(oldId, cloudUserId)
                    db.budgetDao().updateUserId(oldId, cloudUserId)
                    db.savingsGoalDao().updateUserId(oldId, cloudUserId)
                    db.recurringTransactionDao().updateUserId(oldId, cloudUserId)
                    db.debtRecordDao().updateUserId(oldId, cloudUserId)
                    db.partnerConnectionDao().updateUserId(oldId, cloudUserId)
                    db.groupDao().updateCreatedBy(oldId, cloudUserId)
                    db.groupMemberDao().updateUserId(oldId, cloudUserId)
                    db.groupExpenseDao().updateUserId(oldId, cloudUserId)
                } else {
                    val updatedProfile = LocalUserProfile(
                        id = cloudUserId,
                        name = cloudName,
                        email = cloudEmail,
                        partnerShareCode = cloudShareCode,
                        createdDate = existingProfile.createdDate
                    )
                    profileDao.insertProfile(updatedProfile)
                }
            } else {
                val newProfile = LocalUserProfile(
                    id = cloudUserId,
                    name = cloudName,
                    email = cloudEmail,
                    partnerShareCode = cloudShareCode,
                    createdDate = nowStr
                )
                profileDao.insertProfile(newProfile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fetch partner details directly
        fetchProfile()

        return Result.success(userEntity)
    }

    suspend fun fetchProfile(): Result<ProfileResponse> {
        return try {
            val api = apiClient.getService()
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Update cached partner
                partnerDao.clearPartner()
                val activeUserId = sessionManager.getUserId() ?: ""
                if (data.partner != null) {
                    val partnerEntity = PartnerEntity(
                        id = data.partner.id,
                        userOneId = data.partner.userOneId,
                        userTwoId = data.partner.userTwoId,
                        connectedAt = data.partner.connectedAt,
                        status = data.partner.status
                    )
                    partnerDao.insertPartner(partnerEntity)
                    sessionManager.savePartner(data.partner.id, data.partner.partnerDisplayName ?: data.partner.partnerUsername)

                    if (activeUserId.isNotBlank()) {
                        val partnerUserId = if (data.partner.userOneId == activeUserId) data.partner.userTwoId else data.partner.userOneId
                        val partnerName = data.partner.partnerDisplayName ?: data.partner.partnerUsername ?: "Partner"
                        
                        val localProfileDao = db.localUserProfileDao()
                        var partnerProfile = localProfileDao.getProfileById(partnerUserId)
                        if (partnerProfile == null) {
                            partnerProfile = LocalUserProfile(
                                id = partnerUserId,
                                name = partnerName,
                                email = "",
                                partnerShareCode = "",
                                createdDate = data.partner.connectedAt ?: getCurrentIsoTimestamp()
                            )
                            localProfileDao.insertProfile(partnerProfile)
                        }

                        val connDao = db.partnerConnectionDao()
                        val existingConn = connDao.getConnection(activeUserId, partnerUserId)
                        if (existingConn == null) {
                            val conn = PartnerConnection(
                                userId = activeUserId,
                                partnerUserId = partnerUserId,
                                connectedDate = data.partner.connectedAt ?: getCurrentIsoTimestamp(),
                                status = "connected"
                            )
                            connDao.insertConnection(conn)
                        }
                    }
                } else {
                    sessionManager.savePartner(null, null)
                    if (activeUserId.isNotBlank()) {
                        db.partnerConnectionDao().deleteConnectionsForUser(activeUserId)
                    }
                }
                Result.success(data)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to fetch profile"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connectPartner(partnerCode: String): Result<String> {
        return try {
            val api = apiClient.getService()
            val response = api.connectPartner(ConnectPartnerRequest(partnerCode))
            if (response.isSuccessful && response.body() != null) {
                fetchProfile() // Refresh partner state locally
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to connect partner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun disconnectPartner(): Result<String> {
        return try {
            val api = apiClient.getService()
            val response = api.disconnectPartner()
            if (response.isSuccessful && response.body() != null) {
                partnerDao.clearPartner()
                sessionManager.savePartner(null, null)
                
                val activeUserId = sessionManager.getUserId()
                if (activeUserId != null) {
                    db.partnerConnectionDao().deleteConnectionsForUser(activeUserId)
                }

                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to disconnect partner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun blockPartner(): Result<String> {
        return try {
            val api = apiClient.getService()
            val response = api.blockPartner()
            if (response.isSuccessful && response.body() != null) {
                partnerDao.clearPartner()
                sessionManager.savePartner(null, null)
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to block partner"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun createJournal(
        title: String,
        amount: Double,
        category: String,
        notes: String,
        paymentMethod: String,
        date: String
    ): String {
        val ownerId = sessionManager.getUserId() ?: ""
        val journalId = UUID.randomUUID().toString()
        val timestamp = getCurrentIsoTimestamp()

        val journal = JournalEntity(
            id = journalId,
            ownerId = ownerId,
            title = title,
            amount = amount,
            category = category,
            notes = notes,
            paymentMethod = paymentMethod,
            date = date,
            createdAt = timestamp,
            updatedAt = timestamp,
            syncStatus = "LOCAL_ONLY",
            deleted = false
        )

        journalDao.insertJournal(journal)

        // Queue for sync
        queueDao.insertQueueItem(
            SyncQueueEntity(
                entityType = "journal",
                entityId = journalId,
                operation = "INSERT",
                status = "PENDING"
            )
        )

        return journalId
    }

    suspend fun updateJournal(
        id: String,
        title: String,
        amount: Double,
        category: String,
        notes: String,
        paymentMethod: String,
        date: String
    ) {
        val existing = journalDao.getJournalById(id) ?: return
        val timestamp = getCurrentIsoTimestamp()

        val updated = existing.copy(
            title = title,
            amount = amount,
            category = category,
            notes = notes,
            paymentMethod = paymentMethod,
            date = date,
            updatedAt = timestamp,
            syncStatus = "MODIFIED"
        )

        journalDao.insertJournal(updated)

        // Check if there is already an insert in queue
        val existingQueue = queueDao.getQueueItemByEntityId(id)
        if (existingQueue == null) {
            queueDao.insertQueueItem(
                SyncQueueEntity(
                    entityType = "journal",
                    entityId = id,
                    operation = "UPDATE",
                    status = "PENDING"
                )
            )
        } else if (existingQueue.operation == "INSERT") {
            // Keep INSERT since it has not been uploaded yet
        } else {
            // Update queue operation to UPDATE and status to PENDING
            queueDao.insertQueueItem(existingQueue.copy(operation = "UPDATE", status = "PENDING"))
        }
    }

    suspend fun deleteJournal(id: String) {
        val existing = journalDao.getJournalById(id) ?: return
        val timestamp = getCurrentIsoTimestamp()

        val deletedJournal = existing.copy(
            deleted = true,
            updatedAt = timestamp,
            syncStatus = "DELETED"
        )

        journalDao.insertJournal(deletedJournal)

        // Update queue
        val existingQueue = queueDao.getQueueItemByEntityId(id)
        if (existingQueue == null) {
            queueDao.insertQueueItem(
                SyncQueueEntity(
                    entityType = "journal",
                    entityId = id,
                    operation = "DELETE",
                    status = "PENDING"
                )
            )
        } else if (existingQueue.operation == "INSERT") {
            // It was never sent to server, so we can delete from Room and queue completely!
            journalDao.deleteJournalById(id)
            queueDao.deleteQueueItem(existingQueue)
        } else {
            queueDao.insertQueueItem(existingQueue.copy(operation = "DELETE", status = "PENDING"))
        }
    }

    suspend fun sync(): Result<Boolean> {
        if (!sessionManager.isLoggedIn()) {
            return Result.failure(Exception("User not logged in"))
        }
        return try {
            val api = apiClient.getService()
            
            // 1. Prepare local modifications to push
            val pendingJournals = journalDao.getSyncPendingJournals()
            val syncPayloads = pendingJournals.map {
                JournalSyncPayload(
                    id = it.id,
                    ownerId = it.ownerId,
                    sharedPartnerId = it.sharedPartnerId,
                    title = it.title,
                    amount = it.amount,
                    type = it.type,
                    category = it.category,
                    notes = it.notes,
                    paymentMethod = it.paymentMethod,
                    location = it.location,
                    date = it.date,
                    createdAt = it.createdAt,
                    updatedAt = it.updatedAt,
                    deleted = it.deleted
                )
            }

            // 2. Call batch sync API
            val response = api.syncJournals(SyncRequest(syncPayloads))
            if (response.isSuccessful && response.body() != null) {
                val syncData = response.body()!!
                val syncedIdsSet = syncData.syncedIds.toSet()

                // 3. Update locally pushed changes that were successfully synchronized
                pendingJournals.forEach { local ->
                    if (syncedIdsSet.contains(local.id)) {
                        queueDao.deleteQueueItemByEntityId(local.id)
                        if (local.deleted) {
                            journalDao.deleteJournalById(local.id)
                        } else {
                            journalDao.insertJournal(local.copy(syncStatus = "SYNCED"))
                        }
                    }
                }

                // 4. Incorporate remote changes (including partner journals)
                syncData.remoteJournals.forEach { remote ->
                    val local = journalDao.getJournalById(remote.id)
                    if (local == null) {
                        if (!remote.deleted) {
                            journalDao.insertJournal(
                                JournalEntity(
                                    id = remote.id,
                                    ownerId = remote.ownerId,
                                    sharedPartnerId = remote.sharedPartnerId,
                                    title = remote.title,
                                    amount = remote.amount,
                                    type = remote.type,
                                    category = remote.category,
                                    notes = remote.notes,
                                    paymentMethod = remote.paymentMethod,
                                    location = remote.location,
                                    date = remote.date,
                                    createdAt = remote.createdAt,
                                    updatedAt = remote.updatedAt,
                                    syncStatus = "SYNCED",
                                    deleted = false
                                )
                            )
                        }
                    } else {
                        // Conflict resolution: Newest updated_at wins!
                        val localPendingQueue = queueDao.getQueueItemByEntityId(remote.id)
                        
                        if (localPendingQueue != null) {
                            // If local has pending edits, compare timestamps
                            if (remote.updatedAt > local.updatedAt) {
                                // Server wins
                                if (remote.deleted) {
                                    journalDao.deleteJournalById(remote.id)
                                } else {
                                    journalDao.insertJournal(
                                        local.copy(
                                            ownerId = remote.ownerId,
                                            sharedPartnerId = remote.sharedPartnerId,
                                            title = remote.title,
                                            amount = remote.amount,
                                            type = remote.type,
                                            category = remote.category,
                                            notes = remote.notes,
                                            paymentMethod = remote.paymentMethod,
                                            location = remote.location,
                                            date = remote.date,
                                            createdAt = remote.createdAt,
                                            updatedAt = remote.updatedAt,
                                            syncStatus = "SYNCED",
                                            deleted = false
                                        )
                                    )
                                }
                                queueDao.deleteQueueItemByEntityId(remote.id)
                            } else {
                                // Local wins, keep local version. It will be uploaded in next sync.
                            }
                        } else {
                            // Local has no edits, simply update to remote version
                            if (remote.deleted) {
                                journalDao.deleteJournalById(remote.id)
                            } else {
                                journalDao.insertJournal(
                                    JournalEntity(
                                        id = remote.id,
                                        ownerId = remote.ownerId,
                                        sharedPartnerId = remote.sharedPartnerId,
                                        title = remote.title,
                                        amount = remote.amount,
                                        type = remote.type,
                                        category = remote.category,
                                        notes = remote.notes,
                                        paymentMethod = remote.paymentMethod,
                                        location = remote.location,
                                        date = remote.date,
                                        createdAt = remote.createdAt,
                                        updatedAt = remote.updatedAt,
                                        syncStatus = "SYNCED",
                                        deleted = false
                                    )
                                )
                            }
                        }
                    }
                }

                // Refresh partner details if connected
                fetchProfile()

                Result.success(true)
            } else {
                // Update queue items to FAILED status
                val pendingQueue = queueDao.getPendingItems()
                pendingQueue.forEach {
                    queueDao.updateQueueItem(it.copy(
                        status = "FAILED",
                        retryCount = it.retryCount + 1,
                        lastAttempt = getCurrentIsoTimestamp()
                    ))
                }
                Result.failure(Exception(response.errorBody()?.string() ?: "Sync failed"))
            }
        } catch (e: Exception) {
            val pendingQueue = queueDao.getPendingItems()
            pendingQueue.forEach {
                queueDao.updateQueueItem(it.copy(
                    status = "FAILED",
                    retryCount = it.retryCount + 1,
                    lastAttempt = getCurrentIsoTimestamp()
                ))
            }
            Result.failure(e)
        }
    }

    suspend fun logout() {
        userDao.clearUser()
        partnerDao.clearPartner()
        journalDao.clearAll()
        sharedJournalDao.clearAll()
        sharedJournalTxDao.clearAll()
        sessionManager.clearSession()
    }
}
