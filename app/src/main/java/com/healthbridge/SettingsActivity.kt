package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        // Get user info from SharedPreferences
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val userName = prefs.getString("userName", "User")
        val userEmail = prefs.getString("userEmail", "user@email.com")

        // Initialize views
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val tvUserName = findViewById<TextView>(R.id.tvUserName)
        val tvUserEmail = findViewById<TextView>(R.id.tvUserEmail)
        val layoutProfile = findViewById<LinearLayout>(R.id.layoutProfile)
        val layoutEditProfile = findViewById<LinearLayout>(R.id.layoutEditProfile)
        val layoutChangePassword = findViewById<LinearLayout>(R.id.layoutChangePassword)
        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        val layoutLanguage = findViewById<LinearLayout>(R.id.layoutLanguage)
        val layoutHelp = findViewById<LinearLayout>(R.id.layoutHelp)
        val layoutPrivacy = findViewById<LinearLayout>(R.id.layoutPrivacy)
        val layoutAbout = findViewById<LinearLayout>(R.id.layoutAbout)
        val btnLogout = findViewById<LinearLayout>(R.id.btnLogout)
        
        // Set user info
        tvUserName.text = userName
        tvUserEmail.text = userEmail

        // Load notification preference
        val notificationsEnabled = prefs.getBoolean("notificationsEnabled", true)
        switchNotifications.isChecked = notificationsEnabled

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Profile section
        layoutProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Edit Profile
        layoutEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Change Password
        layoutChangePassword.setOnClickListener {
            Toast.makeText(this, "Change password feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notificationsEnabled", isChecked).apply()
            val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Language
        layoutLanguage.setOnClickListener {
            showLanguageDialog()
        }

        // Help & Support
        layoutHelp.setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }
        
        // Privacy Policy
        layoutPrivacy.setOnClickListener {
            Toast.makeText(this, "Privacy policy feature coming soon", Toast.LENGTH_SHORT).show()
        }

        // About
        layoutAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        
        // Logout
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf("English", "Luganda", "Swahili")
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val currentLanguage = prefs.getString("language", "English")
        val currentIndex = languages.indexOf(currentLanguage)

        AlertDialog.Builder(this)
            .setTitle("Select Language")
            .setSingleChoiceItems(languages, currentIndex) { dialog, which ->
                val selectedLanguage = languages[which]
                prefs.edit().putString("language", selectedLanguage).apply()
                findViewById<TextView>(R.id.tvLanguage).text = selectedLanguage
                Toast.makeText(this, "Language changed to $selectedLanguage", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        prefs.edit().clear().apply()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
