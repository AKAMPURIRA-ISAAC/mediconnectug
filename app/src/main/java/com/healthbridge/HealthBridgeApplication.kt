package com.healthbridge

import android.app.Application
import com.healthbridge.data.repository.RepositoryFactory

class HealthBridgeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize database and repositories
        RepositoryFactory.initialize(this)
    }
}

