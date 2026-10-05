package com.example.data.local

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("medvision_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ROLE = "user_role"
        private const val KEY_USERNAME = "username"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_LAST_LOGGED_IN = "last_logged_in"
        private const val PREFIX_USER_PASS = "cred_pass_"
        private const val PREFIX_USER_ROLE = "cred_role_"
        private const val PREFIX_USER_EMAIL = "cred_email_"
        private const val PREFIX_TOKEN_EMAIL = "verify_token_email_"
        private const val PREFIX_TOKEN_ROLE = "verify_token_role_"
    }

    init {
        // Pre-populate standard clinical accounts if not already stored
        if (!prefs.contains(PREFIX_USER_PASS + "dr.jenkins")) {
            saveRegisteredCredentials(
                email = "s.jenkins@medvision.hospital.org",
                username = "dr.jenkins",
                password = "clinicianPass123",
                role = "HEALTHCARE_WORKER"
            )
        }
        if (!prefs.contains(PREFIX_USER_PASS + "eleanor.vance")) {
            saveRegisteredCredentials(
                email = "eleanor.vance@gmail.com",
                username = "eleanor.vance",
                password = "patientPass123",
                role = "PATIENT"
            )
        }
    }

    fun saveRegisteredCredentials(email: String, username: String, password: String, role: String) {
        prefs.edit()
            .putString(PREFIX_USER_PASS + username.trim().lowercase(), password)
            .putString(PREFIX_USER_ROLE + username.trim().lowercase(), role)
            .putString(PREFIX_USER_EMAIL + username.trim().lowercase(), email.trim())
            .putString(PREFIX_USER_EMAIL + email.trim().lowercase(), username.trim().lowercase())
            .apply()
    }

    fun isCredentialsValid(usernameOrEmail: String, password: String): Boolean {
        val cleanKey = usernameOrEmail.trim().lowercase()
        // Check direct username
        val storedPass = prefs.getString(PREFIX_USER_PASS + cleanKey, null)
        if (storedPass != null) {
            return storedPass == password
        }
        // Check if email was provided as login identifier
        val mappedUsername = prefs.getString(PREFIX_USER_EMAIL + cleanKey, null)
        if (mappedUsername != null) {
            val passForUser = prefs.getString(PREFIX_USER_PASS + mappedUsername, null)
            return passForUser == password
        }
        return false
    }

    fun getUserRoleForUsername(usernameOrEmail: String): String {
        val cleanKey = usernameOrEmail.trim().lowercase()
        val directRole = prefs.getString(PREFIX_USER_ROLE + cleanKey, null)
        if (directRole != null) return directRole

        val mappedUsername = prefs.getString(PREFIX_USER_EMAIL + cleanKey, null)
        if (mappedUsername != null) {
            return prefs.getString(PREFIX_USER_ROLE + mappedUsername, "PATIENT") ?: "PATIENT"
        }
        return if (cleanKey.contains("dr.") || cleanKey.contains("doctor") || cleanKey.contains("clinician")) {
            "HEALTHCARE_WORKER"
        } else {
            "PATIENT"
        }
    }

    fun getRegisteredEmail(username: String): String? {
        return prefs.getString(PREFIX_USER_EMAIL + username.trim().lowercase(), null)
    }

    fun createVerificationToken(email: String, role: String): String {
        val token = "MV-VERIFY-" + java.util.UUID.randomUUID().toString().substring(0, 8).uppercase()
        prefs.edit()
            .putString(PREFIX_TOKEN_EMAIL + token, email.trim())
            .putString(PREFIX_TOKEN_ROLE + token, role)
            .apply()
        return token
    }

    fun getPendingVerificationEmail(token: String): Pair<String, String>? {
        val email = prefs.getString(PREFIX_TOKEN_EMAIL + token.trim(), null) ?: return null
        val role = prefs.getString(PREFIX_TOKEN_ROLE + token.trim(), "PATIENT") ?: "PATIENT"
        return email to role
    }

    fun saveSession(token: String, username: String, role: String, userId: Int) {
        prefs.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putString(KEY_USERNAME, username)
            .putString(KEY_USER_ROLE, role)
            .putInt(KEY_USER_ID, userId)
            .putLong(KEY_LAST_LOGGED_IN, System.currentTimeMillis())
            .putBoolean(KEY_BIOMETRIC_ENABLED, true)
            .apply()
    }

    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "Dr. Sarah Jenkins") ?: "Dr. Sarah Jenkins"

    fun getUserRole(): String = prefs.getString(KEY_USER_ROLE, "HEALTHCARE_WORKER") ?: "HEALTHCARE_WORKER"

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, 1)

    fun isBiometricEnabled(): Boolean = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun hasPreviousSession(): Boolean = prefs.getString(KEY_AUTH_TOKEN, null) != null || prefs.contains(KEY_USERNAME)

    fun clearSession() {
        prefs.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_USER_ID)
            .apply()
    }

    fun getCustomApiKey(): String? = prefs.getString("custom_gemini_api_key", null)

    fun setCustomApiKey(key: String?) {
        if (key.isNullOrBlank()) {
            prefs.edit().remove("custom_gemini_api_key").apply()
        } else {
            prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
        }
    }
}
