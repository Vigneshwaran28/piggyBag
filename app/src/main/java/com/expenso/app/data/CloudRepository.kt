package com.expenso.app.data

import com.expenso.app.data.api.*
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

    val allJournalsFlow: Flow<List<JournalEntity>> = journalDao.getAllJournalsFlow()
    val currentUserFlow: Flow<UserEntity?> = userDao.getCurrentUserFlow()
    val partnerFlow: Flow<PartnerEntity?> = partnerDao.getPartnerRelationFlow()

    private fun getCurrentIsoTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
    }

    suspend fun register(username: String, email: String, password: String, displayName: String): Result<UserEntity> {
        return try {
            val api = apiClient.getService()
            val response = api.register(RegisterRequest(username, email, password, displayName))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                val userEntity = UserEntity(
                    id = auth.user.id,
                    username = auth.user.username,
                    email = auth.user.email,
                    displayName = auth.user.displayName,
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
                Result.success(userEntity)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Registration failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(identifier: String, password: String): Result<UserEntity> {
        return try {
            val api = apiClient.getService()
            val response = api.login(LoginRequest(identifier, password))
            if (response.isSuccessful && response.body() != null) {
                val auth = response.body()!!
                val userEntity = UserEntity(
                    id = auth.user.id,
                    username = auth.user.username,
                    email = auth.user.email,
                    displayName = auth.user.displayName,
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
                
                // Fetch partner details directly
                fetchProfile()
                
                Result.success(userEntity)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Login failed"
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchProfile(): Result<ProfileResponse> {
        return try {
            val api = apiClient.getService()
            val response = api.getProfile()
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                // Update cached partner
                partnerDao.clearPartner()
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
                } else {
                    sessionManager.savePartner(null, null)
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
                Result.success(response.body()!!.message)
            } else {
                Result.failure(Exception(response.errorBody()?.string() ?: "Failed to disconnect partner"))
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
                    title = it.title,
                    amount = it.amount,
                    category = it.category,
                    notes = it.notes,
                    paymentMethod = it.paymentMethod,
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
                                    title = remote.title,
                                    amount = remote.amount,
                                    category = remote.category,
                                    notes = remote.notes,
                                    paymentMethod = remote.paymentMethod,
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
                                            title = remote.title,
                                            amount = remote.amount,
                                            category = remote.category,
                                            notes = remote.notes,
                                            paymentMethod = remote.paymentMethod,
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
                                        title = remote.title,
                                        amount = remote.amount,
                                        category = remote.category,
                                        notes = remote.notes,
                                        paymentMethod = remote.paymentMethod,
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
        sessionManager.clearSession()
    }
}
