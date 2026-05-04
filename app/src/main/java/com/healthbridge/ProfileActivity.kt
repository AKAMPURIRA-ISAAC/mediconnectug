package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.healthbridge.data.repository.RepositoryFactory
import com.healthbridge.network.ApiClient
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvAvatar: TextView
    private lateinit var tvProfileName: TextView
    private lateinit var tvProfileEmail: TextView
    private lateinit var tvProfilePhone: TextView
    private lateinit var tvApptCount: TextView
    private lateinit var tvDoctorCount: TextView
    private lateinit var tvBloodType: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Bind header views
        tvAvatar       = findViewById(R.id.tvAvatar)
        tvProfileName  = findViewById(R.id.tvProfileName)
        tvProfileEmail = findViewById(R.id.tvProfileEmail)
        tvProfilePhone = findViewById(R.id.tvProfilePhone)

        // Bind stats
        tvApptCount   = findViewById(R.id.tvApptCount)
        tvDoctorCount = findViewById(R.id.tvDoctorCount)
        tvBloodType   = findViewById(R.id.tvBloodType)

        // Edit profile button
        findViewById<TextView>(R.id.btnEditProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Menu items
        findViewById<LinearLayout>(R.id.menuMedicalRecords).setOnClickListener {
            startActivity(Intent(this, MedicalRecordsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuPrescriptions).setOnClickListener {
            startActivity(Intent(this, PrescriptionsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.menuHelp).setOnClickListener {
            startActivity(Intent(this, HelpSupportActivity::class.java))
        }

        // Logout
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnLogout).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sign Out")
                .setMessage("Are you sure you want to sign out?")
                .setPositiveButton("Sign Out") { _, _ ->
                    getSharedPreferences("HealthBridge", MODE_PRIVATE).edit().clear().apply()
                    ApiClient.authToken = null
                    startActivity(Intent(this, LoginActivity::class.java))
                    finishAffinity()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        setupBottomNav()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadProfileData()
    }

    private fun loadProfileData() {
        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val name      = prefs.getString("userName", "User") ?: "User"
        val email     = prefs.getString("userEmail", "user@example.com") ?: "user@example.com"
        val phone     = prefs.getString("userPhone", "") ?: ""
        var bloodType = prefs.getString("bloodType", "A+") ?: "A+"

        // Try to load from database
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.profileRepository.getProfile()
                result.onSuccess { user ->
                    bloodType = user.bloodType ?: "A+"
                    tvBloodType.text = bloodType
                    // Update SharedPreferences
                    prefs.edit().apply {
                        putString("bloodType", bloodType)
                        apply()
                    }
                }
            } catch (_: Exception) {
                // Fall back to SharedPreferences value
            }
        }

        tvProfileName.text  = name
        tvProfileEmail.text = email
        tvProfilePhone.text = if (phone.isNotEmpty()) "📞 $phone" else ""
        tvBloodType.text    = bloodType

        // Avatar initials (up to 2 chars)
        val initials = name.trim().split(" ")
            .filter { it.isNotEmpty() }
            .take(2)
            .joinToString("") { it.first().uppercaseChar().toString() }
        tvAvatar.text = initials.ifEmpty { "U" }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            try {
                val result = RepositoryFactory.appointmentRepository.getAppointments()
                result.onSuccess { appointments ->
                    tvApptCount.text   = appointments.size.toString()
                    val uniqueDoctors  = appointments.map { it.doctorName }.toSet().size
                    tvDoctorCount.text = uniqueDoctors.toString()
                }
            } catch (_: Exception) {
                // Keep default "0" values if API unavailable
            }
        }
    }

    private fun setupBottomNav() {
        findViewById<TextView>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.navSearch).setOnClickListener {
            startActivity(Intent(this, FindDoctorsActivity::class.java))
        }
        findViewById<TextView>(R.id.navSchedule).setOnClickListener {
            startActivity(Intent(this, MyAppointmentsActivity::class.java))
        }
        findViewById<TextView>(R.id.navChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
        // navProfile is already active — no action needed
    }
}
