package com.healthbridge.util

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import com.healthbridge.LoginActivity

/**
 * SessionManager - Handles authentication, session timeout, and role-based access control
 *
 * Features:
 * - Enforces user registration before accessing app
 * - Implements 15-minute session timeout
 * - Requires password re-entry after timeout
 * - Manages role-based access (patient vs doctor)
 * - Tracks login state and timestamps
 */
class SessionManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("HealthBridge", Context.MODE_PRIVATE)

    companion object {
        private const val SESSION_TIMEOUT_MS = 15 * 60 * 1000  // 15 minutes
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_NAME = "userName"
        private const val KEY_USER_TYPE = "userType"  // "patient" or "doctor"
        private const val KEY_USER_ID = "userId"
        private const val KEY_DOCTOR_ID = "doctorId"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_LAST_SESSION_TIME = "lastSessionTime"
        private const val KEY_REGISTRATION_COMPLETE = "registrationComplete"
    }

    /**
     * Check if session is still valid (not timed out)
     */
    fun isSessionValid(): Boolean {
        if (!isLoggedIn()) return false

        val lastSessionTime = prefs.getLong(KEY_LAST_SESSION_TIME, 0)
        if (lastSessionTime == 0L) return false  // First login, set session

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastSessionTime

        return elapsedTime < SESSION_TIMEOUT_MS
    }

    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) &&
               !prefs.getString(KEY_USER_EMAIL, "").isNullOrBlank()
    }

    /**
     * Check if registration is complete (blocks app access if not)
     */
    fun isRegistrationComplete(): Boolean {
        return prefs.getBoolean(KEY_REGISTRATION_COMPLETE, false)
    }

    /**
     * Save user after successful login
     */
    fun saveUser(
        email: String,
        name: String,
        token: String,
        userType: String = "patient",
        userId: Int = -1,
        doctorId: Int? = null
    ) {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, true)
            putBoolean(KEY_REGISTRATION_COMPLETE, true)  // Registration is complete
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putString(KEY_USER_TYPE, userType)
            putInt(KEY_USER_ID, userId)
            if (doctorId != null) putInt(KEY_DOCTOR_ID, doctorId)
            putString(KEY_AUTH_TOKEN, token)
            putLong(KEY_LAST_SESSION_TIME, System.currentTimeMillis())  // Set initial session time
            apply()
        }
    }

    /**
     * Update session timestamp (call this when app resumes)
     */
    fun updateSessionTime() {
        if (isLoggedIn()) {
            prefs.edit().putLong(KEY_LAST_SESSION_TIME, System.currentTimeMillis()).apply()
        }
    }

    /**
     * Get user email
     */
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)

    /**
     * Get user name
     */
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    /**
     * Get user type
     */
    fun getUserType(): String = prefs.getString(KEY_USER_TYPE, "patient") ?: "patient"

    /**
     * Get user ID
     */
    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    /**
     * Get doctor ID (only for doctor users)
     */
    fun getDoctorId(): Int? {
        val doctorId = prefs.getInt(KEY_DOCTOR_ID, -1)
        return if (doctorId == -1) null else doctorId
    }

    /**
     * Get auth token
     */
    fun getAuthToken(): String? = prefs.getString(KEY_AUTH_TOKEN, null)

    /**
     * Check if user is a doctor
     */
    fun isDoctor(): Boolean = getUserType() == "doctor"

    /**
     * Check if user is a patient
     */
    fun isPatient(): Boolean = getUserType() == "patient"

    /**
     * Get time remaining in session (in milliseconds)
     */
    fun getSessionTimeRemaining(): Long {
        val lastSessionTime = prefs.getLong(KEY_LAST_SESSION_TIME, 0)
        if (lastSessionTime == 0L) return SESSION_TIMEOUT_MS.toLong()

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - lastSessionTime
        val remaining = SESSION_TIMEOUT_MS - elapsedTime

        return maxOf(0L, remaining)
    }

    /**
     * Logout - clears all session data except registration flag
     */
    fun logout() {
        prefs.edit().apply {
            putBoolean(KEY_IS_LOGGED_IN, false)
            remove(KEY_USER_EMAIL)
            remove(KEY_USER_NAME)
            remove(KEY_AUTH_TOKEN)
            remove(KEY_LAST_SESSION_TIME)
            remove(KEY_USER_ID)
            remove(KEY_DOCTOR_ID)
            apply()
        }
    }

    /**
     * Logout and force re-login (session timeout)
     */
    fun forceLogoutDueToTimeout() {
        logout()
    }

    /**
     * Check if session has timed out and redirect to login if needed
     * Returns true if should continue, false if redirecting to login
     */
    fun checkSessionAndRedirectIfNeeded(currentActivity: android.app.Activity): Boolean {
        if (!isLoggedIn()) {
            // Not logged in at all - must register/login
            currentActivity.startActivity(Intent(currentActivity, LoginActivity::class.java))
            currentActivity.finish()
            return false
        }

        if (!isSessionValid()) {
            // Session timeout - logout and redirect to login
            forceLogoutDueToTimeout()
            val intent = Intent(currentActivity, LoginActivity::class.java)
            intent.putExtra("session_timeout", true)
            currentActivity.startActivity(intent)
            currentActivity.finish()
            return false
        }

        // Session is valid - update timestamp
        updateSessionTime()
        return true
    }
}

