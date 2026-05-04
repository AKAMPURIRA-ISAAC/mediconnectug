package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        val btnHelp = findViewById<Button>(R.id.btnHelp)
        val btnAbout = findViewById<Button>(R.id.btnAbout)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        
        btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        
        btnAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        
        btnLogout.setOnClickListener {
            val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
            prefs.edit().clear().apply()
            startActivity(Intent(this, LoginActivity::class.java))
            finishAffinity()
        }
    }
}
