package com.titanbag.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(context: Context) {

    // Non-sensitive preferences (font, colors, sync settings) — plain SharedPreferences
    private val plainPrefs: SharedPreferences =
        context.getSharedPreferences("piggyBag_prefs", Context.MODE_PRIVATE)

    // Sensitive auth preferences — EncryptedSharedPreferences with safe fallback
    private val securePrefs: SharedPreferences = createSecurePrefs(context)

    private fun createSecurePrefs(context: Context): SharedPreferences {
        val fileName = "piggyBag_secure_session_v2"
        return try {
            val masterKey = androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM)
                .build()

            androidx.security.crypto.EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SessionManager", "EncryptedSharedPreferences critical failure: ${e.message}")
            // Critical failure: do not fall back to plain text.
            // Returning an empty, non-persistent preference as a temporary "dead" state.
            context.getSharedPreferences("dead_session", Context.MODE_PRIVATE).apply {
                edit().clear().apply()
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // AUTH SESSION — stored in securePrefs (encrypted)
    // ──────────────────────────────────────────────────────────────────────────

    fun saveSession(
        userId: String,
        username: String,
        email: String,
        displayName: String,
        shareCode: String,
        token: String
    ) {
        securePrefs.edit().apply {
            putString("user_id", userId)
            putString("username", username)
            putString("email", email)
            putString("display_name", displayName)
            putString("share_code", shareCode)
            putString("auth_token", token)
            apply()
        }
    }

    fun savePartner(partnerId: String?, partnerName: String?) {
        securePrefs.edit().apply {
            putString("partner_id", partnerId)
            putString("partner_name", partnerName)
            apply()
        }
    }

    fun saveProfilePhoto(url: String) {
        securePrefs.edit().putString("profile_photo", url).apply()
    }

    fun getProfilePhoto(): String? = securePrefs.getString("profile_photo", null)

    fun getAuthToken(): String? = securePrefs.getString("auth_token", null)
    fun getUserId(): String? = securePrefs.getString("user_id", null)
    fun getUsername(): String? = securePrefs.getString("username", null)
    fun getEmail(): String? = securePrefs.getString("email", null)
    fun getDisplayName(): String? = securePrefs.getString("display_name", null)
    fun getShareCode(): String? = securePrefs.getString("share_code", null)

    fun getPartnerId(): String? = securePrefs.getString("partner_id", null)
    fun getPartnerName(): String? = securePrefs.getString("partner_name", null)

    fun clearSession() {
        securePrefs.edit().apply {
            remove("user_id")
            remove("username")
            remove("email")
            remove("display_name")
            remove("share_code")
            remove("auth_token")
            remove("partner_id")
            remove("partner_name")
            apply()
        }
    }

    fun isLoggedIn(): Boolean = getAuthToken() != null

    // ──────────────────────────────────────────────────────────────────────────
    // NON-SENSITIVE SETTINGS — stored in plainPrefs
    // ──────────────────────────────────────────────────────────────────────────

    fun getBaseUrl(): String =
        plainPrefs.getString("base_url", com.titanbag.app.BuildConfig.CLOUD_BASE_URL) ?: com.titanbag.app.BuildConfig.CLOUD_BASE_URL

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        plainPrefs.edit().putString("base_url", formatted).apply()
    }

    fun isAutoSyncEnabled(): Boolean = plainPrefs.getBoolean("auto_sync", true)

    fun setAutoSyncEnabled(enabled: Boolean) {
        plainPrefs.edit().putBoolean("auto_sync", enabled).apply()
    }

    fun getFontStyle(): String = plainPrefs.getString("font_style", "roboto") ?: "roboto"

    fun setFontStyle(style: String) {
        plainPrefs.edit().putString("font_style", style).apply()
    }

    fun getCustomColorPrimary(): String? = plainPrefs.getString("custom_color_primary", null)
    fun setCustomColorPrimary(hex: String?) = plainPrefs.edit().putString("custom_color_primary", hex).apply()

    fun getCustomColorSecondary(): String? = plainPrefs.getString("custom_color_secondary", null)
    fun setCustomColorSecondary(hex: String?) = plainPrefs.edit().putString("custom_color_secondary", hex).apply()

    fun getCustomColorBackground(): String? = plainPrefs.getString("custom_color_background", null)
    fun setCustomColorBackground(hex: String?) = plainPrefs.edit().putString("custom_color_background", hex).apply()

    fun getVisualStyle(): String = plainPrefs.getString("visual_style", "classic") ?: "classic"
    fun setVisualStyle(style: String) {
        plainPrefs.edit().putString("visual_style", style).apply()
    }

    // PDF Export Options
    fun getPdfDateRange(): String = plainPrefs.getString("pdf_date_range", "This Month") ?: "This Month"
    fun setPdfDateRange(range: String) = plainPrefs.edit().putString("pdf_date_range", range).apply()

    fun getPdfCustomStartDate(): Long = plainPrefs.getLong("pdf_custom_start_date", -1L)
    fun setPdfCustomStartDate(date: Long) = plainPrefs.edit().putLong("pdf_custom_start_date", date).apply()

    fun getPdfCustomEndDate(): Long = plainPrefs.getLong("pdf_custom_end_date", -1L)
    fun setPdfCustomEndDate(date: Long) = plainPrefs.edit().putLong("pdf_custom_end_date", date).apply()

    fun getPdfDateFormat(): String = plainPrefs.getString("pdf_date_format", "DD/MM/YYYY") ?: "DD/MM/YYYY"
    fun setPdfDateFormat(format: String) = plainPrefs.edit().putString("pdf_date_format", format).apply()

    fun getPdfTimeFormat(): String = plainPrefs.getString("pdf_time_format", "12-hour") ?: "12-hour"
    fun setPdfTimeFormat(format: String) = plainPrefs.edit().putString("pdf_time_format", format).apply()

    fun getPdfIncludeNotes(): Boolean = plainPrefs.getBoolean("pdf_include_notes", true)
    fun setPdfIncludeNotes(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_notes", include).apply()

    fun getPdfIncludeCategories(): Boolean = plainPrefs.getBoolean("pdf_include_categories", true)
    fun setPdfIncludeCategories(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_categories", include).apply()

    fun getPdfIncludeAccount(): Boolean = plainPrefs.getBoolean("pdf_include_account", true)
    fun setPdfIncludeAccount(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_account", include).apply()

    fun getPdfIncludeRunningBalance(): Boolean = plainPrefs.getBoolean("pdf_include_running_balance", true)
    fun setPdfIncludeRunningBalance(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_running_balance", include).apply()

    fun getPdfIncludeSummary(): Boolean = plainPrefs.getBoolean("pdf_include_summary", true)
    fun setPdfIncludeSummary(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_summary", include).apply()

    fun getPdfIncludeTransactionIds(): Boolean = plainPrefs.getBoolean("pdf_include_transaction_ids", false)
    fun setPdfIncludeTransactionIds(include: Boolean) = plainPrefs.edit().putBoolean("pdf_include_transaction_ids", include).apply()
}
