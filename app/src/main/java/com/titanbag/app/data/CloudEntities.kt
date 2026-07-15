package com.titanbag.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_users")
data class UserEntity(
    @PrimaryKey val id: String, // Google ID or UUID
    val username: String,
    val email: String,
    val displayName: String,
    val profilePhoto: String? = null,
    val authToken: String? = null,
    val partnerShareCode: String,
    val createdAt: String,
    val updatedAt: String,
    val lastLogin: String?
)

@Entity(tableName = "cloud_partners")
data class PartnerEntity(
    @PrimaryKey val id: String, // UUID
    val userOneId: String,
    val userTwoId: String,
    val connectedAt: String,
    val status: String // active, disconnected
)

@Entity(tableName = "cloud_journals")
data class JournalEntity(
    @PrimaryKey val id: String, // UUID
    val ownerId: String,
    val sharedPartnerId: String? = null,
    val title: String,
    val amount: Double,
    val type: String = "expense",
    val category: String,
    val notes: String,
    val paymentMethod: String,
    val location: String? = null,
    val date: String, // ISO-8601 string
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: String, // LOCAL_ONLY, SYNCED, MODIFIED, DELETED, FAILED
    val deleted: Boolean = false
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // "journal", "shared_journal_tx"
    val entityId: String, // UUID of the entity
    val operation: String, // INSERT, UPDATE, DELETE
    val retryCount: Int = 0,
    val lastAttempt: String? = null,
    val status: String = "PENDING" // PENDING, FAILED
)

@Entity(tableName = "shared_journals")
data class SharedJournalEntity(
    @PrimaryKey val id: String,
    val creatorId: String,
    val title: String,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val currency: String,
    val joinToken: String?,
    val role: String, // creator, member
    val memberCount: Int,
    val createdAt: String,
    val updatedAt: String
)

@Entity(tableName = "shared_journal_transactions")
data class SharedJournalTransactionEntity(
    @PrimaryKey val id: String,
    val journalId: String,
    val paidBy: String,
    val paidByName: String,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: String,
    val type: String,
    val notes: String?,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: String = "SYNCED"
)

