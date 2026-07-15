package com.expenso.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cloud_users")
data class UserEntity(
    @PrimaryKey val id: String, // UUID
    val username: String,
    val email: String,
    val displayName: String,
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
    val title: String,
    val amount: Double,
    val category: String,
    val notes: String,
    val paymentMethod: String,
    val date: String, // ISO-8601 string
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: String, // LOCAL_ONLY, SYNCED, MODIFIED, DELETED, FAILED
    val deleted: Boolean = false
)

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String, // "journal"
    val entityId: String, // UUID of the entity
    val operation: String, // INSERT, UPDATE, DELETE
    val retryCount: Int = 0,
    val lastAttempt: String? = null,
    val status: String = "PENDING" // PENDING, FAILED
)
