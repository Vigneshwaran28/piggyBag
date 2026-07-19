package com.titanbag.app.data.api

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    @SerializedName("display_name") val displayName: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("device_manufacturer") val deviceManufacturer: String,
    @SerializedName("device_id") val deviceId: String
)

data class LoginRequest(
    val identifier: String, // email or username
    val password: String,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("device_manufacturer") val deviceManufacturer: String,
    @SerializedName("device_id") val deviceId: String
)

data class GoogleLoginRequest(
    @SerializedName("id_token") val idToken: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("profile_photo") val profilePhoto: String? = null,
    @SerializedName("device_model") val deviceModel: String,
    @SerializedName("device_manufacturer") val deviceManufacturer: String,
    @SerializedName("device_id") val deviceId: String
)

data class ConnectPartnerRequest(
    @SerializedName("partner_code") val partnerCode: String
)

data class JournalSyncPayload(
    val id: String,
    @SerializedName("owner_id") val ownerId: String,
    @SerializedName("shared_partner_id") val sharedPartnerId: String?,
    val title: String,
    val amount: Double,
    val type: String,
    val category: String,
    val notes: String,
    @SerializedName("payment_method") val paymentMethod: String,
    val location: String?,
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
    @SerializedName("profile_photo") val profilePhoto: String? = null,
    @SerializedName("auth_token") val authToken: String? = null,
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
    val user: UserDto,
    @SerializedName("is_new") val isNew: Boolean? = false
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

data class CreateJournalRequest(
    val title: String,
    val description: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    val currency: String
)

data class JoinJournalRequest(
    val token: String
)

data class SharedJournalDto(
    val id: String,
    @SerializedName("creator_id") val creatorId: String,
    val title: String,
    val description: String?,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    val currency: String,
    @SerializedName("join_token") val joinToken: String?,
    val role: String,
    @SerializedName("member_count") val memberCount: Int,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class SharedJournalTransactionDto(
    val id: String,
    @SerializedName("journal_id") val journalId: String,
    @SerializedName("paid_by") val paidBy: String,
    @SerializedName("paid_by_name") val paidByName: String,
    val amount: Double,
    val category: String,
    val description: String?,
    val date: String,
    val type: String,
    val notes: String?,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)

data class JournalFullDetailsResponse(
    val journal: SharedJournalDto,
    val members: List<UserDto>,
    val transactions: List<SharedJournalTransactionDto>
)

// ─── ACCOUNTS ────────────────────────────────────────────────────────────────

data class AccountDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val name: String,
    val type: String,
    @SerializedName("opening_balance") val openingBalance: Double,
    @SerializedName("current_balance") val currentBalance: Double,
    val icon: String?,
    val color: String?
)

data class AccountRequest(
    val name: String,
    val type: String,
    @SerializedName("opening_balance") val openingBalance: Double = 0.0,
    @SerializedName("current_balance") val currentBalance: Double = 0.0,
    val icon: String? = null,
    val color: String? = null
)

// ─── BUDGETS ─────────────────────────────────────────────────────────────────

data class BudgetDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("budget_amount") val budgetAmount: Double,
    val month: Int,
    val year: Int,
    @SerializedName("budget_type") val budgetType: String,
    @SerializedName("start_date") val startDate: String?,
    @SerializedName("end_date") val endDate: String?,
    @SerializedName("budget_name") val budgetName: String?
)

data class BudgetRequest(
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("budget_amount") val budgetAmount: Double,
    val month: Int,
    val year: Int,
    @SerializedName("budget_type") val budgetType: String = "MONTHLY",
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("end_date") val endDate: String? = null,
    @SerializedName("budget_name") val budgetName: String? = null
)

// ─── SAVINGS GOALS ───────────────────────────────────────────────────────────

data class SavingsGoalDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val title: String,
    @SerializedName("target_amount") val targetAmount: Double,
    @SerializedName("current_amount") val currentAmount: Double,
    @SerializedName("target_date") val targetDate: String?,
    val status: String,
    val icon: String?,
    val color: String?
)

