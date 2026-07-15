package com.expenso.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SessionManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val prefs = EncryptedSharedPreferences.create(
        "titanbag_secure_session",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveSession(
        userId: String,
        username: String,
        email: String,
        displayName: String,
        shareCode: String,
        token: String
    ) {
        prefs.edit().apply {
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
        prefs.edit().apply {
            putString("partner_id", partnerId)
            putString("partner_name", partnerName)
            apply()
        }
    }

    fun getAuthToken(): String? = prefs.getString("auth_token", null)
    fun getUserId(): String? = prefs.getString("user_id", null)
    fun getUsername(): String? = prefs.getString("username", null)
    fun getEmail(): String? = prefs.getString("email", null)
    fun getDisplayName(): String? = prefs.getString("display_name", null)
    fun getShareCode(): String? = prefs.getString("share_code", null)

    fun getPartnerId(): String? = prefs.getString("partner_id", null)
    fun getPartnerName(): String? = prefs.getString("partner_name", null)

    fun clearSession() {
        prefs.edit().apply {
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

    fun getBaseUrl(): String = prefs.getString("base_url", "http://10.0.2.2:5000") ?: "http://10.0.2.2:5000"

    fun setBaseUrl(url: String) {
        val formatted = if (url.endsWith("/")) url else "$url/"
        prefs.edit().putString("base_url", formatted).apply()
    }

    fun isAutoSyncEnabled(): Boolean = prefs.getBoolean("auto_sync", true)

    fun setAutoSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("auto_sync", enabled).apply()
    }
}
