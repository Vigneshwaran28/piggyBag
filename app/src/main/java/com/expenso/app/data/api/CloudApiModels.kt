package com.expenso.app.data.api

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("display_name") val displayName: String
)

data class LoginRequest(
    val identifier: String, // email or username
    val password: String
)

data class ConnectPartnerRequest(
    @SerializedName("partner_code") val partnerCode: String
)

data class JournalSyncPayload(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    val title: String,
    val amount: Double,
    val category: String,
    val notes: String,
    @SerializedName("payment_method") val paymentMethod: String,
    val date: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    val deleted: Boolean
)

data class SyncRequest(
    val journals: List<JournalSyncPayload>
)

data class UserDto(
    val id: String,
    val username: String,
    val email: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("partner_share_code") val partnerShareCode: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("last_login") val lastLogin: String?
)

data class PartnerDto(
    val id: String,
    @SerializedName("user_one_id") val userOneId: String,
    @SerializedName("user_two_id") val userTwoId: String,
    @SerializedName("connected_at") val connectedAt: String,
    val status: String,
    @SerializedName("partner_display_name") val partnerDisplayName: String?,
    @SerializedName("partner_username") val partnerUsername: String?
)

data class AuthResponse(
    val token: String,
    val user: UserDto
)

data class ProfileResponse(
    val user: UserDto,
    val partner: PartnerDto?
)

data class SyncResponse(
    @SerializedName("synced_ids") val syncedIds: List<String>,
    @SerializedName("remote_journals") val remoteJournals: List<JournalSyncPayload>
)

data class GeneralResponse(
    val success: Boolean,
    val message: String
)
