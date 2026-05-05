package com.healthbridge

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * BaseActivity handles global security features like session timeout.
 */
abstract class BaseActivity : AppCompatActivity() {

    private val TIMEOUT_MILLIS = 15 * 60 * 1000 // 15 minutes

    override fun onResume() {
        super.onResume()
        checkSessionTimeout()
        updateLastActiveTime()
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        updateLastActiveTime()
    }

    private fun updateLastActiveTime() {
        val prefs = getSharedPreferences("HealthBridgeSecurity", MODE_PRIVATE)
        prefs.edit().putLong("last_active_time", System.currentTimeMillis()).apply()
    }

    private fun checkSessionTimeout() {
        // Skip check for login/register/splash
        val className = this::class.java.simpleName
        if (className == "LoginActivity" || className == "RegisterActivity" || 
            className == "DoctorRegisterActivity" || className == "SplashActivity" || 
            className == "OnboardingActivity") {
            return
        }

        val prefs = getSharedPreferences("HealthBridgeSecurity", MODE_PRIVATE)
        val lastActive = prefs.getLong("last_active_time", 0L)
        val currentTime = System.currentTimeMillis()

        val mainPrefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val isLoggedIn = mainPrefs.getBoolean("isLoggedIn", false)
        
        if (isLoggedIn && lastActive != 0L && (currentTime - lastActive) > TIMEOUT_MILLIS) {
            handleSessionExpired()
        }
    }

    private fun handleSessionExpired() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra("session_timeout", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
