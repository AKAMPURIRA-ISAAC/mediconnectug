package com.healthbridge

import android.app.Application
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.ApiClient

class HealthBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize database and repositories
        RepositoryFactory.initialize(this)

        // ── Restore auth token from SharedPreferences on every cold start ──
        // Without this, ApiClient.authToken is null after the app is killed
        // and every authenticated request returns HTTP 401.
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val savedToken = prefs.getString("auth_token", null)
        if (!savedToken.isNullOrBlank()) {
            ApiClient.authToken = savedToken
        }
    }
}

