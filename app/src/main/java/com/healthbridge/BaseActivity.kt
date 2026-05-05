package com.healthbridge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.healthbridge.util.SessionManager

/**
 * BaseActivity - All protected activities should extend this for session management
 *
 * Features:
 * - Automatically checks session timeout on resume
 * - Redirects to login if session expired
 * - Enforces registration requirement
 */
abstract class BaseActivity : AppCompatActivity() {

    protected lateinit var sessionManager: SessionManager

    override fun onResume() {
        super.onResume()

        // Initialize session manager
        sessionManager = SessionManager(this)

        // Check if user is registered
        if (!sessionManager.isRegistrationComplete()) {
            // User hasn't registered - send to login/register
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Check if session is still valid
        if (!sessionManager.checkSessionAndRedirectIfNeeded(this)) {
            // Session has expired or user not logged in - redirected to login
            return
        }

        // Session is valid - update the session time
        sessionManager.updateSessionTime()
    }
}

