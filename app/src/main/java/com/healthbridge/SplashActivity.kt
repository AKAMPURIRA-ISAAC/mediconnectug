package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.healthbridge.network.ApiClient
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        
        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
            val isLoggedIn = prefs.getBoolean("isLoggedIn", false)
            // Restore auth token so all API calls are authenticated
            ApiClient.authToken = prefs.getString("auth_token", null)
            val target = if (isLoggedIn) HomeActivity::class.java else LoginActivity::class.java
            startActivity(Intent(this, target))
            finish()
        }, 2000)
    }
}