data class SavingsGoalRequest(
    val title: String,
    @SerializedName("target_amount") val targetAmount: Double,
    @SerializedName("current_amount") val currentAmount: Double = 0.0,
    @SerializedName("target_date") val targetDate: String? = null,
    val status: String = "active",
    val icon: String? = null,
    val color: String? = null
)

// ─── RECURRING TRANSACTIONS ──────────────────────────────────────────────────

data class RecurringTransactionDto(
    val id: Int,
    @SerializedName("user_id") val userId: String,
    val amount: Double,
    val type: String,
    @SerializedName("category_id") val categoryId: Int?,
    @SerializedName("account_id") val accountId: Int?,
    val note: String?,
    val frequency: String,
    @SerializedName("next_execution_date") val nextExecutionDate: String?,
    val enabled: Boolean
)

data class RecurringTransactionRequest(
    val amount: Double,
    val type: String,
    @SerializedName("category_id") val categoryId: Int? = null,
    @SerializedName("account_id") val accountId: Int? = null,
    val note: String? = null,
    val frequency: String,
    @SerializedName("next_execution_date") val nextExecutionDate: String? = null,
    val enabled: Boolean = true
)

// ─── SETTINGS ────────────────────────────────────────────────────────────────

data class SettingsDto(
    @SerializedName("user_id") val userId: String,
    @SerializedName("theme_mode") val themeMode: String,
    val currency: String,
    @SerializedName("pin_enabled") val pinEnabled: Boolean,
    @SerializedName("biometric_enabled") val biometricEnabled: Boolean,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean,
    @SerializedName("debt_list_enabled") val debtListEnabled: Boolean,
    @SerializedName("color_palette") val colorPalette: String,
    @SerializedName("custom_color") val customColor: String?,
    @SerializedName("custom_icon_color") val customIconColor: String?,
    @SerializedName("custom_bg_color") val customBgColor: String?
)

data class SettingsRequest(
    @SerializedName("theme_mode") val themeMode: String = "system",
    val currency: String = "₹",
    @SerializedName("pin_enabled") val pinEnabled: Boolean = false,
    @SerializedName("biometric_enabled") val biometricEnabled: Boolean = false,
    @SerializedName("notifications_enabled") val notificationsEnabled: Boolean = true,
    @SerializedName("debt_list_enabled") val debtListEnabled: Boolean = true,
    @SerializedName("color_palette") val colorPalette: String = "Default",
    @SerializedName("custom_color") val customColor: String? = null,
    @SerializedName("custom_icon_color") val customIconColor: String? = null,
    @SerializedName("custom_bg_color") val customBgColor: String? = null
)

// ─── DEBT RECORDS ─────────────────────────────────────────────────────────────

data class DebtRecordDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("person_name") val personName: String,
    @SerializedName("borrowed_date") val borrowedDate: String,
    val action: String,
    val amount: Double,
    @SerializedName("remainder_boolean") val remainderBoolean: Boolean,
    @SerializedName("date_timestamp") val dateTimestamp: String?,
    @SerializedName("returned_date") val returnedDate: String?,
    val status: String,
    @SerializedName("mode_of_transaction") val modeOfTransaction: String
)

data class DebtRecordRequest(
    val id: String? = null,
    @SerializedName("person_name") val personName: String,
    @SerializedName("borrowed_date") val borrowedDate: String,
    val action: String,
    val amount: Double,
    @SerializedName("remainder_boolean") val remainderBoolean: Boolean = false,
    @SerializedName("date_timestamp") val dateTimestamp: String? = null,
    @SerializedName("returned_date") val returnedDate: String? = null,
    val status: String = "Pending",
    @SerializedName("mode_of_transaction") val modeOfTransaction: String = "Cash"
)

// ─── CATEGORIES ───────────────────────────────────────────────────────────────

data class CategoryDto(
    val id: Int,
    val name: String,
    val type: String,
    val icon: String?,
    val color: String?,
    @SerializedName("is_default") val isDefault: Boolean,
    @SerializedName("order_index") val orderIndex: Int
)

data class CategoryRequest(
    val name: String,
    val type: String,
    val icon: String? = null,
    val color: String? = null,
    @SerializedName("order_index") val orderIndex: Int = 0
)

data class SetPasswordRequest(
    val password: String
)
