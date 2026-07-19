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
    val status: String, // "Running", "Completed", "Cancelled", "Archived"
    val description: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val destination: String = "",
    val photos: String = "",
    val receipts: String = "",
    val budget: Double = 0.0,
    val currency: String = "₹",
    val notes: String = ""
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
    val createdAt: String,
    val category: String = "",
    val subcategory: String = "",
    val receipt: String = "",
    val location: String = "",
    val paymentMethod: String = "",
    val lastModified: String = "",
    val tags: String = "",
    val participantsIncluded: String = "",
    val splitType: String = "Equal",
    val shares: String = ""
)

@Entity(
    tableName = "group_settlements",
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
data class GroupSettlement(
    @PrimaryKey val id: String, // UUID
    val groupId: String,
    val fromUserId: String,
    val fromUserName: String,
    val toUserId: String,
    val toUserName: String,
    val amount: Double,
    val status: String // "Pending", "Partially Paid", "Paid", "Disputed", "Cancelled"
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
