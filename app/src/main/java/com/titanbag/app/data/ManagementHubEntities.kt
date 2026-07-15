package com.titanbag.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "local_user_profiles")
data class LocalUserProfile(
    @PrimaryKey val id: String, // UUID or seeded ID
    val name: String,
    val email: String,
    val partnerShareCode: String,
    val createdDate: String
)

@Entity(
    tableName = "local_partner_connections",
    foreignKeys = [
        ForeignKey(
            entity = LocalUserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId"), Index("partnerUserId")]
)
data class PartnerConnection(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val partnerUserId: String,
    val connectedDate: String,
    val status: String // "connected", "pending"
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey val id: String, // UUID
    val title: String,
    val groupPin: String,
    val createdBy: String, // LocalUserProfile.id
    val createdDate: String,
    val status: String // "active", "completed"
)

@Entity(
    tableName = "group_members",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class GroupMember(
    @PrimaryKey val id: String, // UUID
    val groupId: String,
    val userId: String, // LocalUserProfile.id or a random ID if guest
    val displayName: String,
    val joinedDate: String
)

@Entity(
    tableName = "group_expenses",
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["id"],
            childColumns = ["groupId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("groupId")]
)
data class GroupExpense(
    @PrimaryKey val id: String, // UUID
    val groupId: String,
    val userId: String, // LocalUserProfile.id
    val amount: Double,
    val description: String,
    val expenseDate: String,
    val createdAt: String
)

@Entity(
    tableName = "debt_records",
    foreignKeys = [
        ForeignKey(
            entity = LocalUserProfile::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("userId")]
)
data class DebtRecord(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String,
    val personName: String,
    val borrowedDate: String,
    val action: String, // "Debt" or "Credit"
    val amount: Double,
    val remainderBoolean: Boolean,
    val dateTimestamp: String?,
    val returnedDate: String?,
    val status: String,
    val modeOfTransaction: String = "Cash"
)
