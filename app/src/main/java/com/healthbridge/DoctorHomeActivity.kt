package com.healthbridge

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.healthbridge.adapters.DoctorChatSessionsAdapter
import com.healthbridge.network.ApiClient
import kotlinx.coroutines.launch

class DoctorHomeActivity : AppCompatActivity() {

    private lateinit var tvDoctorName: TextView
    private lateinit var tvOnlineStatus: TextView
    private lateinit var rvChatSessions: RecyclerView
    private lateinit var adapter: DoctorChatSessionsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_doctor_home)

        val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
        val doctorName = prefs.getString("userName", "Doctor") ?: "Doctor"

        tvDoctorName = findViewById(R.id.tvDoctorName)
        tvOnlineStatus = findViewById(R.id.tvOnlineStatus)
        rvChatSessions = findViewById(R.id.rvChatSessions)

        tvDoctorName.text = "Dr. $doctorName"
        tvOnlineStatus.text = "🟢 Online"

        findViewById<ImageView>(R.id.btnLogout).setOnClickListener {
            showLogoutDialog()
        }

        setupChatSessions()
        loadActiveChatSessions()
    }

    private fun setupChatSessions() {
        adapter = DoctorChatSessionsAdapter { session ->
            // Open chat with this patient
            val intent = Intent(this, DoctorChatActivity::class.java)
            intent.putExtra("session_id", session.id)
            intent.putExtra("patient_name", session.patientName)
            intent.putExtra("chief_complaint", session.chiefComplaint)
            intent.putExtra("urgency_level", session.urgency)
            startActivity(intent)
        }
        rvChatSessions.layoutManager = LinearLayoutManager(this)
        rvChatSessions.adapter = adapter
    }

    private fun loadActiveChatSessions() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.instance.getChatSessions()
                if (response.success && response.sessions != null) {
                    adapter.submitList(response.sessions)
                    if (response.sessions.isEmpty()) {
                        Toast.makeText(this@DoctorHomeActivity, "No active chats", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@DoctorHomeActivity, "Offline mode", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout? You'll be marked as offline.")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        lifecycleScope.launch {
            try {
                ApiClient.instance.logout()
            } catch (_: Exception) { }

            val prefs = getSharedPreferences("HealthBridge", MODE_PRIVATE)
            prefs.edit().clear().apply()
            ApiClient.authToken = null

            startActivity(Intent(this@DoctorHomeActivity, LoginActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadActiveChatSessions()
    }
}

